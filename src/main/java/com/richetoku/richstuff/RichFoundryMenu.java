package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Full controller menu for the standard and Alloy Foundry multiblocks. */
public final class RichFoundryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 260;
    public static final int HEIGHT = 232;
    private final RichFoundryBlockEntity foundry;
    private final ContainerData data;

    public RichFoundryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, find(inventory, buffer.readBlockPos()));
    }

    private static RichFoundryBlockEntity find(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof RichFoundryBlockEntity foundry) return foundry;
        throw new IllegalStateException("Rich Foundry at " + pos + " was not found");
    }

    public RichFoundryMenu(int id, Inventory inventory, RichFoundryBlockEntity foundry) {
        super(RichStuff.FOUNDRY_MENU.get(), id);
        this.foundry = foundry;
        this.data = foundry.dataAccess();
        checkContainerSize(foundry, RichFoundryBlockEntity.INPUTS);
        checkContainerDataCount(data, 15);
        for (int row=0; row<3; row++) for (int col=0; col<3; col++)
            addSlot(new Slot(foundry, col+row*3, 24+col*18, 52+row*18));
        int px=49, py=150;
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,px+col*18,py+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,px+col*18,208));
        addDataSlots(data);
    }

    public RichFoundryBlockEntity foundry() { return foundry; }
    public boolean formed() { return data.get(0)!=0; }
    public boolean alloying() { return data.get(1)!=0; }
    public int meltProgress() { return data.get(2); }
    public int alloyProgress() { return data.get(3); }
    public int totalAmount() { return data.get(4); }
    public int baseCapacity() { return data.get(5); }
    public int tankCapacity() { return data.get(6); }
    public int totalCapacity() { return Math.max(1,data.get(7)); }
    public int fluidCount() { return data.get(8); }
    public int selectedAmount() { return data.get(9); }
    public int width() { return data.get(10); }
    public int depth() { return data.get(11); }
    public int height() { return data.get(12); }
    public int drains() { return data.get(13); }
    public int tankCount() { return data.get(14); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && stillValid(player)) { foundry.cycleSelectedFluid(); return true; }
        return false;
    }
    @Override public boolean stillValid(Player player) { return foundry.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result=ItemStack.EMPTY; Slot slot=slots.get(index); if(!slot.hasItem())return result;
        ItemStack source=slot.getItem(); result=source.copy();
        if(index<RichFoundryBlockEntity.INPUTS){if(!moveItemStackTo(source,RichFoundryBlockEntity.INPUTS,slots.size(),true))return ItemStack.EMPTY;}
        else if(RichFoundryRecipes.findMelting(source)!=null){if(!moveItemStackTo(source,0,RichFoundryBlockEntity.INPUTS,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged(); return result;
    }
}
