package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A RichStuff coin that forms a one-to-nine vertical coin stack when used on a block. */
public final class CoinItem extends Item {
    public CoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return placeCoins(context, context.getItemInHand(), 1);
    }

    /**
     * Resolves both the clicked blockspace and the adjacent placement blockspace before creating
     * a new pile. This lets players click the floor beneath a pile, or a neighboring block face,
     * without having to aim at the pile's small collision box.
     */
    static InteractionResult placeCoins(UseOnContext context, ItemStack sourceCoin, int amount) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos adjacent = clicked.relative(context.getClickedFace());

        for (BlockPos candidate : new BlockPos[]{clicked, adjacent}) {
            if (tryAddToExisting(level, candidate, sourceCoin, amount, context)) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        BlockState clickedState = level.getBlockState(clicked);
        BlockPos target = clickedState.canBeReplaced() ? clicked : adjacent;
        if (!level.getBlockState(target).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.setBlock(target, RichStuff.COIN_PILE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (!(level.getBlockEntity(target) instanceof CoinPileBlockEntity pile)
                    || !pile.addCoins(sourceCoin, amount)) {
                level.removeBlock(target, false);
                return InteractionResult.FAIL;
            }
            consumePlacedItem(context, amount);
            level.playSound(null, target, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.45F, 1.35F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean tryAddToExisting(Level level, BlockPos pos, ItemStack sourceCoin, int amount,
                                            UseOnContext context) {
        if (level.getBlockState(pos).getBlock() != RichStuff.COIN_PILE.get()
                || !(level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile)
                || !pile.canAccept(sourceCoin, amount)) {
            return false;
        }
        if (!level.isClientSide() && pile.addCoins(sourceCoin, amount)) {
            consumePlacedItem(context, amount);
            level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.45F, 1.35F);
        }
        return true;
    }

    private static void consumePlacedItem(UseOnContext context, int coinAmount) {
        if (context.getPlayer() != null && context.getPlayer().getAbilities().instabuild) {
            return;
        }
        // A CoinStackItem represents all nine coins in one inventory item.
        context.getItemInHand().shrink(coinAmount == CoinPileBlockEntity.MAX_COINS ? 1 : coinAmount);
    }
}
