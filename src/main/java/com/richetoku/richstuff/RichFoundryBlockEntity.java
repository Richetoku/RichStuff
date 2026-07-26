package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Full hollow multiblock Foundry inventory and shared multi-fluid storage controller. */
public final class RichFoundryBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, IFluidHandler {
    public static final int INPUTS = 9;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INPUTS, ItemStack.EMPTY);
    private final Map<ResourceLocation, Integer> tank = new LinkedHashMap<>();
    private RichFoundryStructure.Result structure = RichFoundryStructure.Result.INVALID;
    private final Set<BlockPos> linkedTanks = new LinkedHashSet<>();
    private int progress;
    private int alloyProgress;
    private int validationCooldown;
    @Nullable private ResourceLocation selected;
    private boolean formed;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> formed ? 1 : 0;
                case 1 -> alloying() ? 1 : 0;
                case 2 -> progress;
                case 3 -> alloyProgress;
                case 4 -> totalFluidAmount();
                case 5 -> baseCapacity();
                case 6 -> tankAddedCapacity();
                case 7 -> totalCapacity();
                case 8 -> tank.size();
                case 9 -> selectedAmount();
                case 10 -> structure.width();
                case 11 -> structure.depth();
                case 12 -> structure.height();
                case 13 -> structure.drains();
                case 14 -> structure.tanks().size();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 15; }
    };

    public RichFoundryBlockEntity(BlockPos pos, BlockState state) { super(RichStuff.FOUNDRY_CONTROLLER_ENTITY.get(), pos, state); }
    public boolean alloying() { return getBlockState().getBlock() instanceof RichFoundryBlock block && block.alloying(); }
    public boolean formed() { return formed; }
    public Map<ResourceLocation,Integer> fluids() { return Map.copyOf(tank); }
    public ResourceLocation selectedFluid() { return selected; }
    public int selectedAmount() { return selected == null ? 0 : tank.getOrDefault(selected, 0); }
    public int baseCapacity() { return RichStuffConfig.FOUNDRY_BASE_CAPACITY.get(); }
    public int tankAddedCapacity() {
        if (level == null) return 0;
        long total = 0;
        Set<BlockPos> candidates = new LinkedHashSet<>(linkedTanks);
        candidates.addAll(structure.tanks());
        for (BlockPos pos : candidates) {
            if (level.getBlockEntity(pos) instanceof RichTankBlockEntity richTank) total += richTank.capacity();
        }
        return (int)Math.min(Integer.MAX_VALUE, total);
    }
    public int totalCapacity() { return (int)Math.min(Integer.MAX_VALUE, (long)baseCapacity() + tankAddedCapacity()); }
    public int totalFluidAmount() { long total=0; for(int amount:tank.values()) total+=amount; return (int)Math.min(Integer.MAX_VALUE,total); }
    public boolean structureContains(BlockPos pos) { return structure.contains(pos); }
    public boolean isTankLinked(BlockPos pos) { return pos != null && (linkedTanks.contains(pos) || structure.tanks().contains(pos)); }
    public boolean canRemoveTank(BlockPos pos, int capacity) {
        if (!isTankLinked(pos)) return true;
        long reducedCapacity = Math.max(baseCapacity(), (long) totalCapacity() - Math.max(0, capacity));
        return totalFluidAmount() <= reducedCapacity;
    }
    public ContainerData dataAccess() { return data; }

    public FluidStack selectedFluidStack() {
        if (selected == null) return FluidStack.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.get(selected);
        return fluid == null || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, selectedAmount());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RichFoundryBlockEntity foundry) {
        if (--foundry.validationCooldown <= 0) {
            foundry.validationCooldown = 20;
            foundry.validateStructure();
        }
        if (!foundry.formed) { foundry.progress = 0; foundry.alloyProgress = 0; return; }
        foundry.tickMelting();
        if (foundry.alloying()) foundry.tickAlloying();
    }

    private void validateStructure() {
        if (level == null) return;
        Direction facing = getBlockState().getValue(RichFoundryBlock.FACING);
        RichFoundryStructure.Result next = RichFoundryStructure.find(level, worldPosition, facing);
        boolean complete = next.formed();

        if (complete) {
            // Commit the candidate shape before absorption so its tank capacity is available while moving
            // standalone tank contents into the shared Foundry storage.
            structure = next;
            for (BlockPos tankPos : next.tanks()) {
                if (!(level.getBlockEntity(tankPos) instanceof RichTankBlockEntity richTank) || !richTank.absorbInto(this)) {
                    complete = false;
                    break;
                }
                linkedTanks.add(tankPos.immutable());
            }
            if (complete && totalFluidAmount() > totalCapacity()) complete = false;
        }

        if (complete) {
            unlinkRemovedTanks(next.tanks());
            linkedTanks.addAll(next.tanks());
        } else {
            structure = RichFoundryStructure.Result.INVALID;
            // Once fluid relies on expansion tanks, keep surviving tank links while the shell is being
            // repaired. This prevents breaking a tank after first invalidating an unrelated casing.
            if (totalFluidAmount() <= baseCapacity()) unlinkRemovedTanks(Set.of());
            else pruneMissingTankLinks();
        }

        if (formed != complete) {
            formed = complete;
            BlockState state = getBlockState();
            level.setBlock(worldPosition, state.setValue(RichFoundryBlock.FORMED, complete), Block.UPDATE_ALL);
        }
        sync();
    }

    private void unlinkRemovedTanks(Set<BlockPos> retained) {
        if (level == null) return;
        for (BlockPos old : List.copyOf(linkedTanks)) {
            if (retained.contains(old)) continue;
            if (level.getBlockEntity(old) instanceof RichTankBlockEntity tankEntity) tankEntity.setFoundryLink(null);
            linkedTanks.remove(old);
        }
    }

    private void pruneMissingTankLinks() {
        if (level == null) return;
        linkedTanks.removeIf(pos -> !(level.getBlockEntity(pos) instanceof RichTankBlockEntity));
    }

    private void tickMelting() {
        int slot = firstMeltableSlot();
        if (slot < 0) { progress = 0; return; }
        RichFoundryRecipes.Melting recipe = RichFoundryRecipes.findMelting(items.get(slot));
        if (recipe == null || totalFluidAmount() + recipe.amount() > totalCapacity()) { progress = 0; return; }
        if (++progress < recipe.ticks()) return;
        items.get(slot).shrink(1);
        if (items.get(slot).isEmpty()) items.set(slot, ItemStack.EMPTY);
        tank.merge(recipe.fluid(), recipe.amount(), Integer::sum);
        if (selected == null) selected = recipe.fluid();
        progress = 0;
        sync();
    }

    private void tickAlloying() {
        RichFoundryRecipes.Alloy recipe = RichFoundryRecipes.findAlloy(tank);
        if (recipe == null) { alloyProgress = 0; return; }
        if (++alloyProgress < recipe.ticks()) return;
        long consumed = 0;
        for (RichFoundryRecipes.FluidAmount input : recipe.inputs()) consumed += input.amount();
        if ((long)totalFluidAmount() - consumed + recipe.amount() > totalCapacity()) { alloyProgress = 0; return; }
        for (RichFoundryRecipes.FluidAmount input : recipe.inputs()) removeFluid(input.fluid(), input.amount());
        tank.merge(recipe.fluid(), recipe.amount(), Integer::sum);
        selected = recipe.fluid();
        alloyProgress = 0;
        sync();
    }

    private int firstMeltableSlot() {
        for (int i=0;i<items.size();i++) if (!items.get(i).isEmpty() && RichFoundryRecipes.findMelting(items.get(i)) != null) return i;
        return -1;
    }

    public boolean tryInsert(ItemStack offered) {
        if (offered.isEmpty() || RichFoundryRecipes.findMelting(offered) == null) return false;
        for (int i=0;i<items.size();i++) {
            if (items.get(i).isEmpty()) { items.set(i, offered.copyWithCount(1)); sync(); return true; }
            if (ItemStack.isSameItemSameComponents(items.get(i), offered) && items.get(i).getCount() < items.get(i).getMaxStackSize()) {
                items.get(i).grow(1); sync(); return true;
            }
        }
        return false;
    }

    public int drainSelected(int amount) {
        if (selected == null || amount <= 0) return 0;
        int drained = Math.min(amount, tank.getOrDefault(selected, 0));
        if (drained > 0) { removeFluid(selected, drained); sync(); }
        return drained;
    }

    private void removeFluid(ResourceLocation fluid, int amount) {
        int left = tank.getOrDefault(fluid, 0) - amount;
        if (left <= 0) tank.remove(fluid); else tank.put(fluid, left);
        if (selected != null && !tank.containsKey(selected)) selectFirst();
    }
    private void selectFirst() { selected = tank.keySet().stream().findFirst().orElse(null); }
    public void cycleSelectedFluid() {
        if (tank.isEmpty()) { selected = null; sync(); return; }
        List<ResourceLocation> fluids = new ArrayList<>(tank.keySet());
        int index = selected == null ? -1 : fluids.indexOf(selected);
        selected = fluids.get((index + 1) % fluids.size());
        sync();
    }

    public Component statusMessage() {
        if (!formed) return Component.translatable("message.richstuff.foundry.incomplete");
        FluidStack fluid = selectedFluidStack();
        if (fluid.isEmpty()) return Component.translatable(alloying() ? "message.richstuff.alloy_foundry.empty" : "message.richstuff.foundry.empty");
        return Component.translatable(alloying() ? "message.richstuff.alloy_foundry.status" : "message.richstuff.foundry.status",
                fluid.getHoverName(), selectedAmount(), tank.size());
    }

    @Override public Component getDisplayName() { return Component.translatable(alloying() ? "container.richstuff.alloy_foundry" : "container.richstuff.foundry"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new RichFoundryMenu(id, inventory, this); }

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int index) { return index == 0 ? selectedFluidStack() : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int index) { return index == 0 ? totalCapacity() : 0; }
    @Override public boolean isFluidValid(int index, FluidStack stack) { return index == 0 && stack != null && !stack.isEmpty(); }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(resource.getFluid());
        if (id == null) return 0;
        int accepted = Math.min(Math.max(0, totalCapacity() - totalFluidAmount()), resource.getAmount());
        if (accepted > 0 && action.execute()) {
            tank.merge(id, accepted, Integer::sum);
            if (selected == null) selected = id;
            sync();
        }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
        FluidStack selectedStack = selectedFluidStack();
        if (selectedStack.isEmpty() || !FluidStack.isSameFluidSameComponents(selectedStack, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack selectedStack = selectedFluidStack();
        if (maxDrain <= 0 || selectedStack.isEmpty()) return FluidStack.EMPTY;
        int amount = Math.min(maxDrain, selectedStack.getAmount());
        FluidStack result = selectedStack.copyWithAmount(amount);
        if (action.execute()) { removeFluid(selected, amount); sync(); }
        return result;
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation,Integer> entry : tank.entrySet()) {
            CompoundTag value = new CompoundTag(); value.putString("Fluid", entry.getKey().toString()); value.putInt("Amount", entry.getValue()); list.add(value);
        }
        tag.put("Fluids", list); tag.putInt("Progress", progress); tag.putInt("AlloyProgress", alloyProgress); tag.putBoolean("Formed", formed);
        tag.putLongArray("LinkedTanks", linkedTanks.stream().mapToLong(BlockPos::asLong).toArray());
        if (selected != null) tag.putString("Selected", selected.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        tank.clear(); ListTag list=tag.getList("Fluids", net.minecraft.nbt.Tag.TAG_COMPOUND); for(int i=0;i<list.size();i++){ CompoundTag value=list.getCompound(i); ResourceLocation id=ResourceLocation.tryParse(value.getString("Fluid")); if(id!=null&&value.getInt("Amount")>0)tank.put(id,value.getInt("Amount")); }
        progress=tag.getInt("Progress"); alloyProgress=tag.getInt("AlloyProgress"); formed=tag.getBoolean("Formed"); selected=tag.contains("Selected")?ResourceLocation.tryParse(tag.getString("Selected")):null;
        linkedTanks.clear();
        for (long packed : tag.getLongArray("LinkedTanks")) linkedTanks.add(BlockPos.of(packed));
        validationCooldown=1;
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    private static final int[] SLOTS = {0,1,2,3,4,5,6,7,8};
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) { return RichFoundryRecipes.findMelting(stack) != null; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return false; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack value=ContainerHelper.removeItem(items,slot,amount); if(!value.isEmpty()) sync(); return value; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot,stack); stack.limitSize(getMaxStackSize(stack)); sync(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0; }
    @Override public void clearContent() { for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY); sync(); }
}
