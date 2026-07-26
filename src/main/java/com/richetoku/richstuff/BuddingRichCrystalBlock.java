package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Amethyst-compatible budding block used for every RichStuff gem/crystal family. */
public class BuddingRichCrystalBlock extends Block {
    private final String materialName;

    public BuddingRichCrystalBlock(Properties properties, String materialName) {
        super(properties.randomTicks());
        this.materialName = materialName;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        // Registry block-state caches are created before COMMON configs load.
        // Never read ModConfigSpec values from this method.
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!RichStuffConfig.ENABLE_CRYSTAL_GROWTH.get()
                || !RichStuffMaterialDefinitions.isMaterialEnabled(materialName)
                || random.nextInt(5) != 0) return;

        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos targetPos = pos.relative(direction);
        BlockState target = level.getBlockState(targetPos);
        Block next = null;

        if (target.isAir() || target.getFluidState().is(Fluids.WATER)) {
            next = RichStuff.blockOrNull("small_" + materialName + "_crystal_bud");
        } else if (isStage(target, "small_" + materialName + "_crystal_bud", direction)) {
            next = RichStuff.blockOrNull("medium_" + materialName + "_crystal_bud");
        } else if (isStage(target, "medium_" + materialName + "_crystal_bud", direction)) {
            next = RichStuff.blockOrNull("large_" + materialName + "_crystal_bud");
        } else if (isStage(target, "large_" + materialName + "_crystal_bud", direction)) {
            next = RichStuff.blockOrNull(materialName + "_crystal_cluster");
        }

        if (next == null) return;
        BlockState grown = next.defaultBlockState();
        // Some legacy catalog entries were previously registered as plain Blocks. Never attempt
        // to write the amethyst facing property unless the resolved growth stage actually exposes it.
        // This keeps old worlds safe and prevents random-tick crashes while data packs are reloaded.
        if (!grown.hasProperty(AmethystClusterBlock.FACING)) return;
        grown = grown.setValue(AmethystClusterBlock.FACING, direction);
        if (grown.hasProperty(AmethystClusterBlock.WATERLOGGED)) {
            grown = grown.setValue(AmethystClusterBlock.WATERLOGGED, target.getFluidState().is(Fluids.WATER));
        }
        level.setBlock(targetPos, grown, Block.UPDATE_ALL);
    }

    private static boolean isStage(BlockState state, String id, Direction direction) {
        Block block = RichStuff.blockOrNull(id);
        return block != null && state.is(block)
                && state.hasProperty(AmethystClusterBlock.FACING)
                && state.getValue(AmethystClusterBlock.FACING) == direction;
    }
}
