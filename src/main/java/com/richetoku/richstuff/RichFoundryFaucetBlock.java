package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Player- or redstone-triggered faucet that pours the selected molten fluid into a casting block below. */
public final class RichFoundryFaucetBlock extends Block {
    public RichFoundryFaucetBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) pour(level,pos); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state,level,pos,neighbor,neighborPos,moved); if(!level.isClientSide && level.hasNeighborSignal(pos)) pour(level,pos);
    }
    private static void pour(Level level, BlockPos pos) {
        RichFoundryBlockEntity foundry = RichFoundryDrainBlock.findController(level,pos);
        if (foundry == null || foundry.selectedFluid() == null) return;
        BlockPos target = pos.below();
        if (!(level.getBlockEntity(target) instanceof RichCastingBlockEntity casting)) return;
        casting.beginPour(foundry);
    }
}
