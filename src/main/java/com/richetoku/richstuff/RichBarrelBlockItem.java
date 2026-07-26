package com.richetoku.richstuff;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class RichBarrelBlockItem extends BlockItem {
    private final int tier;
    public RichBarrelBlockItem(Block block,Properties properties,int tier){super(block,properties.stacksTo(1));this.tier=tier;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.richstuff.rich_barrel.slots",54));
        tooltip.add(Component.translatable("tooltip.richstuff.rich_barrel.slot_limit",64 << Math.max(0,tier-1)));
        super.appendHoverText(stack,context,tooltip,flag);
    }
}
