package com.richetoku.richstuff;

import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Optional bridge for packs that want vanilla tool/armor recipes to hand the player a Silent Gear equivalent.
 * The config is off by default. Silent Gear itself supports vanilla conversion recipes, but this lets RichStuff
 * mark conversions with the source material so pack scripts can preserve RichStuff/Silent Gear grading workflows.
 */
public final class RichStuffSilentGearEvents {
    private RichStuffSilentGearEvents() {}

    private static final Map<Item, String> VANILLA_TO_SILENT = Map.ofEntries(
        Map.entry(Items.WOODEN_PICKAXE, "silentgear:pickaxe"), Map.entry(Items.STONE_PICKAXE, "silentgear:pickaxe"), Map.entry(Items.IRON_PICKAXE, "silentgear:pickaxe"), Map.entry(Items.GOLDEN_PICKAXE, "silentgear:pickaxe"), Map.entry(Items.DIAMOND_PICKAXE, "silentgear:pickaxe"), Map.entry(Items.NETHERITE_PICKAXE, "silentgear:pickaxe"),
        Map.entry(Items.WOODEN_AXE, "silentgear:axe"), Map.entry(Items.STONE_AXE, "silentgear:axe"), Map.entry(Items.IRON_AXE, "silentgear:axe"), Map.entry(Items.GOLDEN_AXE, "silentgear:axe"), Map.entry(Items.DIAMOND_AXE, "silentgear:axe"), Map.entry(Items.NETHERITE_AXE, "silentgear:axe"),
        Map.entry(Items.WOODEN_SHOVEL, "silentgear:shovel"), Map.entry(Items.STONE_SHOVEL, "silentgear:shovel"), Map.entry(Items.IRON_SHOVEL, "silentgear:shovel"), Map.entry(Items.GOLDEN_SHOVEL, "silentgear:shovel"), Map.entry(Items.DIAMOND_SHOVEL, "silentgear:shovel"), Map.entry(Items.NETHERITE_SHOVEL, "silentgear:shovel"),
        Map.entry(Items.WOODEN_SWORD, "silentgear:sword"), Map.entry(Items.STONE_SWORD, "silentgear:sword"), Map.entry(Items.IRON_SWORD, "silentgear:sword"), Map.entry(Items.GOLDEN_SWORD, "silentgear:sword"), Map.entry(Items.DIAMOND_SWORD, "silentgear:sword"), Map.entry(Items.NETHERITE_SWORD, "silentgear:sword"),
        Map.entry(Items.WOODEN_HOE, "silentgear:hoe"), Map.entry(Items.STONE_HOE, "silentgear:hoe"), Map.entry(Items.IRON_HOE, "silentgear:hoe"), Map.entry(Items.GOLDEN_HOE, "silentgear:hoe"), Map.entry(Items.DIAMOND_HOE, "silentgear:hoe"), Map.entry(Items.NETHERITE_HOE, "silentgear:hoe"),
        Map.entry(Items.IRON_HELMET, "silentgear:helmet"), Map.entry(Items.GOLDEN_HELMET, "silentgear:helmet"), Map.entry(Items.DIAMOND_HELMET, "silentgear:helmet"), Map.entry(Items.NETHERITE_HELMET, "silentgear:helmet"),
        Map.entry(Items.IRON_CHESTPLATE, "silentgear:chestplate"), Map.entry(Items.GOLDEN_CHESTPLATE, "silentgear:chestplate"), Map.entry(Items.DIAMOND_CHESTPLATE, "silentgear:chestplate"), Map.entry(Items.NETHERITE_CHESTPLATE, "silentgear:chestplate"),
        Map.entry(Items.IRON_LEGGINGS, "silentgear:leggings"), Map.entry(Items.GOLDEN_LEGGINGS, "silentgear:leggings"), Map.entry(Items.DIAMOND_LEGGINGS, "silentgear:leggings"), Map.entry(Items.NETHERITE_LEGGINGS, "silentgear:leggings"),
        Map.entry(Items.IRON_BOOTS, "silentgear:boots"), Map.entry(Items.GOLDEN_BOOTS, "silentgear:boots"), Map.entry(Items.DIAMOND_BOOTS, "silentgear:boots"), Map.entry(Items.NETHERITE_BOOTS, "silentgear:boots")
    );

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!RichStuffConfig.CONVERT_VANILLA_TOOL_RECIPES_TO_SILENT_GEAR.get()) return;
        if (!ModList.get().isLoaded("silentgear")) return;
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;
        String silentId = VANILLA_TO_SILENT.get(crafted.getItem());
        if (silentId == null) return;
        Item target = BuiltInRegistries.ITEM.get(ResourceLocation.parse(silentId));
        if (target == Items.AIR) return;

        ItemStack replacement = new ItemStack(target, crafted.getCount());
        CompoundTag tag = new CompoundTag();
        tag.putString("RichStuffConvertedFrom", BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString());
        tag.putBoolean("RichStuffVanillaRecipeConversion", true);
        replacement.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        crafted.setCount(0);
        Player player = event.getEntity();
        if (!player.addItem(replacement)) {
            player.drop(replacement, false);
        }
    }
}
