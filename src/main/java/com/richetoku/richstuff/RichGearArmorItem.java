package com.richetoku.richstuff;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;

import java.util.List;

/** Native modular armor body shared by all RichStuff material combinations. */
public final class RichGearArmorItem extends ArmorItem implements RichGearMarker {
    public RichGearArmorItem(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties.durability(type.getDurability(24)));
    }
    @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
    @Override public boolean isBarVisible(ItemStack stack) { return RichGearData.damage(stack) > 0; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13.0F * RichGearData.durabilityRemaining(stack) / Math.max(1.0F, RichGearData.maxDurability(stack))); }
    @Override public int getBarColor(ItemStack stack) {
        if (RichGearData.isBroken(stack)) return 0xD01818;
        float ratio = RichGearData.durabilityRemaining(stack) / Math.max(1.0F, RichGearData.maxDurability(stack));
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> output, TooltipFlag flag) {
        RichGearTooltips.append(stack, output::add);
        super.appendHoverText(stack, context, output, flag);
    }
}
