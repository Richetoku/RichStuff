package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Read-only, packet-synchronized status menu for a complete Rich Tank multiblock. */
public final class RichTankMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 118;
    public static final int DATA_COUNT = 8;

    @Nullable private final RichTankBlockEntity tank;
    private final ContainerData data;
    private final BlockPos pos;

    /** Client constructor deliberately does not resolve a live world block entity. */
    public RichTankMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(RichStuff.RICH_TANK_MENU.get(), id);
        this.pos = buffer.readBlockPos();
        this.tank = null;
        this.data = new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
    }

    public RichTankMenu(int id, Inventory inventory, RichTankBlockEntity tank) {
        super(RichStuff.RICH_TANK_MENU.get(), id);
        this.tank = tank;
        this.pos = tank.getBlockPos();
        this.data = tank.dataAccess();
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    public int tier() { return data.get(0); }
    public int amount() { return Math.max(0, data.get(1)); }
    public int capacity() { return Math.max(1, data.get(2)); }
    public int width() { return Math.max(1, data.get(3)); }
    public int height() { return Math.max(1, data.get(4)); }
    public int depth() { return Math.max(1, data.get(5)); }
    public int tankCount() { return Math.max(1, data.get(6)); }
    public int fluidRegistryId() { return data.get(7) - 1; }
    public BlockPos blockPos() { return pos; }

    @Override public boolean stillValid(Player player) {
        return tank == null || tank.stillValid(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
