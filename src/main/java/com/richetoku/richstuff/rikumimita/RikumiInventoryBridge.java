package com.richetoku.richstuff.rikumimita;

import com.richetoku.richstuff.rikumimita.ai.RikumiAiLifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * Persistent, server-authoritative inventory shared by the native Rikumi menu and her fake-player
 * action executor. The handler is the canonical inventory; it never exposes mutable references from
 * the fake player to menu slots. This prevents client-side slot mutation from being overwritten by
 * a second live inventory and fixes the duplicate-then-vanish/remove-then-reappear behavior.
 */
public final class RikumiInventoryBridge extends ItemStackHandler {
    public static final int STORAGE_SLOTS = 27;
    public static final int MAIN_HAND_SLOT = STORAGE_SLOTS;
    public static final int OFF_HAND_SLOT = STORAGE_SLOTS + 1;
    public static final int SLOT_COUNT = STORAGE_SLOTS + 2;

    private static final int ACTOR_STORAGE_START = 9;

    private final Runnable dirtyCallback;
    private final ItemStack[] actorSnapshot = new ItemStack[SLOT_COUNT];
    private FakePlayer attachedPlayer;
    private boolean synchronizing;

    public RikumiInventoryBridge(Runnable dirtyCallback) {
        super(SLOT_COUNT);
        this.dirtyCallback = dirtyCallback;
        for (int slot = 0; slot < actorSnapshot.length; slot++) actorSnapshot[slot] = ItemStack.EMPTY;
    }

    @Override protected void onContentsChanged(int slot) {
        if (!synchronizing && attachedPlayer != null) pushSlotToActor(attachedPlayer, slot);
        dirtyCallback.run();
    }

    public synchronized void attachToActor(FakePlayer player) {
        if (player == null || attachedPlayer == player) return;
        if (attachedPlayer != null) captureActorChanges(attachedPlayer);
        attachedPlayer = player;

        if (isCanonicalEmpty() && actorHasMappedItems(player)) captureAllFromActor(player);
        else pushAllToActor(player);
        snapshotActor(player);
    }

    public synchronized void detachFromActor() {
        if (attachedPlayer != null) captureActorChanges(attachedPlayer);
        attachedPlayer = null;
        clearSnapshot();
    }

    /** Reconciles external fake-player actions into the canonical menu inventory once per server tick. */
    public synchronized void syncWithActor() {
        FakePlayer actor = onlineActor().orElse(attachedPlayer);
        if (actor == null) return;
        if (attachedPlayer != actor) attachToActor(actor);
        normalizeHiddenHotbar(actor);
        captureActorChanges(actor);
    }

    private boolean isCanonicalEmpty() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) if (!super.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    private static boolean actorHasMappedItems(FakePlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            if (!inventory.getItem(ACTOR_STORAGE_START + slot).isEmpty()) return true;
        }
        return !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
    }

    /** Moves pickup results out of the eight non-selected hotbar slots into the visible 27-slot store. */
    private static void normalizeHiddenHotbar(FakePlayer player) {
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (hotbar == selected) continue;
            ItemStack hidden = inventory.getItem(hotbar);
            if (hidden.isEmpty()) continue;
            int remaining = hidden.getCount();
            for (int slot = ACTOR_STORAGE_START; slot < ACTOR_STORAGE_START + STORAGE_SLOTS && remaining > 0; slot++) {
                ItemStack stored = inventory.getItem(slot);
                if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, hidden)) continue;
                int moved = Math.min(remaining, Math.max(0, stored.getMaxStackSize() - stored.getCount()));
                if (moved > 0) { stored.grow(moved); remaining -= moved; }
            }
            for (int slot = ACTOR_STORAGE_START; slot < ACTOR_STORAGE_START + STORAGE_SLOTS && remaining > 0; slot++) {
                if (!inventory.getItem(slot).isEmpty()) continue;
                int moved = Math.min(remaining, hidden.getMaxStackSize());
                inventory.setItem(slot, hidden.copyWithCount(moved));
                remaining -= moved;
            }
            if (remaining <= 0) inventory.setItem(hotbar, ItemStack.EMPTY);
            else hidden.setCount(remaining);
        }
        inventory.setChanged();
    }

    private void captureAllFromActor(FakePlayer player) {
        synchronizing = true;
        try {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                super.setStackInSlot(slot, actorStack(player, slot).copy());
            }
        } finally {
            synchronizing = false;
        }
        dirtyCallback.run();
    }

    private void captureActorChanges(FakePlayer player) {
        synchronizing = true;
        boolean changed = false;
        try {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack actorStack = actorStack(player, slot);
                if (!sameStack(actorStack, actorSnapshot[slot])) {
                    super.setStackInSlot(slot, actorStack.copy());
                    actorSnapshot[slot] = actorStack.copy();
                    changed = true;
                }
            }
        } finally {
            synchronizing = false;
        }
        if (changed) dirtyCallback.run();
    }

    private void pushAllToActor(FakePlayer player) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) pushSlotToActor(player, slot);
        player.getInventory().setChanged();
    }

    private void pushSlotToActor(FakePlayer player, int slot) {
        ItemStack value = super.getStackInSlot(slot).copy();
        if (slot < STORAGE_SLOTS) {
            player.getInventory().setItem(ACTOR_STORAGE_START + slot, value);
        } else if (slot == MAIN_HAND_SLOT) {
            player.getInventory().setItem(player.getInventory().selected, value);
        } else if (slot == OFF_HAND_SLOT) {
            player.setItemSlot(EquipmentSlot.OFFHAND, value);
        }
        actorSnapshot[slot] = value.copy();
        player.getInventory().setChanged();
    }

    private static ItemStack actorStack(FakePlayer player, int slot) {
        if (slot < STORAGE_SLOTS) return player.getInventory().getItem(ACTOR_STORAGE_START + slot);
        if (slot == MAIN_HAND_SLOT) return player.getMainHandItem();
        return player.getOffhandItem();
    }

    private void snapshotActor(FakePlayer player) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) actorSnapshot[slot] = actorStack(player, slot).copy();
    }

    private void clearSnapshot() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) actorSnapshot[slot] = ItemStack.EMPTY;
    }

    private Optional<FakePlayer> onlineActor() {
        return RikumiAiLifecycle.player();
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return left.isEmpty() && right.isEmpty();
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        syncWithActor();
        return super.serializeNBT(provider);
    }

    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        synchronizing = true;
        try {
            super.deserializeNBT(provider, nbt);
        } finally {
            synchronizing = false;
        }
        attachedPlayer = null;
        clearSnapshot();
    }
}
