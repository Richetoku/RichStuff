package com.richetoku.richstuff;

import net.neoforged.fml.ModList;

public final class ModDetection {
    private ModDetection() {}

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isCreateLoaded() {
        return isModLoaded("create");
    }

    public static boolean isVintageMachinesLoaded() {
        return isModLoaded("vintagemachines") || isModLoaded("vintage_machines");
    }

    public static boolean isPamsHarvestCraftLoaded() {
        return isModLoaded("pamhc2crops") || isModLoaded("pamhc2trees") || isModLoaded("pamhc2foodcore") || isModLoaded("pamhc2foodextended");
    }

    public static boolean isSilentGearLoaded() {
        return isModLoaded("silentgear");
    }

    public static boolean isAllTheOresLoaded() {
        return isModLoaded("alltheores");
    }

    public static boolean isRichStuffLoaded() {
        return isModLoaded("richstuff");
    }

    public static int getModPriority(String modId) {
        return switch (modId) {
            case "alltheores" -> 1;
            case "richstuff" -> 2;
            case "create" -> 3;
            case "pamhc2crops", "pamhc2trees", "pamhc2foodcore", "pamhc2foodextended" -> 4;
            default -> 5;
        };
    }

    public static String getHighestPriorityModForItem(String itemId) {
        // This would be used to determine which mod's version of an item to use
        // For now, return the highest priority mod that provides this item
        return "alltheores"; // Default to AllTheOres as highest priority
    }
}