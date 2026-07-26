package com.richetoku.richstuff;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** A compact item representing exactly nine identical coins. */
public final class CoinStackItem extends Item {
    private final ResourceLocation coinId;

    public CoinStackItem(Properties properties, ResourceLocation coinId) {
        super(properties);
        this.coinId = coinId;
    }

    public ItemStack coinStack() {
        Item coin = BuiltInRegistries.ITEM.get(coinId);
        return coin == null ? ItemStack.EMPTY : new ItemStack(coin, CoinPileBlockEntity.MAX_COINS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack coins = coinStack();
        if (coins.isEmpty() || !(coins.getItem() instanceof CoinItem)) {
            return InteractionResult.FAIL;
        }
        return CoinItem.placeCoins(context, coins, CoinPileBlockEntity.MAX_COINS);
    }
}
