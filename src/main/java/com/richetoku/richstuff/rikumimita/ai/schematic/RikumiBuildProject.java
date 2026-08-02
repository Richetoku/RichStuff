package com.richetoku.richstuff.rikumimita.ai.schematic;

import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiAction;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

/** Persistent, orientation-aware, one-block-at-a-time schematic construction project. */
public final class RikumiBuildProject {
    private final RikumiSchematic schematic;
    private final BlockPos origin;
    @Nullable private final BlockPos markerPos;
    private final Direction facing;
    private int index;
    private long nextBuildTick;
    private boolean cancelled;
    private boolean completionHandled;

    public RikumiBuildProject(RikumiSchematic schematic, BlockPos origin) {
        this(schematic, origin, null, Direction.SOUTH, 0);
    }

    public RikumiBuildProject(RikumiSchematic schematic, BlockPos origin, int completedPlacements) {
        this(schematic, origin, null, Direction.SOUTH, completedPlacements);
    }

    public RikumiBuildProject(RikumiSchematic schematic, BlockPos origin, @Nullable BlockPos markerPos,
                              Direction facing, int completedPlacements) {
        this.schematic = schematic;
        this.origin = origin.immutable();
        this.markerPos = markerPos == null ? null : markerPos.immutable();
        this.facing = facing == null || facing.getAxis().isVertical() ? Direction.SOUTH : facing;
        this.index = Math.max(0, Math.min(schematic.placements().size(), completedPlacements));
    }

    public ResourceLocation schematicId() { return schematic.id(); }
    public String displayName() { return schematic.displayName(); }
    public BlockPos origin() { return origin; }
    @Nullable public BlockPos markerPos() { return markerPos; }
    public Direction facing() { return facing; }
    public int placed() { return index; }
    public int total() { return schematic.placements().size(); }
    public String progressDescription() {
        int remaining = Math.max(0, total() - index);
        String anchor = markerPos == null ? origin.toShortString()
                : markerPos.toShortString() + " facing " + facing.getSerializedName();
        return displayName() + ": " + index + " of " + total() + " blocks placed; " + remaining
                + " remaining; marker " + anchor;
    }
    public boolean complete() { return cancelled || index >= total(); }
    public void cancel() { cancelled = true; }

    public Map<ResourceLocation, Integer> remainingMaterials() {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        for (int i = index; i < schematic.placements().size(); i++) {
            RikumiSchematic.Placement placement = schematic.placements().get(i);
            if (!placement.consumesItem()) continue;
            ResourceLocation item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(placement.state().getBlock().asItem());
            if (item != null && !item.equals(ResourceLocation.withDefaultNamespace("air"))) result.merge(item, 1, Integer::sum);
        }
        return result;
    }

    public boolean tick(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (cancelled) return false;
        if (index >= total()) {
            finish(level, player, avatar);
            return false;
        }
        if (!avatar.hasHome()) {
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
            avatar.setGoalStatus("Set Home", "Set Rikumi's home before starting construction");
            avatar.setTaskStatus("Waiting", "Construction is disabled until a home is set");
            return false;
        }
        if (level.getGameTime() < nextBuildTick) return false;
        nextBuildTick = level.getGameTime() + 8L;

        while (index < schematic.placements().size()) {
            RikumiSchematic.Placement placement = schematic.placements().get(index);
            BlockPos target = origin.offset(rotateOffset(placement.offset()));
            BlockState wanted = placement.state().rotate(rotation());
            if (level.getBlockState(target).equals(wanted)) {
                index++;
                continue;
            }
            if (!level.getBlockState(target).canBeReplaced()) {
                avatar.setGoalStatus("Clear Build Site", "Clear the build site for " + displayName());
                avatar.setTaskStatus("Build Blocked", "Blocked at " + target.toShortString());
                return false;
            }

            avatar.setGoalStatus("Build Project", "Build " + displayName() + " (" + index + "/" + total() + ")");
            if (!avatar.canWorkAt(target)) {
                avatar.getNavigation().stop();
                avatar.setTaskStatus("Outside Home", "The next build position is outside the home work area");
                return false;
            }
            BlockPos stand = findStandPosition(level, target, avatar.blockPosition());
            if (avatar.distanceToSqr(stand.getCenter()) > 4.0D || avatar.distanceToSqr(target.getCenter()) > 9.0D) {
                avatar.setTaskStatus("Traveling", "Walking next to the build position at " + target.toShortString());
                avatar.setActionState(RikumiAction.WALK, 12);
                avatar.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 1.05D);
                return false;
            }
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);

            Block block = wanted.getBlock();
            if (placement.consumesItem() && !RikumiRecipeKnowledge.ensureBlockItem(player, block)) {
                ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(block.asItem());
                avatar.setTaskStatus("Needs Materials", "Needs " + (itemId == null ? block.getName().getString() : itemId));
                avatar.setGoalStatus("Gather Materials", "Gather or craft materials for " + displayName());
                return false;
            }
            if (placement.consumesItem() && !RikumiRecipeKnowledge.consumeBlockItem(player, block)) return false;

            level.setBlock(target, wanted, 3);
            avatar.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            avatar.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            avatar.setActionState(RikumiAction.BUILD, 12);
            avatar.setTaskStatus("Building", "Placed " + block.getName().getString() + " at " + target.toShortString());
            index++;
            if (index >= total()) finish(level, player, avatar);
            return true;
        }
        finish(level, player, avatar);
        return false;
    }

    private void finish(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (completionHandled || cancelled || index < total()) return;
        completionHandled = true;
        if (markerPos != null && level.getBlockEntity(markerPos) instanceof RikumiSchematicMarkerBlockEntity marker) {
            marker.removeSchematic();
            level.removeBlock(markerPos, false);
            giveOrDrop(level, player, new ItemStack(RichStuff.RIKUMI_SCHEMATIC_MARKER_ITEM.get()));
        }
        if (schematic.id().equals(RikumiSchematicItem.STARTER_HOUSE)) avatar.markStarterHouseCompleted();
        // Schematic authoring is a planning artifact rather than a consumed building material.
        giveOrDrop(level, player, RikumiSchematicItem.create(RichStuff.RIKUMI_SCHEMATIC_ITEM.get(), schematic.id()));
        avatar.setGoalStatus("Project Complete", "Completed " + displayName());
        avatar.setTaskStatus("Complete", "Project complete; recovered the reusable placement marker and saved the schematic");
        avatar.sendDialogueToOwner("I finished the " + displayName() + "!");
    }

    private void giveOrDrop(ServerLevel level, FakePlayer player, ItemStack stack) {
        ItemStack remainder = stack.copy();
        if (player.getInventory().add(remainder)) return;
        Block.popResource(level, markerPos == null ? player.blockPosition() : markerPos, remainder);
    }

    private static BlockPos findStandPosition(ServerLevel level, BlockPos target, BlockPos current) {
        BlockPos best = current;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = target.relative(direction);
            if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) continue;
            BlockPos head = candidate.above();
            if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) continue;
            BlockPos floor = candidate.below();
            if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) continue;
            double distance = current.distSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private Rotation rotation() {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private BlockPos rotateOffset(BlockPos offset) {
        return switch (rotation()) {
            case CLOCKWISE_90 -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180 -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }
}
