package com.richetoku.richstuff;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Shared client-specific visualization key for every Rich module. */
public final class RichVisualizerItem extends Item {
    public RichVisualizerItem(Properties properties) { super(properties.stacksTo(1)); }
    public static boolean isHeld(Player player) {
        return player != null && (player.getMainHandItem().getItem() instanceof RichVisualizerItem
                || player.getOffhandItem().getItem() instanceof RichVisualizerItem);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.richstuff.rich_visualizer"));
    }
}
