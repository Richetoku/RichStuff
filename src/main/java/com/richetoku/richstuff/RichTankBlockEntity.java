package com.richetoku.richstuff;

import com.richetoku.richcore.api.RichFluidItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Standalone multiblock or Foundry-linked storage for one Rich Tank block. */
public final class RichTankBlockEntity extends BlockEntity implements IFluidHandler, MenuProvider {
    private FluidStack fluid = FluidStack.EMPTY;
    @Nullable private BlockPos foundryController;

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            RichTankNetwork.View network = RichTankNetwork.resolve(RichTankBlockEntity.this);
            FluidStack visible = getFluidInTank(0);
            return switch (index) {
                case 0 -> tier();
                case 1 -> visible.getAmount();
                case 2 -> getTankCapacity(0);
                case 3 -> network.width();
                case 4 -> network.height();
                case 5 -> network.depth();
                case 6 -> network.members().size();
                case 7 -> visible.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(visible.getFluid()) + 1;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return RichTankMenu.DATA_COUNT; }
    };

    public RichTankBlockEntity(BlockPos pos, BlockState state) {
        super(RichStuff.RICH_TANK_ENTITY.get(), pos, state);
    }

    public int tier() { return getBlockState().getBlock() instanceof RichTankBlock tank ? tank.tier() : 1; }
    public int capacity() { return RichStuffConfig.richTankCapacity(tier()); }
    public boolean linked() { return linkedFoundry() != null; }
    @Nullable public BlockPos foundryController() { return foundryController; }
    ContainerData dataAccess() { return menuData; }

    @Override public Component getDisplayName() {
        return Component.translatable("container.richstuff.rich_tank", tier());
    }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RichTankMenu(id, inventory, this);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    @Nullable private RichFoundryBlockEntity linkedFoundry() {
        RichFoundryBlockEntity foundry = linkedFoundryController();
        return foundry != null && foundry.formed() && foundry.structureContains(worldPosition) ? foundry : null;
    }

    /** Resolve the stored link even while a Foundry shell is temporarily incomplete. */
    @Nullable private RichFoundryBlockEntity linkedFoundryController() {
        if (level == null || foundryController == null) return null;
        if (level.getBlockEntity(foundryController) instanceof RichFoundryBlockEntity foundry
                && foundry.isTankLinked(worldPosition)) return foundry;
        return null;
    }

    FluidStack localFluid() { return fluid.copy(); }

    /** Updates local storage without emitting a client packet when nothing changed. */
    boolean setLocalFluidIfChanged(FluidStack value) {
        FluidStack next = value == null ? FluidStack.EMPTY : value.copy();
        if (fluid.getAmount() == next.getAmount()
                && (fluid.isEmpty() && next.isEmpty()
                || !fluid.isEmpty() && !next.isEmpty() && FluidStack.isSameFluidSameComponents(fluid, next))) return false;
        fluid = next;
        setChanged();
        return true;
    }

    void syncLocal() { sync(); }
    public int networkWidth() { return RichTankNetwork.resolve(this).width(); }
    public int networkDepth() { return RichTankNetwork.resolve(this).depth(); }
    public int networkHeight() { return RichTankNetwork.resolve(this).height(); }

    public FluidStack visualFluid() {
        RichFoundryBlockEntity foundry = linkedFoundry();
        return foundry == null ? RichTankNetwork.resolve(this).fluid() : foundry.selectedFluidStack();
    }

    public int visualAmount() {
        RichFoundryBlockEntity foundry = linkedFoundry();
        if (foundry != null) {
            int total = Math.max(1, foundry.totalCapacity());
            return Math.round(capacity() * Math.min(1.0F, (float) foundry.totalFluidAmount() / total));
        }
        return RichTankNetwork.resolve(this).visualAmount(this);
    }

    public void setFoundryLink(@Nullable BlockPos controller) {
        if (controller != null && controller.equals(foundryController)) return;
        foundryController = controller == null ? null : controller.immutable();
        if (level != null) RichTankNetwork.invalidate(level);
        sync();
    }

    /** Transfers standalone contents into a newly formed Foundry without deleting fluid. */
    public boolean absorbInto(RichFoundryBlockEntity foundry) {
        if (fluid.isEmpty()) { setFoundryLink(foundry.getBlockPos()); return true; }
        int accepted = foundry.fill(fluid, FluidAction.SIMULATE);
        if (accepted < fluid.getAmount()) return false;
        foundry.fill(fluid, FluidAction.EXECUTE);
        fluid = FluidStack.EMPTY;
        setFoundryLink(foundry.getBlockPos());
        return true;
    }

    /** Redistributes shared fluid before this member is removed so no contents are lost. */
    public void prepareForRemoval() {
        RichFoundryBlockEntity foundry = linkedFoundryController();
        if (foundry != null) return;
        RichTankNetwork.View network = RichTankNetwork.resolve(this);
        if (network.members().size() > 1) network.redistributeWithout(this);
    }

    public boolean canSafelyRemove() {
        RichFoundryBlockEntity foundry = linkedFoundryController();
        if (foundry != null) return foundry.canRemoveTank(worldPosition, capacity());
        RichTankNetwork.View network = RichTankNetwork.resolve(this);
        return network.members().size() <= 1 || network.amount() <= network.capacity() - capacity();
    }

    public void loadFromItem(ItemStack stack) {
        FluidStack stored = RichFluidItemHandler.getFluid(stack);
        fluid = stored.isEmpty() ? FluidStack.EMPTY : stored.copyWithAmount(Math.min(capacity(), stored.getAmount()));
        if (level != null) RichTankNetwork.invalidate(level);
        sync();
    }

    public void saveToItem(ItemStack stack) {
        if (RichStuffConfig.RICH_TANKS_RETAIN_FLUID.get() && !fluid.isEmpty()) {
            RichFluidItemHandler.setFluid(stack, fluid.copy());
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            RichFluidItemHandler.setFluid(stack, FluidStack.EMPTY);
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
    }

    @Override public int getTanks() { return 1; }

    @Override public FluidStack getFluidInTank(int tank) {
        if (tank != 0) return FluidStack.EMPTY;
        RichFoundryBlockEntity foundry = linkedFoundry();
        return foundry == null ? RichTankNetwork.resolve(this).fluid() : foundry.getFluidInTank(0);
    }

    @Override public int getTankCapacity(int tank) {
        if (tank != 0) return 0;
        RichFoundryBlockEntity foundry = linkedFoundry();
        return foundry == null ? RichTankNetwork.resolve(this).capacity() : foundry.getTankCapacity(0);
    }

    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && stack != null && !stack.isEmpty();
    }

    @Override public int fill(FluidStack resource, FluidAction action) {
        RichFoundryBlockEntity foundry = linkedFoundry();
        if (foundry != null) return foundry.fill(resource, action);
        if (resource == null || resource.isEmpty()) return 0;
        RichTankNetwork.View network = RichTankNetwork.resolve(this);
        if (!network.compatible(resource)) return 0;
        int accepted = Math.min(Math.max(0, network.capacity() - network.amount()), resource.getAmount());
        if (accepted > 0 && action.execute()) network.distribute(resource, network.amount() + accepted);
        return accepted;
    }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        RichFoundryBlockEntity foundry = linkedFoundry();
        if (foundry != null) return foundry.drain(resource, action);
        RichTankNetwork.View network = RichTankNetwork.resolve(this);
        FluidStack current = network.fluid();
        if (resource == null || resource.isEmpty() || current.isEmpty()
                || !FluidStack.isSameFluidSameComponents(current, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        RichFoundryBlockEntity foundry = linkedFoundry();
        if (foundry != null) return foundry.drain(maxDrain, action);
        if (maxDrain <= 0) return FluidStack.EMPTY;
        RichTankNetwork.View network = RichTankNetwork.resolve(this);
        FluidStack current = network.fluid();
        if (current.isEmpty()) return FluidStack.EMPTY;
        int amount = Math.min(maxDrain, network.amount());
        FluidStack out = current.copyWithAmount(amount);
        if (action.execute()) network.distribute(current, network.amount() - amount);
        return out;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.save(registries));
        if (foundryController != null) tag.putLong("Foundry", foundryController.asLong());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
        foundryController = tag.contains("Foundry") ? BlockPos.of(tag.getLong("Foundry")) : null;
        if (level != null) RichTankNetwork.invalidate(level);
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
