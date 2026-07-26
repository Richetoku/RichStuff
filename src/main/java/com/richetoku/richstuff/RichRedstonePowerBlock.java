package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Full-strength redstone source matching the vanilla redstone storage block. */
public final class RichRedstonePowerBlock extends Block {
    public RichRedstonePowerBlock(Properties properties) { super(properties); }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 15; }
}
