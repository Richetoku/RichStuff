package com.richetoku.richstuff;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FeatureConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_TINTED_INGOTS;
    public static final ModConfigSpec.BooleanValue ENABLE_VANILLA_CRAFTING_FALLBACK;
    public static final ModConfigSpec.BooleanValue ENABLE_CREATE_WIRE_RECIPES;
    public static final ModConfigSpec.BooleanValue ENABLE_VINTAGE_WIRE_RECIPES;
    public static final ModConfigSpec.BooleanValue ENABLE_JAM_JELLY_JARS;
    public static final ModConfigSpec.BooleanValue ENABLE_FOOD_JUGS;
    public static final ModConfigSpec.IntValue UNIFICATION_PRIORITY_ALLTHEORES;
    public static final ModConfigSpec.IntValue UNIFICATION_PRIORITY_RICHSTUFF;
    public static final ModConfigSpec.IntValue UNIFICATION_PRIORITY_CREATE;
    public static final ModConfigSpec.IntValue UNIFICATION_PRIORITY_PAMS;
    public static final ModConfigSpec.IntValue UNIFICATION_PRIORITY_OTHER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.push("textures");
        ENABLE_TINTED_INGOTS = builder.comment("Use tinted iron ingot texture for all metal ingots instead of individual textures.").define("enableTintedIngots", true);
        builder.pop();
        
        builder.push("crafting");
        ENABLE_VANILLA_CRAFTING_FALLBACK = builder.comment("Enable vanilla crafting recipes for springs, gears, and other tool-requiring items when Create or required machines are not available. Off by default.").define("enableVanillaCraftingFallback", false);
        ENABLE_CREATE_WIRE_RECIPES = builder.comment("Enable wire recipes using Create deployer with shears.").define("enableCreateWireRecipes", true);
        ENABLE_VINTAGE_WIRE_RECIPES = builder.comment("Enable wire recipes using Vintage machines coil machine when available.").define("enableVintageWireRecipes", true);
        builder.pop();
        
        builder.push("containers");
        ENABLE_JAM_JELLY_JARS = builder.comment("Enable full-height jam/jelly jars with dynamic content colors and proper hitboxes.").define("enableJamJellyJars", true);
        ENABLE_FOOD_JUGS = builder.comment("Enable jugs for food fluids (milk, water, etc.) usable in food recipes.").define("enableFoodJugs", true);
        builder.pop();
        
        builder.push("unification_priority");
        builder.comment("Priority order for item unification (lower number = higher priority). AllTheOres and RichStuff should be top priority.");
        UNIFICATION_PRIORITY_ALLTHEORES = builder.defineInRange("alltheores", 1, 1, 100);
        UNIFICATION_PRIORITY_RICHSTUFF = builder.defineInRange("richstuff", 2, 1, 100);
        UNIFICATION_PRIORITY_CREATE = builder.defineInRange("create", 3, 1, 100);
        UNIFICATION_PRIORITY_PAMS = builder.defineInRange("pams_harvestcraft", 4, 1, 100);
        UNIFICATION_PRIORITY_OTHER = builder.defineInRange("other_mods", 5, 1, 100);
        builder.pop();
        
        SPEC = builder.build();
    }

    private FeatureConfig() {}
}