package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Casting table (molds/parts) or casting basin (storage blocks). */
public final class RichCastingBlock extends BaseEntityBlock {
    public static final MapCodec<RichCastingBlock> TABLE_CODEC = simpleCodec(properties -> new RichCastingBlock(properties, false));
    public static final MapCodec<RichCastingBlock> BASIN_CODEC = simpleCodec(properties -> new RichCastingBlock(properties, true));
    private final boolean basin;
    public RichCastingBlock(BlockBehaviour.Properties properties, boolean basin) { super(properties); this.basin=basin; }
    public boolean basin() { return basin; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){ return basin ? BASIN_CODEC : TABLE_CODEC; }
    @Override public RenderShape getRenderShape(BlockState state){ return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state){ return new RichCastingBlockEntity(pos,state); }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type){ return level.isClientSide?null:createTickerHelper(type,RichStuff.CASTING_ENTITY.get(),RichCastingBlockEntity::serverTick); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof RichCastingBlockEntity casting && !basin && casting.insertMold(stack)){ if(!player.getAbilities().instabuild) stack.shrink(1); return ItemInteractionResult.sidedSuccess(level.isClientSide); }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof RichCastingBlockEntity casting && casting.takeOutput(player)){ return InteractionResult.sidedSuccess(level.isClientSide); }
        return InteractionResult.PASS;
    }
}
