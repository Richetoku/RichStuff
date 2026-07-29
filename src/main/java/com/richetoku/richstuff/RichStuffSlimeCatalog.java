package com.richetoku.richstuff;

public final class RichStuffSlimeCatalog {
    private RichStuffSlimeCatalog() {}

    public record MetalSlimeDef(String material, String entityId, String nuggetId, int tier, int red, int green, int blue, int minY, int maxY, int weight, int minCount, int maxCount, float spawnChance, String biomeProfile) {}

    public static final MetalSlimeDef[] METAL_SLIMES = new MetalSlimeDef[] {
        new MetalSlimeDef("aluminum", "aluminum_slime", "aluminum_nugget", 1, 217, 217, 217, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("andesite_alloy", "andesite_alloy_slime", "andesite_alloy_nugget", 2, 210, 210, 162, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("azure_electrum", "azure_electrum_slime", "azure_electrum_nugget", 5, 10, 16, 208, -64, 16, 8, 1, 1, 0.055F, "deep_overworld"),
        new MetalSlimeDef("azure_silver", "azure_silver_slime", "azure_silver_nugget", 3, 125, 60, 231, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("blaze_gold", "blaze_gold_slime", "blaze_gold_nugget", 3, 255, 165, 61, 0, 128, 12, 1, 2, 0.085F, "nether"),
        new MetalSlimeDef("brass", "brass_slime", "brass_nugget", 2, 212, 166, 106, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("bronze", "bronze_slime", "bronze_nugget", 2, 196, 126, 80, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("cast_iron", "cast_iron_slime", "cast_iron_nugget", 1, 91, 91, 91, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("cobalt", "cobalt_slime", "cobalt_nugget", 3, 0, 71, 171, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("constantan", "constantan_slime", "constantan_nugget", 2, 160, 145, 133, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("copper", "copper_slime", "copper_nugget", 1, 207, 154, 99, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("copronickel", "copronickel_slime", "copronickel_nugget", 2, 169, 178, 179, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("crimson_iron", "crimson_iron_slime", "crimson_iron_nugget", 3, 211, 47, 85, 0, 128, 12, 1, 2, 0.085F, "nether"),
        new MetalSlimeDef("crimson_steel", "crimson_steel_slime", "crimson_steel_nugget", 4, 226, 73, 79, 0, 128, 10, 1, 1, 0.070F, "nether"),
        new MetalSlimeDef("dark_iron", "dark_iron_slime", "dark_iron_nugget", 4, 37, 39, 43, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("eclipse_alloy", "eclipse_alloy_slime", "eclipse_alloy_nugget", 7, 42, 42, 42, -64, 16, 4, 1, 1, 0.025F, "deep_overworld"),
        new MetalSlimeDef("electrum", "electrum_slime", "electrum_nugget", 2, 230, 207, 117, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("enderium", "enderium_slime", "enderium_nugget", 4, 89, 143, 123, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("ferricore", "ferricore_slime", "ferricore_nugget", 4, 127, 170, 187, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("gold", "gold_slime", "gold_nugget", 1, 232, 201, 88, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("invar", "invar_slime", "invar_nugget", 2, 169, 181, 154, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("iridium", "iridium_slime", "iridium_nugget", 4, 198, 198, 198, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("iron", "iron_slime", "iron_nugget", 1, 204, 204, 204, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("lead", "lead_slime", "lead_nugget", 1, 113, 126, 146, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("lithium", "lithium_slime", "lithium_nugget", 3, 215, 217, 232, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("lumium", "lumium_slime", "lumium_nugget", 4, 246, 242, 152, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("netherite", "netherite_slime", "netherite_nugget", 6, 106, 106, 107, -64, 16, 6, 1, 1, 0.040F, "deep_overworld"),
        new MetalSlimeDef("nickel", "nickel_slime", "nickel_nugget", 1, 197, 194, 151, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("osmium", "osmium_slime", "osmium_nugget", 3, 124, 143, 151, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("pig_iron", "pig_iron_slime", "pig_iron_nugget", 1, 184, 115, 51, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("platinum", "platinum_slime", "platinum_nugget", 3, 167, 177, 195, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("reinforced_copper", "reinforced_copper_slime", "reinforced_copper_nugget", 2, 204, 119, 34, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("signalum", "signalum_slime", "signalum_nugget", 4, 176, 91, 81, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("silver", "silver_slime", "silver_nugget", 1, 172, 179, 193, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("steel", "steel_slime", "steel_nugget", 2, 202, 202, 202, -48, 72, 14, 1, 2, 0.100F, "overworld"),
        new MetalSlimeDef("tin", "tin_slime", "tin_nugget", 1, 202, 202, 203, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("tyrian_steel", "tyrian_steel_slime", "tyrian_steel_nugget", 7, 153, 0, 103, -64, 16, 4, 1, 1, 0.025F, "deep_overworld"),
        new MetalSlimeDef("uranium", "uranium_slime", "uranium_nugget", 4, 80, 200, 120, -64, 16, 10, 1, 1, 0.070F, "deep_overworld"),
        new MetalSlimeDef("vanadium", "vanadium_slime", "vanadium_nugget", 3, 120, 149, 168, -40, 96, 12, 1, 2, 0.085F, "mountain_caves"),
        new MetalSlimeDef("wrought_iron", "wrought_iron_slime", "wrought_iron_nugget", 1, 157, 157, 157, -48, 72, 16, 1, 2, 0.115F, "overworld"),
        new MetalSlimeDef("zinc", "zinc_slime", "zinc_nugget", 1, 212, 213, 189, -48, 72, 16, 1, 2, 0.115F, "overworld")
    };
}
