package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Mold, cooling timer, molten amount and cast output for a casting table or basin. */
public final class RichCastingBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private ResourceLocation fluid; private int amount; private int progress; private int target;
    public RichCastingBlockEntity(BlockPos pos,BlockState state){ super(RichStuff.CASTING_ENTITY.get(),pos,state); }
    public boolean basin(){ return getBlockState().getBlock() instanceof RichCastingBlock block && block.basin(); }
    public boolean insertMold(ItemStack stack){ if(basin()||!items.get(0).isEmpty()||stack.isEmpty()||!BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("mold")) return false; items.set(0,stack.copyWithCount(1)); sync(); return true; }
    public void beginPour(RichFoundryBlockEntity foundry){
        if(!items.get(1).isEmpty()||fluid!=null||foundry.selectedFluid()==null) return;
        RichFoundryRecipes.Casting recipe=RichFoundryRecipes.findCasting(foundry.selectedFluid(),items.get(0),basin());
        if(recipe==null||foundry.selectedAmount()<recipe.amount()) return;
        if(foundry.drainSelected(recipe.amount())!=recipe.amount()) return;
        fluid=recipe.fluid(); amount=recipe.amount(); target=recipe.ticks(); progress=0; sync();
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,RichCastingBlockEntity casting){
        if(casting.fluid==null||!casting.items.get(1).isEmpty()) return;
        if(++casting.progress<casting.target) return;
        RichFoundryRecipes.Casting recipe=RichFoundryRecipes.findCasting(casting.fluid,casting.items.get(0),casting.basin());
        if(recipe!=null){ ItemStack output=new ItemStack(BuiltInRegistries.ITEM.get(recipe.result()),recipe.count()); if(!output.isEmpty()) casting.items.set(1,output); }
        casting.fluid=null; casting.amount=0; casting.progress=0; casting.target=0; casting.sync();
    }
    public boolean takeOutput(Player player){ if(items.get(1).isEmpty()) return false; if(!level.isClientSide){ if(!player.addItem(items.get(1).copy())) player.drop(items.get(1).copy(),false); items.set(1,ItemStack.EMPTY); sync(); } return true; }
    private void sync(){ setChanged(); if(level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){ super.saveAdditional(tag,provider); ContainerHelper.saveAllItems(tag,items,provider); if(fluid!=null) tag.putString("Fluid",fluid.toString()); tag.putInt("Amount",amount); tag.putInt("Progress",progress); tag.putInt("Target",target); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){ super.loadAdditional(tag,provider); ContainerHelper.loadAllItems(tag,items,provider); fluid=tag.contains("Fluid")?ResourceLocation.parse(tag.getString("Fluid")):null; amount=tag.getInt("Amount"); progress=tag.getInt("Progress"); target=tag.getInt("Target"); }
}
