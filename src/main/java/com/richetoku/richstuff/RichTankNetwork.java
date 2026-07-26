package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Create-style same-tier tank multiblock resolver.
 *
 * <p>A valid standalone tank network is either a 1x1 vertical column or a complete square footprint
 * from 2x2 through 5x5. Square networks grow only in complete layers and may reach 15 blocks tall.
 * Every member shares one fluid handler and internal faces are hidden. Partial rows and rectangles
 * remain independent until they become a complete square.</p>
 *
 * <p>Resolution is cached once per level tick and the resulting view is assigned to every member.
 * This prevents each visible tank from rescanning up to 2,349 nearby positions every render frame,
 * which was capable of stalling the integrated server/client when opening screens near large tanks.</p>
 */
final class RichTankNetwork {
    static final int MAX_WIDTH = 5;
    static final int MAX_HEIGHT = 15;

    private static final Map<Level, TickCache> CACHES = Collections.synchronizedMap(new WeakHashMap<>());

    private RichTankNetwork() {}

    static View resolve(RichTankBlockEntity start) {
        Level level = start.getLevel();
        if (level == null || start.foundryController() != null) return View.single(start);

        TickCache cache = cache(level);
        View cached = cache.views.get(start.getBlockPos());
        if (cached != null) return cached;

        View resolved = compute(start, level);
        for (RichTankBlockEntity member : resolved.members()) cache.views.put(member.getBlockPos(), resolved);
        return resolved;
    }

    static void invalidate(Level level) {
        synchronized (CACHES) {
            TickCache cache = CACHES.get(level);
            if (cache != null) cache.views.clear();
        }
    }

    /** Rebuilds connected-face states for all same-tier tanks that could be affected by one edit. */
    static void refreshConnections(Level level, BlockPos center, int tier) {
        if (level == null) return;
        invalidate(level);
        List<RichTankBlockEntity> nearby = new ArrayList<>();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - (MAX_HEIGHT - 1));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + (MAX_HEIGHT - 1));
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - (MAX_WIDTH - 1); x <= center.getX() + (MAX_WIDTH - 1); x++) {
                for (int z = center.getZ() - (MAX_WIDTH - 1); z <= center.getZ() + (MAX_WIDTH - 1); z++) {
                    RichTankBlockEntity tank = tankAt(level, x, y, z, tier);
                    if (tank != null) nearby.add(tank);
                }
            }
        }
        for (RichTankBlockEntity tank : nearby) RichTankBlock.applyConnectionState(level, tank, resolve(tank));
    }

    private static TickCache cache(Level level) {
        synchronized (CACHES) {
            long tick = level.getGameTime();
            TickCache cache = CACHES.computeIfAbsent(level, ignored -> new TickCache(tick));
            if (cache.tick != tick) {
                cache.tick = tick;
                cache.views.clear();
            }
            return cache;
        }
    }

    private static View compute(RichTankBlockEntity start, Level level) {
        int tier = start.tier();
        BlockPos origin = start.getBlockPos();
        Candidate best = null;

        // Prefer the largest complete square footprint containing the queried tank.
        for (int size = MAX_WIDTH; size >= 2; size--) {
            for (int minX = origin.getX() - size + 1; minX <= origin.getX(); minX++) {
                for (int minZ = origin.getZ() - size + 1; minZ <= origin.getZ(); minZ++) {
                    if (!completeLayer(level, tier, minX, origin.getY(), minZ, size)) continue;
                    int minY = origin.getY();
                    int maxY = origin.getY();
                    while (minY > level.getMinBuildHeight() && maxY - minY + 1 < MAX_HEIGHT
                            && completeLayer(level, tier, minX, minY - 1, minZ, size)) minY--;
                    while (maxY < level.getMaxBuildHeight() - 1 && maxY - minY + 1 < MAX_HEIGHT
                            && completeLayer(level, tier, minX, maxY + 1, minZ, size)) maxY++;
                    Candidate candidate = candidate(level, tier, minX, minY, minZ, size, maxY - minY + 1);
                    if (candidate != null && (best == null || candidate.volume() > best.volume()
                            || candidate.volume() == best.volume() && candidate.size() > best.size())) best = candidate;
                }
            }
            if (best != null && best.size() == size) break;
        }

        // A one-wide vertical column always shares storage, matching Create fluid tanks.
        if (best == null) {
            int minY = origin.getY();
            int maxY = origin.getY();
            while (minY > level.getMinBuildHeight() && maxY - minY + 1 < MAX_HEIGHT
                    && tankAt(level, origin.getX(), minY - 1, origin.getZ(), tier) != null) minY--;
            while (maxY < level.getMaxBuildHeight() - 1 && maxY - minY + 1 < MAX_HEIGHT
                    && tankAt(level, origin.getX(), maxY + 1, origin.getZ(), tier) != null) maxY++;
            best = candidate(level, tier, origin.getX(), minY, origin.getZ(), 1, maxY - minY + 1);
        }

        if (best == null) return View.single(start);
        return new View(best.members(), best.min(), best.max(), best.size(), best.height(), best.size(), start.capacity());
    }

    private static boolean completeLayer(Level level, int tier, int minX, int y, int minZ, int size) {
        for (int x = minX; x < minX + size; x++) {
            for (int z = minZ; z < minZ + size; z++) {
                if (tankAt(level, x, y, z, tier) == null) return false;
            }
        }
        return true;
    }

    private static RichTankBlockEntity tankAt(Level level, int x, int y, int z, int tier) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos)) return null;
        if (level.getBlockEntity(pos) instanceof RichTankBlockEntity tank
                && tank.tier() == tier && tank.foundryController() == null) return tank;
        return null;
    }

    private static Candidate candidate(Level level, int tier, int minX, int minY, int minZ, int size, int height) {
        List<RichTankBlockEntity> members = new ArrayList<>(size * size * height);
        FluidStack type = FluidStack.EMPTY;
        for (int y = minY; y < minY + height; y++) {
            for (int z = minZ; z < minZ + size; z++) {
                for (int x = minX; x < minX + size; x++) {
                    RichTankBlockEntity tank = tankAt(level, x, y, z, tier);
                    if (tank == null) return null;
                    FluidStack local = tank.localFluid();
                    if (!local.isEmpty()) {
                        if (type.isEmpty()) type = local.copy();
                        else if (!FluidStack.isSameFluidSameComponents(type, local)) return null;
                    }
                    members.add(tank);
                }
            }
        }
        members.sort(Comparator.comparingInt((RichTankBlockEntity tank) -> tank.getBlockPos().getY())
                .thenComparingInt(tank -> tank.getBlockPos().getZ())
                .thenComparingInt(tank -> tank.getBlockPos().getX()));
        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(minX + size - 1, minY + height - 1, minZ + size - 1);
        return new Candidate(List.copyOf(members), min, max, size, height);
    }

    private static final class TickCache {
        private long tick;
        private final Map<BlockPos, View> views = new HashMap<>();
        private TickCache(long tick) { this.tick = tick; }
    }

    private record Candidate(List<RichTankBlockEntity> members, BlockPos min, BlockPos max, int size, int height) {
        int volume() { return members.size(); }
    }

    record View(List<RichTankBlockEntity> members, BlockPos min, BlockPos max, int width, int height, int depth,
                int perBlockCapacity) {
        static View single(RichTankBlockEntity tank) {
            return new View(List.of(tank), tank.getBlockPos(), tank.getBlockPos(), 1, 1, 1, tank.capacity());
        }

        boolean contains(BlockPos pos) {
            if (pos.getX() < min.getX() || pos.getX() > max.getX()
                    || pos.getY() < min.getY() || pos.getY() > max.getY()
                    || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) return false;
            for (RichTankBlockEntity member : members) if (member.getBlockPos().equals(pos)) return true;
            return false;
        }

        int capacity() {
            long value = (long) perBlockCapacity * members.size();
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
        }

        FluidStack fluid() {
            for (RichTankBlockEntity tank : members) if (!tank.localFluid().isEmpty()) return tank.localFluid().copy();
            return FluidStack.EMPTY;
        }

        int amount() {
            long total = 0L;
            for (RichTankBlockEntity tank : members) total += tank.localFluid().getAmount();
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        boolean compatible(FluidStack offered) {
            FluidStack current = fluid();
            return current.isEmpty() || FluidStack.isSameFluidSameComponents(current, offered);
        }

        int visualAmount(RichTankBlockEntity tank) {
            int total = amount();
            if (total <= 0) return 0;
            int layer = tank.getBlockPos().getY() - min.getY();
            long layerCapacity = (long) perBlockCapacity * width * depth;
            long inLayer = Math.max(0L, Math.min(layerCapacity, (long) total - layer * layerCapacity));
            return (int) Math.min(perBlockCapacity,
                    Math.round(perBlockCapacity * (inLayer / (float) Math.max(1L, layerCapacity))));
        }


        void redistributeWithout(RichTankBlockEntity removed) {
            FluidStack type = fluid();
            int remaining = amount();
            List<RichTankBlockEntity> changed = new ArrayList<>();
            for (RichTankBlockEntity tank : members) {
                if (tank == removed) {
                    if (tank.setLocalFluidIfChanged(FluidStack.EMPTY)) changed.add(tank);
                    continue;
                }
                int part = Math.min(perBlockCapacity, remaining);
                FluidStack next = part <= 0 || type.isEmpty() ? FluidStack.EMPTY : type.copyWithAmount(part);
                if (tank.setLocalFluidIfChanged(next)) changed.add(tank);
                remaining -= part;
            }
            for (RichTankBlockEntity tank : changed) tank.syncLocal();
            Level level = removed.getLevel();
            if (level != null) invalidate(level);
        }

        void distribute(FluidStack type, int amount) {
            int remaining = Math.max(0, Math.min(capacity(), amount));
            List<RichTankBlockEntity> changed = new ArrayList<>();
            for (RichTankBlockEntity tank : members) {
                int part = Math.min(perBlockCapacity, remaining);
                FluidStack next = part <= 0 ? FluidStack.EMPTY : type.copyWithAmount(part);
                if (tank.setLocalFluidIfChanged(next)) changed.add(tank);
                remaining -= part;
            }
            for (RichTankBlockEntity tank : changed) tank.syncLocal();
            Level level = members.isEmpty() ? null : members.get(0).getLevel();
            if (level != null) invalidate(level);
        }
    }
}
