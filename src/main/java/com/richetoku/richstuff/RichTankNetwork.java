package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Create-style same-tier tank multiblock resolver.
 *
 * <p>Every same-tier tank may always join a one-wide vertical column when its horizontal slice is
 * not assigned to a completed square layer. Completed 2x2 through 7x7 layers are selected
 * deterministically and never overlap: the largest square wins, then the north-west-most square.
 * A complete layer may connect vertically only to another complete layer with exactly the same
 * footprint. Partial layers and vertical columns above one member remain independent.</p>
 */
final class RichTankNetwork {
    static final int MAX_WIDTH = 7;
    static final int MAX_HEIGHT = 7;
    private static final int MAX_HORIZONTAL_COMPONENT = 2048;

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
            if (cache != null) cache.clear();
        }
    }

    /** Rebuilds face states and resolves vertical-column/layer junction fluid safely. */
    static void refreshConnections(Level level, BlockPos center, int tier) {
        if (level == null) return;
        invalidate(level);
        List<RichTankBlockEntity> nearby = nearby(level, center, tier);

        // When a completed platform claims the bottom member of an existing vertical column,
        // preserve column ownership by moving that member's contents upward first. Any overflow
        // remains in the newly formed platform, exactly as requested.
        boolean moved = evacuateLayerJunctions(level, nearby, tier);
        if (moved) {
            invalidate(level);
            nearby = nearby(level, center, tier);
        }

        for (RichTankBlockEntity tank : nearby) RichTankBlock.applyConnectionState(level, tank, resolve(tank));
    }

    private static List<RichTankBlockEntity> nearby(Level level, BlockPos center, int tier) {
        List<RichTankBlockEntity> nearby = new ArrayList<>();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - (MAX_HEIGHT - 1));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + (MAX_HEIGHT - 1));
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - (MAX_WIDTH * 2); x <= center.getX() + (MAX_WIDTH * 2); x++) {
                for (int z = center.getZ() - (MAX_WIDTH * 2); z <= center.getZ() + (MAX_WIDTH * 2); z++) {
                    RichTankBlockEntity tank = tankAt(level, x, y, z, tier);
                    if (tank != null) nearby.add(tank);
                }
            }
        }
        return nearby;
    }

    private static boolean evacuateLayerJunctions(Level level, List<RichTankBlockEntity> tanks, int tier) {
        boolean changed = false;
        // Stable position order keeps the transfer deterministic when several columns meet a layer.
        tanks.sort(Comparator.comparingInt((RichTankBlockEntity tank) -> tank.getBlockPos().getY())
                .thenComparingInt(tank -> tank.getBlockPos().getZ())
                .thenComparingInt(tank -> tank.getBlockPos().getX()));
        for (RichTankBlockEntity lower : tanks) {
            Layer lowerLayer = layerAt(level, lower.getBlockPos(), tier);
            if (lowerLayer == null || lower.localFluid().isEmpty()) continue;
            RichTankBlockEntity upper = tankAt(level, lower.getBlockPos().getX(), lower.getBlockPos().getY() + 1,
                    lower.getBlockPos().getZ(), tier);
            if (upper == null || layerAt(level, upper.getBlockPos(), tier) != null) continue;
            View upperColumn = resolve(upper);
            if (upperColumn.width() != 1 || !upperColumn.compatible(lower.localFluid())) continue;
            FluidStack source = lower.localFluid();
            int accepted = Math.min(source.getAmount(), Math.max(0, upperColumn.capacity() - upperColumn.amount()));
            if (accepted <= 0) continue;
            upperColumn.distribute(source, upperColumn.amount() + accepted);
            int remaining = source.getAmount() - accepted;
            lower.setLocalFluidIfChanged(remaining <= 0 ? FluidStack.EMPTY : source.copyWithAmount(remaining));
            lower.syncLocal();
            changed = true;
            invalidate(level);
        }
        return changed;
    }

    private static TickCache cache(Level level) {
        synchronized (CACHES) {
            long tick = level.getGameTime();
            TickCache cache = CACHES.computeIfAbsent(level, ignored -> new TickCache(tick));
            if (cache.tick != tick) {
                cache.tick = tick;
                cache.clear();
            }
            return cache;
        }
    }

    private static View compute(RichTankBlockEntity start, Level level) {
        int tier = start.tier();
        BlockPos origin = start.getBlockPos();

        // Reconstruct the largest complete square stack directly from the actual tanks before
        // consulting persisted face-state locks. This is essential after bulk placement, chunk
        // reloads, or upgrades from older builds where the connection booleans may not yet describe
        // the complete structure. A valid 7x7x7 Tier VII tank therefore exposes all 343 members
        // (175,616 buckets), rather than falling back to one local 512-bucket block.
        Candidate completeStack = largestCompleteSquareStack(level, origin, tier);
        if (completeStack != null && completeStack.members().size() > 1) {
            return new View(completeStack.members(), completeStack.min(), completeStack.max(),
                    completeStack.size(), completeStack.height(), completeStack.size(), start.capacity());
        }

        Layer originLayer = layerAt(level, origin, tier);
        Candidate best;

        if (originLayer != null) {
            int minY = origin.getY();
            int maxY = origin.getY();
            while (minY > level.getMinBuildHeight() && maxY - minY + 1 < MAX_HEIGHT) {
                Layer below = layerAt(level, new BlockPos(origin.getX(), minY - 1, origin.getZ()), tier);
                if (below == null || !originLayer.sameFootprint(below)) break;
                minY--;
            }
            while (maxY < level.getMaxBuildHeight() - 1 && maxY - minY + 1 < MAX_HEIGHT) {
                Layer above = layerAt(level, new BlockPos(origin.getX(), maxY + 1, origin.getZ()), tier);
                if (above == null || !originLayer.sameFootprint(above)) break;
                maxY++;
            }
            best = candidate(level, tier, originLayer.minX(), minY, originLayer.minZ(),
                    originLayer.size(), maxY - minY + 1);
        } else {
            int minY = origin.getY();
            int maxY = origin.getY();
            while (minY > level.getMinBuildHeight() && maxY - minY + 1 < MAX_HEIGHT) {
                BlockPos below = new BlockPos(origin.getX(), minY - 1, origin.getZ());
                if (tankAt(level, below.getX(), below.getY(), below.getZ(), tier) == null
                        || layerAt(level, below, tier) != null) break;
                minY--;
            }
            while (maxY < level.getMaxBuildHeight() - 1 && maxY - minY + 1 < MAX_HEIGHT) {
                BlockPos above = new BlockPos(origin.getX(), maxY + 1, origin.getZ());
                if (tankAt(level, above.getX(), above.getY(), above.getZ(), tier) == null
                        || layerAt(level, above, tier) != null) break;
                maxY++;
            }
            best = candidate(level, tier, origin.getX(), minY, origin.getZ(), 1, maxY - minY + 1);
        }

        if (best == null) return View.single(start);
        return new View(best.members(), best.min(), best.max(), best.size(), best.height(), best.size(), start.capacity());
    }


    /** Finds the largest complete square footprint containing {@code origin} and extends it vertically. */
    @Nullable
    private static Candidate largestCompleteSquareStack(Level level, BlockPos origin, int tier) {
        Candidate best = null;
        int bestVolume = 0;
        for (int size = MAX_WIDTH; size >= 2; size--) {
            for (int offsetX = 0; offsetX < size; offsetX++) {
                int minX = origin.getX() - offsetX;
                for (int offsetZ = 0; offsetZ < size; offsetZ++) {
                    int minZ = origin.getZ() - offsetZ;
                    if (!completeSquare(level, minX, origin.getY(), minZ, size, tier)) continue;
                    int minY = origin.getY();
                    int maxY = origin.getY();
                    while (minY > level.getMinBuildHeight() && maxY - minY + 1 < MAX_HEIGHT
                            && completeSquare(level, minX, minY - 1, minZ, size, tier)) minY--;
                    while (maxY < level.getMaxBuildHeight() - 1 && maxY - minY + 1 < MAX_HEIGHT
                            && completeSquare(level, minX, maxY + 1, minZ, size, tier)) maxY++;
                    Candidate candidate = candidate(level, tier, minX, minY, minZ, size, maxY - minY + 1);
                    if (candidate == null) continue;
                    int volume = candidate.members().size();
                    if (volume > bestVolume || volume == bestVolume && better(candidate, best)) {
                        best = candidate;
                        bestVolume = volume;
                    }
                }
            }
            // A larger square always wins over any smaller square at the same permitted height.
            if (best != null && best.size() == size) break;
        }
        return best;
    }

    private static boolean completeSquare(Level level, int minX, int y, int minZ, int size, int tier) {
        for (int x = minX; x < minX + size; x++) for (int z = minZ; z < minZ + size; z++) {
            if (tankAt(level, x, y, z, tier) == null) return false;
        }
        return true;
    }

    private static boolean better(Candidate candidate, @Nullable Candidate current) {
        if (current == null) return true;
        if (candidate.size() != current.size()) return candidate.size() > current.size();
        if (candidate.height() != current.height()) return candidate.height() > current.height();
        if (candidate.min().getX() != current.min().getX()) return candidate.min().getX() < current.min().getX();
        return candidate.min().getZ() < current.min().getZ();
    }

    /** Assigns all tanks in one horizontal connected component to non-overlapping complete squares. */
    private static Layer layerAt(Level level, BlockPos pos, int tier) {
        TickCache cache = cache(level);
        Layer known = cache.layers.get(pos);
        if (known != null) return known;
        if (cache.noLayer.contains(pos)) return null;
        if (tankAt(level, pos.getX(), pos.getY(), pos.getZ(), tier) == null) {
            cache.noLayer.add(pos.immutable());
            return null;
        }

        Layer locked = lockedLayerFromState(level, pos, tier);
        Set<BlockPos> component = horizontalComponent(level, pos, tier);
        if (component.isEmpty()) return null;
        int minX = component.stream().mapToInt(BlockPos::getX).min().orElse(pos.getX());
        int maxX = component.stream().mapToInt(BlockPos::getX).max().orElse(pos.getX());
        int minZ = component.stream().mapToInt(BlockPos::getZ).min().orElse(pos.getZ());
        int maxZ = component.stream().mapToInt(BlockPos::getZ).max().orElse(pos.getZ());

        List<Layer> candidates = new ArrayList<>();
        for (int size = MAX_WIDTH; size >= 2; size--) {
            for (int x = minX; x <= maxX - size + 1; x++) {
                for (int z = minZ; z <= maxZ - size + 1; z++) {
                    boolean complete = true;
                    for (int dx = 0; dx < size && complete; dx++) {
                        for (int dz = 0; dz < size; dz++) {
                            if (!component.contains(new BlockPos(x + dx, pos.getY(), z + dz))) {
                                complete = false;
                                break;
                            }
                        }
                    }
                    if (complete) {
                        Layer candidate = new Layer(x, pos.getY(), z, size);
                        boolean claimsExistingLayer = false;
                        for (BlockPos member : candidate.positions()) {
                            Layer memberLayer = lockedLayerFromState(level, member, tier);
                            // Never split or partially steal an established platform. A larger complete
                            // square may absorb it only when the old footprint is wholly contained.
                            if (memberLayer != null && !candidate.contains(memberLayer)) {
                                claimsExistingLayer = true;
                                break;
                            }
                        }
                        if (!claimsExistingLayer) candidates.add(candidate);
                    }
                }
            }
        }
        if (locked != null && candidates.stream().noneMatch(candidate -> candidate.contains(locked))) {
            candidates.add(locked);
        }
        candidates.sort(Comparator.comparingInt(Layer::size).reversed()
                .thenComparingInt(Layer::minX).thenComparingInt(Layer::minZ));

        Set<BlockPos> occupied = new HashSet<>();
        for (Layer candidate : candidates) {
            boolean overlaps = candidate.positions().stream().anyMatch(occupied::contains);
            if (overlaps) continue;
            for (BlockPos member : candidate.positions()) {
                occupied.add(member);
                cache.layers.put(member, candidate);
            }
        }
        for (BlockPos member : component) if (!cache.layers.containsKey(member)) cache.noLayer.add(member);
        return cache.layers.get(pos);
    }


    /**
     * Reconstructs an already-formed horizontal layer from the persisted face-connection state.
     * This locks an established platform footprint so a newly placed adjacent tank cannot steal
     * one of its members to create an overlapping or larger layer.
     */
    private static Layer lockedLayerFromState(Level level, BlockPos start, int tier) {
        RichTankBlockEntity first = tankAt(level, start.getX(), start.getY(), start.getZ(), tier);
        if (first == null) return null;
        var firstState = level.getBlockState(start);
        if (!(firstState.getBlock() instanceof RichTankBlock) || !hasHorizontalConnection(firstState)) return null;

        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start.immutable());
        while (!open.isEmpty() && found.size() <= MAX_WIDTH * MAX_WIDTH) {
            BlockPos current = open.removeFirst();
            if (!found.add(current)) continue;
            var state = level.getBlockState(current);
            if (!(state.getBlock() instanceof RichTankBlock)) return null;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!connected(state, direction)) continue;
                BlockPos next = current.relative(direction);
                RichTankBlockEntity nextTank = tankAt(level, next.getX(), next.getY(), next.getZ(), tier);
                if (nextTank == null) return null;
                var nextState = level.getBlockState(next);
                if (!connected(nextState, direction.getOpposite())) return null;
                if (!found.contains(next)) open.addLast(next.immutable());
            }
        }
        int minX = found.stream().mapToInt(BlockPos::getX).min().orElse(start.getX());
        int maxX = found.stream().mapToInt(BlockPos::getX).max().orElse(start.getX());
        int minZ = found.stream().mapToInt(BlockPos::getZ).min().orElse(start.getZ());
        int maxZ = found.stream().mapToInt(BlockPos::getZ).max().orElse(start.getZ());
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        if (width != depth || width < 2 || width > MAX_WIDTH || found.size() != width * depth) return null;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!found.contains(new BlockPos(x, start.getY(), z))) return null;
        }
        return new Layer(minX, start.getY(), minZ, width);
    }

    private static boolean hasHorizontalConnection(net.minecraft.world.level.block.state.BlockState state) {
        return connected(state, Direction.NORTH) || connected(state, Direction.SOUTH)
                || connected(state, Direction.EAST) || connected(state, Direction.WEST);
    }

    private static boolean connected(net.minecraft.world.level.block.state.BlockState state, Direction direction) {
        return switch (direction) {
            case NORTH -> state.hasProperty(RichTankBlock.NORTH) && state.getValue(RichTankBlock.NORTH);
            case SOUTH -> state.hasProperty(RichTankBlock.SOUTH) && state.getValue(RichTankBlock.SOUTH);
            case EAST -> state.hasProperty(RichTankBlock.EAST) && state.getValue(RichTankBlock.EAST);
            case WEST -> state.hasProperty(RichTankBlock.WEST) && state.getValue(RichTankBlock.WEST);
            default -> false;
        };
    }

    private static Set<BlockPos> horizontalComponent(Level level, BlockPos start, int tier) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start.immutable());
        while (!open.isEmpty() && found.size() < MAX_HORIZONTAL_COMPONENT) {
            BlockPos current = open.removeFirst();
            if (!found.add(current)) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (!found.contains(next) && tankAt(level, next.getX(), next.getY(), next.getZ(), tier) != null) {
                    open.addLast(next.immutable());
                }
            }
        }
        return found;
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
        private final Map<BlockPos, Layer> layers = new HashMap<>();
        private final Set<BlockPos> noLayer = new HashSet<>();
        private TickCache(long tick) { this.tick = tick; }
        private void clear() { views.clear(); layers.clear(); noLayer.clear(); }
    }

    private record Layer(int minX, int y, int minZ, int size) {
        boolean sameFootprint(Layer other) {
            return other != null && minX == other.minX && minZ == other.minZ && size == other.size;
        }
        boolean contains(Layer other) {
            return other != null && y == other.y && other.minX >= minX && other.minZ >= minZ
                    && other.minX + other.size <= minX + size
                    && other.minZ + other.size <= minZ + size;
        }
        List<BlockPos> positions() {
            List<BlockPos> result = new ArrayList<>(size * size);
            for (int x = minX; x < minX + size; x++) for (int z = minZ; z < minZ + size; z++) {
                result.add(new BlockPos(x, y, z));
            }
            return result;
        }
    }

    private record Candidate(List<RichTankBlockEntity> members, BlockPos min, BlockPos max, int size, int height) {}

    record View(List<RichTankBlockEntity> members, BlockPos min, BlockPos max, int width, int height, int depth,
                int perBlockCapacity) {
        static View single(RichTankBlockEntity tank) {
            return new View(List.of(tank), tank.getBlockPos(), tank.getBlockPos(), 1, 1, 1, tank.capacity());
        }

        boolean contains(BlockPos pos) {
            for (RichTankBlockEntity member : members) if (member.getBlockPos().equals(pos)) return true;
            return false;
        }

        int capacity() {
            long value = (long) perBlockCapacity * members.size();
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
        }

        FluidStack fluid() {
            FluidStack type = FluidStack.EMPTY;
            long total = 0L;
            for (RichTankBlockEntity tank : members) {
                FluidStack local = tank.localFluid();
                if (local.isEmpty()) continue;
                if (type.isEmpty()) type = local.copyWithAmount(1);
                total += local.getAmount();
            }
            if (type.isEmpty() || total <= 0L) return FluidStack.EMPTY;
            return type.copyWithAmount((int) Math.min(Integer.MAX_VALUE, total));
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

        /** Bottom layers fill first; members within each layer receive an even share. */
        int visualAmount(RichTankBlockEntity tank) {
            int total = amount();
            if (total <= 0) return 0;
            int layerIndex = tank.getBlockPos().getY() - min.getY();
            int tanksPerLayer = Math.max(1, width * depth);
            long layerCapacity = (long) perBlockCapacity * tanksPerLayer;
            long inLayer = Math.max(0L, Math.min(layerCapacity, (long) total - layerIndex * layerCapacity));
            if (inLayer <= 0) return 0;
            return Math.min(perBlockCapacity, (int) Math.ceil(inLayer / (double) tanksPerLayer));
        }

        void redistributeWithout(RichTankBlockEntity removed) {
            FluidStack type = fluid();
            int remaining = amount();
            List<RichTankBlockEntity> survivors = members.stream().filter(tank -> tank != removed).toList();
            distributeAmong(type, remaining, survivors);
            if (removed.setLocalFluidIfChanged(FluidStack.EMPTY)) removed.syncLocal();
            Level level = removed.getLevel();
            if (level != null) invalidate(level);
        }

        void distribute(FluidStack type, int amount) {
            distributeAmong(type, Math.max(0, Math.min(capacity(), amount)), members);
            Level level = members.isEmpty() ? null : members.get(0).getLevel();
            if (level != null) invalidate(level);
        }

        private void distributeAmong(FluidStack type, int requested, List<RichTankBlockEntity> targets) {
            int remaining = Math.max(0, requested);
            List<RichTankBlockEntity> changed = new ArrayList<>();
            Map<Integer, List<RichTankBlockEntity>> byLayer = new java.util.TreeMap<>();
            for (RichTankBlockEntity tank : targets) {
                byLayer.computeIfAbsent(tank.getBlockPos().getY(), ignored -> new ArrayList<>()).add(tank);
            }
            for (List<RichTankBlockEntity> layer : byLayer.values()) {
                layer.sort(Comparator.comparingInt((RichTankBlockEntity tank) -> tank.getBlockPos().getZ())
                        .thenComparingInt(tank -> tank.getBlockPos().getX()));
                int layerCapacity = Math.min(Integer.MAX_VALUE, perBlockCapacity * layer.size());
                int layerAmount = Math.min(remaining, layerCapacity);
                int each = layer.isEmpty() ? 0 : layerAmount / layer.size();
                int extra = layer.isEmpty() ? 0 : layerAmount % layer.size();
                for (int index = 0; index < layer.size(); index++) {
                    int part = each + (index < extra ? 1 : 0);
                    FluidStack next = part <= 0 || type == null || type.isEmpty()
                            ? FluidStack.EMPTY : type.copyWithAmount(part);
                    if (layer.get(index).setLocalFluidIfChanged(next)) changed.add(layer.get(index));
                }
                remaining -= layerAmount;
            }
            for (RichTankBlockEntity tank : changed) tank.syncLocal();
        }
    }
}
