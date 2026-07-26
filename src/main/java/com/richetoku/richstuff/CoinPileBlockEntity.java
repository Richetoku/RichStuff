package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Stores one RichStuff coin type with a visible vertical stack size from one to nine. */
public final class CoinPileBlockEntity extends BlockEntity {
    public static final int MAX_COINS = 9;
    private final NonNullList<ItemStack> contents = NonNullList.withSize(1, ItemStack.EMPTY);

    public CoinPileBlockEntity(BlockPos pos, BlockState state) {
        super(RichStuff.COIN_PILE_ENTITY.get(), pos, state);
    }

    public ItemStack coin() {
        return contents.getFirst();
    }

    public int count() {
        return coin().isEmpty() ? 0 : Math.min(MAX_COINS, coin().getCount());
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public boolean isFull() {
        return count() >= MAX_COINS;
    }

    public boolean canAccept(ItemStack source) {
        return canAccept(source, 1);
    }

    public boolean canAccept(ItemStack source, int amount) {
        if (amount <= 0 || source.isEmpty() || !(source.getItem() instanceof CoinItem)) return false;
        ItemStack stored = coin();
        return count() + amount <= MAX_COINS && (stored.isEmpty() || stored.getItem() == source.getItem());
    }

    public boolean add(ItemStack source) {
        return addCoins(source, 1);
    }

    public boolean addCoins(ItemStack source, int amount) {
        if (!canAccept(source, amount)) return false;
        ItemStack stored = coin();
        if (stored.isEmpty()) contents.set(0, source.copyWithCount(amount));
        else stored.grow(amount);
        sync();
        return true;
    }

    public ItemStack removeOne() {
        ItemStack stored = coin();
        if (stored.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stored.copyWithCount(1);
        stored.shrink(1);
        if (stored.isEmpty()) contents.set(0, ItemStack.EMPTY);
        sync();
        return result;
    }

    /** Returns the compact nine-coin item when this is a full stack. */
    public ItemStack displayOrDropStack() {
        if (isEmpty()) return ItemStack.EMPTY;
        if (count() == MAX_COINS) {
            ResourceLocation coinKey = BuiltInRegistries.ITEM.getKey(coin().getItem());
            if (coinKey != null) {
                ResourceLocation stackKey = ResourceLocation.fromNamespaceAndPath(
                        coinKey.getNamespace(), coinKey.getPath() + "_stack");
                Item stackItem = BuiltInRegistries.ITEM.get(stackKey);
                if (stackItem instanceof CoinStackItem) {
                    return new ItemStack(stackItem);
                }
            }
        }
        return coin().copy();
    }

    public void dropAll(Level level) {
        if (level.isClientSide() || isEmpty()) return;
        Block.popResource(level, worldPosition, displayOrDropStack());
        contents.set(0, ItemStack.EMPTY);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, contents, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        ContainerHelper.loadAllItems(tag, contents, registries);
        if (!coin().isEmpty() && coin().getCount() > MAX_COINS) coin().setCount(MAX_COINS);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
