package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

/**
 * 56-slot tiered barrel (a full double-chest worth plus two utility slots). Each logical slot stores exactly tier times the item's native stack size.
 *
 * Automation intentionally treats the barrel as one merging inventory instead of 56 unrelated
 * vanilla slots. Matching stacks are filled to the tier limit first, then remaining items are
 * allocated into empty slots. This keeps funnels, hoppers and pipes from creating small duplicate
 * stacks while an earlier matching slot still has room.
 */
public final class RichBarrelBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOTS = 56;
    private static final String ITEM_DATA = "RichBarrel";
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private static final int[] ALL_SLOTS = IntStream.range(0, SLOTS).toArray();
    private final IItemHandler automationHandler = new MergingItemHandler();

    public RichBarrelBlockEntity(BlockPos pos, BlockState state) { super(RichStuff.RICH_BARREL_ENTITY.get(), pos, state); }
    public int tier() { return getBlockState().getBlock() instanceof RichBarrelBlock barrel ? barrel.tier() : 1; }
    private int tierMultiplier() { return 1 << Math.max(0, Math.min(6, tier() - 1)); }
    public int slotLimit() { return Math.min(4096, 64 * tierMultiplier()); }
    /** Exponential tier storage: I=1x through VII=64x, while respecting each item's native size. */
    public int slotLimit(ItemStack stack) {
        int base = stack == null || stack.isEmpty() ? 64 : Math.max(1, stack.getMaxStackSize());
        return Math.min(4096, base * tierMultiplier());
    }
    public IItemHandler automationHandler() { return automationHandler; }

    public java.util.List<ItemStack> contentsForWorldDrops() {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        for (ItemStack stored : items) {
            if (stored.isEmpty()) continue;
            int remaining = stored.getCount();
            int max = Math.max(1, stored.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(remaining, max);
                drops.add(stored.copyWithCount(amount));
                remaining -= amount;
            }
        }
        return drops;
    }


    public void saveToItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        if (isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            stack.remove(DataComponents.MAX_STACK_SIZE);
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = new CompoundTag(); saveAdditional(data, level.registryAccess());
        root.put(ITEM_DATA, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
    }
    public void loadFromItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(ITEM_DATA)) loadAdditional(root.getCompound(ITEM_DATA), level.registryAccess());
        sync();
    }
    /** Menu slot packets already synchronize viewers; avoid broadcasting all extended stacks on every insert. */
    private void sync() { setChanged(); }

    @Override public Component getDisplayName() { return Component.translatable("container.richstuff.rich_barrel", tier()); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new RichBarrelMenu(id, inventory, this); }
    @Override public int getContainerSize() { return SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOTS ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int nativeAmount = Math.min(Math.max(0, amount), existing.getMaxStackSize());
        ItemStack result = ContainerHelper.removeItem(items, slot, nativeAmount);
        if (!result.isEmpty()) sync();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int nativeAmount = Math.min(existing.getCount(), existing.getMaxStackSize());
        ItemStack result = existing.copyWithCount(nativeAmount);
        existing.shrink(nativeAmount);
        if (existing.isEmpty()) items.set(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        ItemStack stored = stack.copy();
        int limit = slotLimit(stored);
        if (stored.getCount() > limit) stored.setCount(limit);
        items.set(slot, stored);
        sync();
    }
    @Override public int getMaxStackSize() { return slotLimit(); }
    @Override public int getMaxStackSize(ItemStack stack) { return slotLimit(stack); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getCenter())<=64.0D; }
    @Override public void clearContent() { items.clear(); sync(); }
    @Override public int[] getSlotsForFace(Direction side) { return ALL_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return true; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    /**
     * Vanilla ItemStack NBT limits Count to 99 in 1.21.1. Rich Barrels intentionally support counts
     * above that, so each stack is encoded with a safe count and its actual extended count is stored
     * separately. This prevents autosave from throwing forever once a slot reaches 100.
     */
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag encoded = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.putInt("ExtendedCount", stack.getCount());
            entry.put("Stack", stack.copyWithCount(Math.min(99, Math.max(1, stack.getCount()))).save(registries));
            encoded.add(entry);
        }
        tag.put("ExtendedItems", encoded);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        if (tag.contains("ExtendedItems", Tag.TAG_LIST)) {
            ListTag encoded = tag.getList("ExtendedItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < encoded.size(); i++) {
                CompoundTag entry = encoded.getCompound(i);
                int slot = entry.getByte("Slot") & 255;
                if (slot < 0 || slot >= items.size() || !entry.contains("Stack", Tag.TAG_COMPOUND)) continue;
                ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
                if (stack.isEmpty()) continue;
                int count = Math.max(1, entry.getInt("ExtendedCount"));
                stack.setCount(Math.min(slotLimit(stack), count));
                items.set(slot, stack);
            }
            return;
        }

        // One-time compatibility path for pre-0.0.5 barrels whose stacks were still vanilla-sized.
        try {
            ContainerHelper.loadAllItems(tag, items, registries);
        } catch (RuntimeException exception) {
            RichStuff.LOGGER.error("Could not load legacy Rich Barrel contents at {}; invalid stacks were discarded", worldPosition, exception);
            items.clear();
        }
    }

    private final class MergingItemHandler implements IItemHandler {
        @Override public int getSlots() { return SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) {
            ItemStack stored = RichBarrelBlockEntity.this.getItem(slot);
            return stored.isEmpty() ? ItemStack.EMPTY : stored.copyWithCount(Math.min(stored.getCount(), stored.getMaxStackSize()));
        }

        /**
         * The requested slot is accepted as an automation hint only. Insertion always performs a
         * deterministic whole-barrel merge pass so transport systems cannot skip partially filled
         * extended stacks by probing a later empty slot.
         */
        @Override public ItemStack insertItem(int slot, ItemStack offered, boolean simulate) {
            if (offered.isEmpty() || slot < 0 || slot >= SLOTS) return offered;
            int remaining = offered.getCount();
            int limit = slotLimit(offered);

            // Pass 1: fill every existing matching stack to this barrel tier's extended limit.
            for (int index = 0; index < SLOTS && remaining > 0; index++) {
                ItemStack existing = items.get(index);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, offered)) continue;
                int move = Math.min(remaining, Math.max(0, limit - existing.getCount()));
                if (move <= 0) continue;
                if (!simulate) existing.grow(move);
                remaining -= move;
            }

            // Pass 2: allocate the remainder into new slots only after all matching slots are full.
            for (int index = 0; index < SLOTS && remaining > 0; index++) {
                if (!items.get(index).isEmpty()) continue;
                int move = Math.min(remaining, limit);
                if (!simulate) items.set(index, offered.copyWithCount(move));
                remaining -= move;
            }
            if (!simulate && remaining != offered.getCount()) sync();
            return remaining <= 0 ? ItemStack.EMPTY : offered.copyWithCount(remaining);
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= SLOTS || amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            // Expose one native item stack per extraction operation even though the logical slot
            // may hold several stacks. Hoppers, funnels and other handlers therefore interact with
            // the barrel as a normal slot at a time without losing the extended internal count.
            int extracted = Math.min(Math.min(amount, existing.getMaxStackSize()), existing.getCount());
            ItemStack result = existing.copyWithCount(extracted);
            if (!simulate) {
                existing.shrink(extracted);
                if (existing.isEmpty()) items.set(slot, ItemStack.EMPTY);
                sync();
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) {
            ItemStack existing = slot >= 0 && slot < SLOTS ? items.get(slot) : ItemStack.EMPTY;
            return slotLimit(existing);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot >= 0 && slot < SLOTS && !stack.isEmpty(); }
    }
}
