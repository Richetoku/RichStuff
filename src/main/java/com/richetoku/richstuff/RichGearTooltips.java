package com.richetoku.richstuff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class RichGearTooltips {
    private RichGearTooltips() {}
    public static void append(ItemStack stack, Consumer<Component> output) {
        String primary = RichGearData.primary(stack);
        RichGearProfile profile = RichGearData.combinedProfile(stack);
        if (RichGearData.isBroken(stack)) {
            output.accept(Component.translatable("tooltip.richstuff.rich_gear.broken").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        int level = RichGearData.level(stack);
        int required = level >= RichGearData.maxLevel() ? 0 : RichGearData.required(level);
        output.accept(Component.translatable("tooltip.richstuff.rich_gear.level", level, RichGearData.experience(stack), required).withStyle(ChatFormatting.GOLD));
        output.accept(Component.translatable("tooltip.richstuff.rich_gear.durability", RichGearData.durabilityRemaining(stack), RichGearData.maxDurability(stack)).withStyle(RichGearData.isBroken(stack) ? ChatFormatting.RED : ChatFormatting.GRAY));
        output.accept(Component.translatable("tooltip.richstuff.rich_gear.material", primary.isBlank() ? "unbound" : primary).withStyle(ChatFormatting.AQUA));
        output.accept(Component.translatable("tooltip.richstuff.rich_gear.slots", RichGearData.modifiers(stack).size(), RichGearData.slots(stack)).withStyle(ChatFormatting.GRAY));
        output.accept(Component.translatable("tooltip.richstuff.rich_gear.stats", String.format("%.2f", profile.miningSpeed()), String.format("%.2f", profile.attackBonus()), String.format("%.1f%%", profile.protection() * 100), String.format("%.2f", profile.durability())).withStyle(ChatFormatting.DARK_GREEN));
        if (!primary.isBlank()) {
            output.accept(profile.familyName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            output.accept(profile.signatureName().copy().withStyle(ChatFormatting.DARK_PURPLE));
        }
        for (String material : RichGearData.modifiers(stack)) output.accept(Component.translatable("tooltip.richstuff.rich_gear.modifier", material).withStyle(ChatFormatting.BLUE));
    }
}
