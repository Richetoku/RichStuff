package com.richetoku.richstuff;

import com.richetoku.richcore.api.RichFluidItemHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Portable Rich Tank item retaining one registered FluidStack. */
public final class RichTankBlockItem extends BlockItem {
    private final int tier;
    public RichTankBlockItem(RichTankBlock block, Properties properties, int tier) {
        super(block, properties.stacksTo(1));
        this.tier = tier;
    }
    public int tier() { return tier; }
    public int capacity() { return RichStuffConfig.richTankCapacity(tier); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = RichFluidItemHandler.getFluid(stack);
        if (fluid.isEmpty()) tooltip.add(Component.translatable("tooltip.richstuff.rich_tank.empty", capacity()).withStyle(ChatFormatting.GRAY));
        else tooltip.add(Component.translatable("tooltip.richstuff.rich_tank.contents", fluid.getHoverName(), fluid.getAmount(), capacity()).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
