package com.richetoku.richstuff;

import com.richetoku.richcore.api.RichFluidItemHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** One universal one-bucket jar. The contained fluid determines its displayed name and tooltip. */
public final class RichJarItem extends Item {
    public static final int CAPACITY = 1000;

    public RichJarItem(Properties properties) {
        super(properties.stacksTo(8));
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = RichFluidItemHandler.getFluid(stack);
        return fluid.isEmpty()
                ? Component.translatable("item.richstuff.empty_jar")
                : Component.translatable("item.richstuff.vessel.filled_jar", fluid.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = RichFluidItemHandler.getFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.richstuff.fluid.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.richstuff.fluid.amount", fluid.getHoverName(),
                    fluid.getAmount(), CAPACITY).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
