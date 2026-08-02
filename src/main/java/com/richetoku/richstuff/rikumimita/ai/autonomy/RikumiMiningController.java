package com.richetoku.richstuff.rikumimita.ai.autonomy;

import com.richetoku.richstuff.rikumimita.RikumiAction;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

/** Server-authoritative, player-speed block breaking shared by all Rikumi control paths. */
public final class RikumiMiningController {
    public enum Result { IN_PROGRESS, COMPLETE, BLOCKED, PROTECTED }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private RikumiMiningController() {}

    /** Advances an existing mining session every server tick. */
    public static boolean continueActive(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        Session session = SESSIONS.get(avatar.getUUID());
        if (session == null) return false;
        Result result = advance(level, player, avatar, session.target(), session.goal(), session.detail());
        return result == Result.IN_PROGRESS || result == Result.COMPLETE;
    }

    /** Starts a new target or advances the current target immediately. */
    public static Result mine(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar,
                              BlockPos target, String goal, String detail) {
        Session current = SESSIONS.get(avatar.getUUID());
        if (current == null || !current.target().equals(target)) {
            clear(level, avatar);
            current = new Session(target.immutable(), 0.0F, level.getGameTime(), goal, detail, null, 0);
            SESSIONS.put(avatar.getUUID(), current);
        }
        return advance(level, player, avatar, target, goal, detail);
    }

    public static boolean hasActive(RikumiMitaEntity avatar) {
        return SESSIONS.containsKey(avatar.getUUID());
    }

    public static void clear(ServerLevel level, RikumiMitaEntity avatar) {
        Session old = SESSIONS.remove(avatar.getUUID());
        if (old != null) level.destroyBlockProgress(avatar.getId(), old.target(), -1);
    }

    private static Result advance(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar,
                                  BlockPos target, String goal, String detail) {
        if (!avatar.hasHome()) {
            clear(level, avatar);
            avatar.setGoalStatus("Set Home", "Set a home before Rikumi starts mining or working");
            avatar.setTaskStatus("Waiting", "Use Set Home in Rikumi's GUI");
            return Result.BLOCKED;
        }
        if (!avatar.canWorkAt(target)) {
            clear(level, avatar);
            avatar.setTaskStatus("Outside Home", "Skipped work outside the home area at " + target.toShortString());
            return Result.PROTECTED;
        }
        BlockState state = level.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(level, target) < 0.0F) {
            clear(level, avatar);
            avatar.setTaskStatus("Blocked", "The target at " + target.toShortString() + " cannot be mined");
            return Result.BLOCKED;
        }
        if (!RikumiPlacementLedger.mayRikumiBreak(level, target)) {
            clear(level, avatar);
            avatar.setTaskStatus("Protected", "Skipped a player-placed block at " + target.toShortString());
            return Result.PROTECTED;
        }

        // Mining progress never advances at range. Rikumi first walks to an adjacent stand position,
        // then stops completely beside the target just like a player working in reach.
        if (avatar.distanceToSqr(target.getCenter()) > 9.0D) {
            Session old = SESSIONS.get(avatar.getUUID());
            BlockPos stand = old == null ? null : old.stand();
            if (stand == null || stand.distSqr(target) > 10.0D || !isReachableStand(level, avatar, stand))
                stand = findStandPosition(level, avatar, target, avatar.blockPosition());
            if (stand == null) {
                clear(level, avatar);
                avatar.getNavigation().stop();
                avatar.setTaskStatus("Route Blocked", "No reachable standing position exists beside "
                        + state.getBlock().getName().getString() + " at " + target.toShortString());
                return Result.BLOCKED;
            }
            long now = level.getGameTime();
            if (avatar.getNavigation().isDone() || old == null || now - old.lastTick() >= 20L)
                avatar.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 1.05D);
            avatar.setGoalStatus(goal, detail);
            avatar.setTaskStatus("Approaching", "Walking into player reach of "
                    + state.getBlock().getName().getString());
            avatar.setActionState(RikumiAction.WALK, 8);
            float saved = old == null ? 0.0F : old.progress();
            long pathTick = old == null || now - old.lastTick() >= 20L ? now : old.lastTick();
            SESSIONS.put(avatar.getUUID(), new Session(target.immutable(), saved, pathTick, goal, detail, stand.immutable(), old == null ? 0 : old.failedFinishes()));
            return Result.IN_PROGRESS;
        }
        avatar.getNavigation().stop();
        avatar.setDeltaMovement(Vec3.ZERO);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(avatar.getX(), avatar.getY(), avatar.getZ(), avatar.getYRot(), avatar.getXRot());
        player.setOnGround(avatar.onGround());
        player.fallDistance = avatar.fallDistance;

        int bestSlot = bestToolSlot(player, state, level, target);
        if (bestSlot == Integer.MIN_VALUE) {
            clear(level, avatar);
            avatar.setTaskStatus("Needs Tool", "A suitable tool is required to mine " + state.getBlock().getName().getString());
            return Result.BLOCKED;
        }
        if (bestSlot >= 0) swapIntoSelected(player, bestSlot);
        avatar.syncDisplayedHands(player);
        avatar.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        avatar.setGoalStatus(goal, detail);
        String workVerb = taskDescriptor(state, player.getMainHandItem());
        avatar.setTaskStatus(workVerb, workVerb + " " + state.getBlock().getName().getString()
                + " at " + target.toShortString());
        avatar.setActionState(RikumiAction.MINE, 4);
        if ((level.getGameTime() & 3L) == 0L) avatar.swing(InteractionHand.MAIN_HAND, true);

        Session old = SESSIONS.get(avatar.getUUID());
        long now = level.getGameTime();
        long elapsed = old == null ? 1L : Math.max(1L, Math.min(20L, now - old.lastTick()));
        float perTick = state.getDestroyProgress(player, level, target);
        if (perTick <= 0.0F) {
            clear(level, avatar);
            avatar.setTaskStatus("Blocked", "The selected tool cannot damage " + state.getBlock().getName().getString());
            return Result.BLOCKED;
        }
        float progress = (old == null ? 0.0F : old.progress()) + perTick * elapsed;
        SESSIONS.put(avatar.getUUID(), new Session(target.immutable(), progress, now, goal, detail,
                old == null ? null : old.stand(), old == null ? 0 : old.failedFinishes()));
        int crack = Math.min(9, Math.max(0, (int) (progress * 10.0F)));
        level.destroyBlockProgress(avatar.getId(), target, crack);
        if (progress < 1.0F) return Result.IN_PROGRESS;

        String minedName = state.getBlock().getName().getString();
        boolean destroyed = player.gameMode.destroyBlock(target) || level.getBlockState(target).isAir();
        int failedFinishes = old == null ? 0 : old.failedFinishes();
        if (!destroyed && failedFinishes >= 2 && RikumiPlacementLedger.mayRikumiBreak(level, target)) {
            // The normal FakePlayer path remains authoritative. This protected fallback only runs after
            // repeated final-stage failures and uses the FakePlayer as the breaker/drop source, preventing
            // a permanent 100% crack loop while preserving normal survival drops.
            destroyed = level.destroyBlock(target, true, player) || level.getBlockState(target).isAir();
        }
        if (!destroyed) {
            SESSIONS.put(avatar.getUUID(), new Session(target.immutable(), 1.0F, now, goal, detail,
                    old == null ? null : old.stand(), failedFinishes + 1));
            avatar.setTaskStatus("Finishing", "Finishing " + workVerb.toLowerCase(java.util.Locale.ROOT)
                    + " " + minedName + " at " + target.toShortString());
            return Result.IN_PROGRESS;
        }
        clear(level, avatar);
        avatar.swing(InteractionHand.MAIN_HAND, true);
        avatar.rememberMinedBlock(target);
        String completedVerb = switch (workVerb) {
            case "Chopping" -> "Chopped";
            case "Digging" -> "Dug";
            case "Clearing" -> "Cleared";
            case "Mining" -> "Mined";
            default -> "Broke";
        };
        avatar.setTaskStatus(completedVerb, completedVerb + " " + minedName + " at " + target.toShortString());
        return Result.COMPLETE;
    }

    @Nullable
    private static BlockPos findStandPosition(ServerLevel level, RikumiMitaEntity avatar,
                                              BlockPos target, BlockPos current) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos currentStand = avatar.blockPosition();
        if (currentStand.distSqr(target) <= 10.0D && isReachableStand(level, avatar, currentStand)) {
            best = currentStand.immutable();
            bestDistance = 0.0D;
        }
        BlockPos lastMined = avatar.getLastMinedBlock();
        if (lastMined != null && lastMined.distSqr(target) <= 10.0D
                && isReachableStand(level, avatar, lastMined)) {
            double distance = lastMined.distSqr(current);
            if (distance < bestDistance) { bestDistance = distance; best = lastMined.immutable(); }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = target.relative(direction);
            if (!isReachableStand(level, avatar, candidate)) continue;
            double distance = candidate.distSqr(current);
            if (distance < bestDistance) { bestDistance = distance; best = candidate.immutable(); }
        }
        // Mining stairs may expose the block from one level above or below at a switchback turn.
        for (BlockPos candidate : new BlockPos[]{target.above(), target.below()}) {
            if (!isReachableStand(level, avatar, candidate)) continue;
            double distance = candidate.distSqr(current);
            if (distance < bestDistance) { bestDistance = distance; best = candidate.immutable(); }
        }
        return best;
    }

    private static boolean isReachableStand(ServerLevel level, RikumiMitaEntity avatar, BlockPos candidate) {
        if (candidate == null || !avatar.canWorkAt(candidate)) return false;
        if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) return false;
        if (!level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) return false;
        if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) return false;
        if (avatar.distanceToSqr(candidate.getCenter()) <= 9.0D) return true;
        Path path = avatar.getNavigation().createPath(candidate, 0);
        return path != null && path.canReach();
    }

    /**
     * Selects tools using Minecraft's own player break-progress calculation. This includes the
     * block's preferred tool type, harvest requirements, Efficiency and other enchantments, active
     * effects, underwater/airborne penalties, and the FakePlayer's current attributes.
     */
    private static int bestToolSlot(FakePlayer player, BlockState state, ServerLevel level, BlockPos target) {
        int selected = player.getInventory().selected;
        ItemStack selectedOriginal = player.getInventory().getItem(selected);
        int bestSlot = selected;
        float bestProgress = evaluatePlayerProgress(player, state, level, target, selectedOriginal);
        boolean hasCorrectTool = selectedOriginal.isCorrectToolForDrops(state);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            boolean correct = stack.isCorrectToolForDrops(state);
            hasCorrectTool |= correct;
            float candidate = evaluatePlayerProgress(player, state, level, target, stack);
            if (candidate > bestProgress + 0.000001F
                    || Math.abs(candidate - bestProgress) < 0.000001F && correct
                    && !player.getInventory().getItem(bestSlot).isCorrectToolForDrops(state)) {
                bestProgress = candidate;
                bestSlot = slot;
            }
        }
        player.getInventory().setItem(selected, selectedOriginal);
        if (state.requiresCorrectToolForDrops() && !hasCorrectTool) return Integer.MIN_VALUE;
        return bestSlot == selected ? -1 : bestSlot;
    }

    private static float evaluatePlayerProgress(FakePlayer player, BlockState state, ServerLevel level,
                                                BlockPos target, ItemStack candidate) {
        int selected = player.getInventory().selected;
        ItemStack original = player.getInventory().getItem(selected);
        player.getInventory().setItem(selected, candidate);
        float progress = state.getDestroyProgress(player, level, target);
        player.getInventory().setItem(selected, original);
        return progress;
    }

    private static String taskDescriptor(BlockState state, ItemStack tool) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.MINEABLE_WITH_AXE) || tool.is(ItemTags.AXES))
            return "Chopping";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL) || tool.is(ItemTags.SHOVELS))
            return "Digging";
        if (state.is(BlockTags.MINEABLE_WITH_HOE) || tool.is(ItemTags.HOES))
            return "Clearing";
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || tool.is(ItemTags.PICKAXES))
            return "Mining";
        return "Breaking";
    }

    private static void swapIntoSelected(FakePlayer player, int slot) {
        if (slot < 0) return;
        int selected = player.getInventory().selected;
        if (slot == selected) return;
        ItemStack target = player.getInventory().getItem(slot).copy();
        ItemStack current = player.getInventory().getItem(selected).copy();
        player.getInventory().setItem(selected, target);
        player.getInventory().setItem(slot, current);
        player.getInventory().setChanged();
    }

    private record Session(BlockPos target, float progress, long lastTick, String goal, String detail,
                           @Nullable BlockPos stand, int failedFinishes) {}
}
