package com.richetoku.richstuff;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Bucket-like capability that swaps empty and named Jar item identities instead of attaching NBT. */
public final class RichJarFluidHandler implements IFluidHandlerItem {
    private ItemStack container;
    public RichJarFluidHandler(ItemStack stack) { this.container=stack.copy(); }
    private RichJarItem jar() { return container.getItem() instanceof RichJarItem value ? value : null; }
    private Fluid fluid() { RichJarItem item=jar(); return item==null?Fluids.EMPTY:item.fluid(); }
    @Override public ItemStack getContainer(){ return container; }
    @Override public int getTanks(){ return 1; }
    @Override public FluidStack getFluidInTank(int tank){ Fluid value=fluid(); return tank==0&&value!=Fluids.EMPTY?new FluidStack(value,RichJarItem.CAPACITY):FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank){ return tank==0?RichJarItem.CAPACITY:0; }
    @Override public boolean isFluidValid(int tank,FluidStack stack){ return tank==0&&jar()!=null&&jar().isEmptyJar()&&stack!=null&&!stack.isEmpty()&&!RichStuff.jarForFluid(stack.getFluid()).isEmpty(); }
    @Override public int fill(FluidStack resource,IFluidHandler.FluidAction action){
        RichJarItem item=jar();
        if(container.getCount()!=1||item==null||!item.isEmptyJar()||resource==null||resource.getAmount()<RichJarItem.CAPACITY) return 0;
        ItemStack named=RichStuff.jarForFluid(resource.getFluid()); if(named.isEmpty()) return 0;
        if(action.execute()) container=named.copyWithCount(1);
        return RichJarItem.CAPACITY;
    }
    @Override public FluidStack drain(FluidStack resource,IFluidHandler.FluidAction action){
        Fluid value=fluid(); if(container.getCount()!=1||value==Fluids.EMPTY||resource==null||resource.getAmount()<RichJarItem.CAPACITY||!FluidStack.isSameFluidSameComponents(new FluidStack(value,RichJarItem.CAPACITY),resource)) return FluidStack.EMPTY;
        return drain(RichJarItem.CAPACITY,action);
    }
    @Override public FluidStack drain(int maxDrain,IFluidHandler.FluidAction action){
        Fluid value=fluid(); if(container.getCount()!=1||value==Fluids.EMPTY||maxDrain<RichJarItem.CAPACITY) return FluidStack.EMPTY;
        FluidStack result=new FluidStack(value,RichJarItem.CAPACITY); if(action.execute()) container=RichStuff.emptyJar(); return result;
    }
}
