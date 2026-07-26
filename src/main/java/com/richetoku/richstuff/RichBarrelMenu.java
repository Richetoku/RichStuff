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
    public static final int HEIGHT = 240;

    private final Container contents;
    @Nullable private final RichBarrelBlockEntity barrel;
    private final int tier;
    private final BlockPos pos;

    public RichBarrelMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(RichStuff.RICH_BARREL_MENU.get(), id);
        this.pos = buffer.readBlockPos();
        this.tier = Math.max(1, Math.min(7, buffer.readVarInt()));
        this.barrel = null;
        this.contents = new SimpleContainer(RichBarrelBlockEntity.SLOTS);
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
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new BarrelSlot(contents, tier, col + row * 9, 8 + col * 18, 20 + row * 18));
            }
        }
        int playerY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
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
        ItemStack copy = source.copy();
        if (index < RichBarrelBlockEntity.SLOTS) {
            if (!moveItemStackTo(source, RichBarrelBlockEntity.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 0, RichBarrelBlockEntity.SLOTS, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    private static final class BarrelSlot extends Slot {
        private final int tier;
        BarrelSlot(Container container, int tier, int index, int x, int y) {
            super(container, index, x, y);
            this.tier = tier;
        }
        private int limit(ItemStack stack) {
            int base = stack == null || stack.isEmpty() ? 64 : Math.max(1, stack.getMaxStackSize());
            return Math.min(4096, base << Math.max(0, tier - 1));
        }
        @Override public int getMaxStackSize() { return Math.min(4096, 64 << Math.max(0, tier - 1)); }
        @Override public int getMaxStackSize(ItemStack stack) { return limit(stack); }
    }
}
