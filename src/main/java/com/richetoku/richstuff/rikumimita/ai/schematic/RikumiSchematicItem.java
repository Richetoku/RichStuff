package com.richetoku.richstuff.rikumimita.ai.schematic;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Portable schematic reference used by the reusable Rikumi placement marker. */
public final class RikumiSchematicItem extends Item {
    public static final ResourceLocation STARTER_HOUSE = ResourceLocation.fromNamespaceAndPath("richstuff", "starter_house");
    private static final String NBT_SCHEMATIC = "RikumiSchematicId";

    public RikumiSchematicItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static ItemStack create(Item item, ResourceLocation schematicId) {
        ItemStack stack = new ItemStack(item);
        setSchematicId(stack, schematicId);
        return stack;
    }

    public static void setSchematicId(ItemStack stack, ResourceLocation schematicId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(NBT_SCHEMATIC, (schematicId == null ? STARTER_HOUSE : schematicId).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ResourceLocation getSchematicId(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation parsed = tag.contains(NBT_SCHEMATIC) ? ResourceLocation.tryParse(tag.getString(NBT_SCHEMATIC)) : null;
        return parsed == null ? STARTER_HOUSE : parsed;
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getSchematicId(stack);
        String name = RikumiSchematicRegistry.get(id).map(RikumiSchematic::displayName).orElse(id.toString());
        return Component.translatable("item.richstuff.rikumi_schematic.named", name);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getSchematicId(stack);
        tooltip.add(Component.translatable("tooltip.richstuff.rikumi_schematic.source", id.toString()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.richstuff.rikumi_schematic.marker").withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
