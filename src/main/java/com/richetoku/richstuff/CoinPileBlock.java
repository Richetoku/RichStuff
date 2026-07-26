package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Shared invisible host for one material of coin, stacked vertically from one to nine. */
public final class CoinPileBlock extends BaseEntityBlock {
    public static final MapCodec<CoinPileBlock> CODEC = simpleCodec(CoinPileBlock::new);
    /** Each rendered coin is one and a half model pixels thick. */
    public static final double COIN_HEIGHT_PIXELS = 1.5D;
    private static final VoxelShape EMPTY = Shapes.empty();

    public CoinPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoinPileBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) return shapeFor(pile.count());
        return EMPTY;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** A centered coin cylinder approximation whose height always matches the rendered stack. */
    public static VoxelShape shapeFor(int count) {
        if (count <= 0) return EMPTY;
        double height = count >= CoinPileBlockEntity.MAX_COINS
                ? 16.0D
                : Math.min(CoinPileBlockEntity.MAX_COINS, count) * COIN_HEIGHT_PIXELS;
        return Block.box(4.0D, 0.0D, 4.0D, 12.0D, height, 12.0D);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            removeOne(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.getItem() instanceof CoinItem && level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            if (!pile.canAccept(stack)) return ItemInteractionResult.FAIL;
            if (!level.isClientSide() && pile.add(stack) && !player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            removeOne(level, pos, player);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    private static void removeOne(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile)) return;
        ItemStack removed = pile.removeOne();
        if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
        if (pile.isEmpty()) level.removeBlock(pos, false);
    }

    /** Makes pick-block and probe-style overlays resolve the visible coin or compact nine-coin stack. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            return pile.displayOrDropStack().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            pile.dropAll(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
