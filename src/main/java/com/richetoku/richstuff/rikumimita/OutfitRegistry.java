package com.richetoku.richstuff.rikumimita;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class OutfitRegistry {
    public record Outfit(String id, String label, ResourceLocation texture) {}

    public static final List<Outfit> OUTFITS = List.of(
            outfit("default", "Default Outfit"),
            outfit("cyber_goth", "Cyber Goth"),
            outfit("strawberry_cafe", "Strawberry Café"),
            outfit("moonlit_witch", "Moonlit Witch"),
            outfit("forest_explorer", "Forest Explorer"),
            outfit("winter_cozy", "Winter Cozy"),
            outfit("crimson_formal", "Crimson Formal"),
            outfit("azure_casual", "Azure Casual")
    );

    private static Outfit outfit(String id, String label) {
        return new Outfit(id, label, ResourceLocation.fromNamespaceAndPath(
                "richstuff", "textures/entity/rikumi_mita/outfits/" + id + ".png"));
    }

    private OutfitRegistry() {}

    public static Outfit byIndex(int index) {
        return OUTFITS.get(Math.floorMod(index, OUTFITS.size()));
    }
}
