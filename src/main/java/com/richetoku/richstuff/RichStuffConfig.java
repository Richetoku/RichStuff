package com.richetoku.richstuff;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RichStuffConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_CRYSTAL_GROWTH;
    public static final ModConfigSpec.BooleanValue CREATE_RECIPES_REQUIRE_CREATE;
    public static final ModConfigSpec.BooleanValue ENABLE_METAL_SLIMES;
    public static final ModConfigSpec.DoubleValue METAL_SLIME_SPAWN_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_MOLTEN_SILENT_GEAR_CASTING;
    public static final ModConfigSpec.BooleanValue CONVERT_VANILLA_TOOL_RECIPES_TO_SILENT_GEAR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("processing");
        ENABLE_CRYSTAL_GROWTH = builder.comment("Enable budding RichStuff crystal blocks to grow crystal blocks in adjacent air.").define("enableCrystalGrowth", true);
        CREATE_RECIPES_REQUIRE_CREATE = builder.comment("Generated Create recipe JSON is gated with a create mod_loaded condition.").define("createRecipesRequireCreate", true);
        builder.pop();
        builder.push("metal_slimes");
        ENABLE_METAL_SLIMES = builder.comment("Enable colored RichStuff metal slimes that drop matching metal nuggets.").define("enableMetalSlimes", true);
        METAL_SLIME_SPAWN_MULTIPLIER = builder.comment("Global multiplier applied to metal slime spawn-rule chances. Biome modifier weights remain data-pack controlled.").defineInRange("metalSlimeSpawnMultiplier", 1.0D, 0.0D, 64.0D);
        builder.pop();
        builder.push("silent_gear");
        ENABLE_MOLTEN_SILENT_GEAR_CASTING = builder.comment("Enable RichStuff molten metal mold recipes that output Silent Gear parts when Silent Gear and Create are loaded.").define("enableMoltenSilentGearCasting", true);
        CONVERT_VANILLA_TOOL_RECIPES_TO_SILENT_GEAR = builder.comment("When true, RichStuff adds server-side handling and optional duplicate vanilla-pattern recipes so vanilla tool/armor crafting produces Silent Gear equivalents. Off by default so vanilla tool crafting stays untouched.").define("convertVanillaToolRecipesToSilentGear", false);
        builder.pop();
        SPEC = builder.build();
    }

    private RichStuffConfig() {}
}
