package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla redstone wire behavior with a real per-form power ceiling.
 *
 * <p>All placement, connection-shape, survival and neighbor propagation work is delegated to
 * vanilla first. RichStuff then clamps the stored POWER value and emitted signal. The former
 * implementation replaced vanilla neighborChanged entirely, which prevented normal connection
 * updates and was the cause of placed RichStuff redstone failing in-world.</p>
 */
public final class RichRedstoneWireBlock extends RedStoneWireBlock {
    private final int maxSignal;
    private boolean clamping;

    public RichRedstoneWireBlock(BlockBehaviour.Properties properties, int maxSignal) {
        super(properties);
        this.maxSignal = Math.max(1, Math.min(15, maxSignal));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        clampStoredPower(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        clampStoredPower(level, pos);
    }

    private void clampStoredPower(Level level, BlockPos pos) {
        if (level.isClientSide() || clamping) return;
        BlockState current = level.getBlockState(pos);
        if (!current.is(this) || !current.hasProperty(POWER)) return;
        int power = current.getValue(POWER);
        if (power <= maxSignal) return;
        clamping = true;
        try {
            level.setBlock(pos, current.setValue(POWER, maxSignal), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(pos, this);
            for (Direction direction : Direction.values()) level.updateNeighborsAt(pos.relative(direction), this);
        } finally {
            clamping = false;
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return Math.min(maxSignal, super.getSignal(state, level, pos, direction));
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return Math.min(maxSignal, super.getDirectSignal(state, level, pos, direction));
    }
}
