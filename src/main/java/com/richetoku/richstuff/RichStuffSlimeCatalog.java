package com.richetoku.richstuff;

public final class RichStuffSlimeCatalog {
    private RichStuffSlimeCatalog() {}

    public record MetalSlimeDef(String material, String entityId, String nuggetId, int red, int green, int blue, int minY, int maxY, int weight, int minCount, int maxCount, float spawnChance, String biomeProfile) {}

    public static final MetalSlimeDef[] METAL_SLIMES = new MetalSlimeDef[] {
        new MetalSlimeDef("copper", "copper_slime", "copper_nugget", 210, 114, 62, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("gold", "gold_slime", "gold_nugget", 255, 213, 71, -64, 80, 10, 1, 2, 0.08F, "gold"),
        new MetalSlimeDef("iron", "iron_slime", "iron_nugget", 216, 213, 204, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("lead", "lead_slime", "lead_nugget", 93, 103, 132, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("nickel", "nickel_slime", "nickel_nugget", 205, 197, 145, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("silver", "silver_slime", "silver_nugget", 205, 218, 228, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("tin", "tin_slime", "tin_nugget", 190, 205, 210, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("uranium", "uranium_slime", "uranium_nugget", 80, 205, 88, -64, 16, 8, 1, 1, 0.06F, "deep_overworld"),
        new MetalSlimeDef("iridium", "iridium_slime", "iridium_nugget", 190, 202, 255, -64, 16, 8, 1, 1, 0.06F, "deep_overworld"),
        new MetalSlimeDef("osmium", "osmium_slime", "osmium_nugget", 125, 160, 208, -64, 16, 8, 1, 1, 0.06F, "deep_overworld"),
        new MetalSlimeDef("platinum", "platinum_slime", "platinum_nugget", 185, 235, 233, -64, 16, 8, 1, 1, 0.06F, "deep_overworld"),
        new MetalSlimeDef("zinc", "zinc_slime", "zinc_nugget", 170, 185, 196, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("aluminum", "aluminum_slime", "aluminum_nugget", 196, 207, 210, -48, 72, 14, 1, 2, 0.1F, "overworld"),
        new MetalSlimeDef("cobalt", "cobalt_slime", "cobalt_nugget", 47, 95, 218, -64, 16, 8, 1, 1, 0.06F, "deep_overworld"),
        new MetalSlimeDef("azure_silver", "azure_silver_slime", "azure_silver_nugget", 80, 185, 236, -40, 96, 7, 1, 1, 0.06F, "mountain_caves"),
        new MetalSlimeDef("crimson_iron", "crimson_iron_slime", "crimson_iron_nugget", 178, 42, 45, 0, 128, 12, 1, 2, 0.08F, "nether")
    };
}
