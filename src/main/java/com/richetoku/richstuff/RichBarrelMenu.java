package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Extended-stack barrel menu whose client side is detached from the live world block entity. */
public final class RichBarrelMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 258;
    public static final int BARREL_COLUMNS = 8;
    public static final int BARREL_ROWS = 7;

    private final Container contents;
    @Nullable private final RichBarrelBlockEntity barrel;
    private final int tier;
    private final BlockPos pos;

    public RichBarrelMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(RichStuff.RICH_BARREL_MENU.get(), id);
        this.pos = buffer.readBlockPos();
        this.tier = Math.max(1, Math.min(7, buffer.readVarInt()));
        this.barrel = null;
        this.contents = new SimpleContainer(RichBarrelBlockEntity.SLOTS) {
            private int multiplier() { return 1 << Math.max(0, Math.min(6, RichBarrelMenu.this.tier - 1)); }
            @Override public int getMaxStackSize() { return Math.min(4096, 64 * multiplier()); }
            @Override public int getMaxStackSize(ItemStack stack) {
                int base = stack == null || stack.isEmpty() ? 64 : Math.max(1, stack.getMaxStackSize());
                return Math.min(4096, base * multiplier());
            }
        };
        addSlots(inventory);
    }

    public RichBarrelMenu(int id, Inventory inventory, RichBarrelBlockEntity barrel) {
        super(RichStuff.RICH_BARREL_MENU.get(), id);
        this.pos = barrel.getBlockPos();
        this.tier = barrel.tier();
        this.barrel = barrel;
        this.contents = barrel;
        checkContainerSize(contents, RichBarrelBlockEntity.SLOTS);
        addSlots(inventory);
    }

    private void addSlots(Inventory inventory) {
        for (int row = 0; row < BARREL_ROWS; row++) {
            for (int col = 0; col < BARREL_COLUMNS; col++) {
                addSlot(new BarrelSlot(contents, tier, col + row * BARREL_COLUMNS,
                        17 + col * 18, 20 + row * 18));
            }
        }
        int playerY = 158;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 216));
    }

    @Nullable public RichBarrelBlockEntity barrel() { return barrel; }
    public int tier() { return tier; }
    public BlockPos blockPos() { return pos; }

    @Override public boolean stillValid(Player player) { return barrel == null || barrel.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        if (index < RichBarrelBlockEntity.SLOTS) {
            // One shift-click removes at most one native stack. The logical barrel slot may hold
            // thousands, but player inventories and network packets must only receive legal stacks.
            int offeredCount = Math.min(source.getCount(), source.getMaxStackSize());
            ItemStack offered = source.copyWithCount(offeredCount);
            if (!moveItemStackTo(offered, RichBarrelBlockEntity.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            int moved = offeredCount - offered.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            ItemStack movedStack = source.copyWithCount(moved);
            source.shrink(moved);
            if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            return movedStack;
        }
        ItemStack copy = source.copy();
        if (!moveItemStackTo(source, 0, RichBarrelBlockEntity.SLOTS, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    private static final class BarrelSlot extends Slot {
        private final int tier;
        BarrelSlot(Container container, int tier, int index, int x, int y) {
            super(container, index, x, y);
            this.tier = tier;
        }
        private int multiplier() { return 1 << Math.max(0, Math.min(6, tier - 1)); }
        private int limit(ItemStack stack) {
            int base = stack == null || stack.isEmpty() ? 64 : Math.max(1, stack.getMaxStackSize());
            return Math.min(4096, base * multiplier());
        }
        @Override public int getMaxStackSize() { return Math.min(4096, 64 * multiplier()); }
        @Override public int getMaxStackSize(ItemStack stack) { return limit(stack); }
        @Override public ItemStack remove(int amount) {
            ItemStack stored = getItem();
            return super.remove(Math.min(amount, stored.isEmpty() ? 0 : stored.getMaxStackSize()));
        }
    }
}
