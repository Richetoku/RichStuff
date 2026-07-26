package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.LinkedHashSet;
import java.util.Set;

/** Detects a hollow, rotated 3..16 block Rich Foundry with a lower front-center controller. */
public final class RichFoundryStructure {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 16;
    private RichFoundryStructure() {}

    public static Result find(Level level, BlockPos controllerPos, Direction facing) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        for (int width = MIN_SIZE; width <= MAX_SIZE; width++) {
            int controllerX = width / 2;
            BlockPos frontLeft = controllerPos.relative(right, -controllerX);
            for (int depth = MIN_SIZE; depth <= MAX_SIZE; depth++) {
                for (int height = MIN_SIZE; height <= MAX_SIZE; height++) {
                    Result result = validate(level, controllerPos, frontLeft, facing, right, back, width, depth, height, controllerX);
                    if (result != null) return result;
                }
            }
        }
        return Result.INVALID;
    }

    private static Result validate(Level level, BlockPos controllerPos, BlockPos frontLeft, Direction facing,
                                   Direction right, Direction back, int width, int depth, int height, int controllerX) {
        Set<BlockPos> tanks = new LinkedHashSet<>();
        Set<BlockPos> shell = new LinkedHashSet<>();
        int drains = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos pos = frontLeft.relative(right, x).relative(back, z).above(y);
                    boolean boundary = x == 0 || x == width - 1 || z == 0 || z == depth - 1 || y == 0 || y == height - 1;
                    if (!boundary) {
                        if (!level.getBlockState(pos).isAir()) return null;
                        continue;
                    }
                    shell.add(pos.immutable());
                    if (pos.equals(controllerPos)) {
                        if (x != controllerX || z != 0 || y != 0) return null;
                        continue;
                    }
                    Block block = level.getBlockState(pos).getBlock();
                    if (block == RichStuff.FOUNDRY_CASING.get()) continue;
                    if (block == RichStuff.FOUNDRY_DRAIN.get()) { drains++; continue; }
                    if (block instanceof RichTankBlock) { tanks.add(pos.immutable()); continue; }
                    return null;
                }
            }
        }
        if (drains <= 0) return null;
        BlockPos heatPos = frontLeft.relative(right, width / 2).relative(back, depth / 2).below();
        var heat = level.getBlockState(heatPos);
        boolean heated = heat.getFluidState().is(Fluids.LAVA) || heat.is(Blocks.FIRE) || heat.is(Blocks.SOUL_FIRE);
        if (!heated) return null;
        return new Result(true, frontLeft.immutable(), width, depth, height, drains, Set.copyOf(tanks), Set.copyOf(shell));
    }

    public static BlockPos interior(BlockPos controllerPos, Direction facing) {
        return controllerPos.relative(facing.getOpposite()).above();
    }

    public record Result(boolean formed, BlockPos frontLeft, int width, int depth, int height,
                         int drains, Set<BlockPos> tanks, Set<BlockPos> shell) {
        public static final Result INVALID = new Result(false, BlockPos.ZERO, 0, 0, 0, 0, Set.of(), Set.of());
        public boolean contains(BlockPos pos) { return formed && shell.contains(pos); }
        public int volume() { return formed ? width * depth * height : 0; }
    }
}
