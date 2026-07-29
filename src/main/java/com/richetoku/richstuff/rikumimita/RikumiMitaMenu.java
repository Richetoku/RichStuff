package com.richetoku.richstuff.rikumimita;

import com.richetoku.richstuff.RichStuff;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class RikumiMitaMenu extends AbstractContainerMenu {
    public static final int COMPANION_SLOTS = RikumiInventoryBridge.SLOT_COUNT;
    public static final int STORAGE_SLOTS = RikumiInventoryBridge.STORAGE_SLOTS;

    // Coordinates are shared with RikumiMitaScreen so rendered slot backplates and
    // actual interactive slots always stay aligned.
    public static final int EQUIPMENT_X = 132;
    public static final int MAIN_HAND_Y = 31;
    public static final int OFF_HAND_Y = 55;
    public static final int COMPANION_X = 168;
    public static final int COMPANION_Y = 28;
    public static final int PLAYER_X = 168;
    public static final int PLAYER_Y = 112;
    public static final int HOTBAR_Y = 170;

    private final RikumiMitaEntity rikumi;

    public RikumiMitaMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, find(playerInventory, buffer.readVarInt()));
    }

    private static RikumiMitaEntity find(Inventory inventory, int entityId) {
        Entity entity = inventory.player.level().getEntity(entityId);
        if (entity instanceof RikumiMitaEntity rikumi) return rikumi;
        throw new IllegalStateException("Rikumi Mita entity " + entityId + " was not found");
    }

    public RikumiMitaMenu(int id, Inventory playerInventory, RikumiMitaEntity rikumi) {
        super(RichStuff.RIKUMI_MITA_MENU.get(), id);
        this.rikumi = rikumi;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(
                        rikumi.getInventoryHandler(),
                        col + row * 9,
                        COMPANION_X + col * 18,
                        COMPANION_Y + row * 18));
            }
        }
        addSlot(new SlotItemHandler(rikumi.getInventoryHandler(), RikumiInventoryBridge.MAIN_HAND_SLOT,
                EQUIPMENT_X, MAIN_HAND_Y));
        addSlot(new SlotItemHandler(rikumi.getInventoryHandler(), RikumiInventoryBridge.OFF_HAND_SLOT,
                EQUIPMENT_X, OFF_HAND_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        PLAYER_X + col * 18,
                        PLAYER_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    public RikumiMitaEntity rikumi() {
        return rikumi;
    }

    @Override
    public boolean stillValid(Player player) {
        return rikumi.isAlive() && rikumi.mayConfigure(player) && player.distanceToSqr(rikumi) <= 64.0D;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        switch (id) {
            case 0 -> rikumi.cycleOutfit(-1);
            case 1 -> rikumi.cycleOutfit(1);
            case 2 -> rikumi.toggleSitFollow();
            case 3 -> rikumi.toggleVoiceWithDialogue();
            case 4 -> rikumi.toggleNameplateWithDialogue();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack source = slot.getItem();
            result = source.copy();
            if (index < COMPANION_SLOTS) {
                if (!moveItemStackTo(source, COMPANION_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(source, 0, COMPANION_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
