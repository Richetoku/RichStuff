package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BuddingRichCrystalBlock extends Block {
    private final String materialName;

    public BuddingRichCrystalBlock(Properties properties, String materialName) {
        super(properties.randomTicks());
        this.materialName = materialName;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return RichStuffConfig.ENABLE_CRYSTAL_GROWTH.get();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!RichStuffConfig.ENABLE_CRYSTAL_GROWTH.get() || random.nextInt(5) != 0) return;
        Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(dir);
        if (!level.isEmptyBlock(target)) return;
        var block = RichStuff.BLOCKS.get(materialName + "_block");
        if (block != null) level.setBlock(target, block.get().defaultBlockState(), Block.UPDATE_ALL);
    }
}
