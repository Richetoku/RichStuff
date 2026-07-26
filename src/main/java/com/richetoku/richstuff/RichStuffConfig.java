package com.richetoku.richstuff;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RichStuffConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_CRYSTAL_GROWTH;
    public static final ModConfigSpec.BooleanValue CREATE_RECIPES_REQUIRE_CREATE;
    public static final ModConfigSpec.BooleanValue ENABLE_METAL_SLIMES;
    public static final ModConfigSpec.DoubleValue METAL_SLIME_SPAWN_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_VANILLA_FALLBACK_RECIPES;
    public static final ModConfigSpec.IntValue RICH_GEAR_MAX_LEVEL;
    public static final ModConfigSpec.IntValue RICH_GEAR_REPAIR_AMOUNT;
    public static final ModConfigSpec.DoubleValue RICH_GEAR_REPAIR_PERCENT;
    public static final ModConfigSpec.IntValue FOUNDRY_BASE_CAPACITY;
    public static final ModConfigSpec.BooleanValue RICH_TANKS_RETAIN_FLUID;
    public static final ModConfigSpec.IntValue[] RICH_TANK_CAPACITIES = new ModConfigSpec.IntValue[7];

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("processing");
        ENABLE_CRYSTAL_GROWTH = builder.comment("Enable budding RichStuff crystal blocks to grow crystal blocks in adjacent air.").define("enableCrystalGrowth", true);
        CREATE_RECIPES_REQUIRE_CREATE = builder.comment("Generated Create recipe JSON is gated with a create mod_loaded condition.").define("createRecipesRequireCreate", true);
        ENABLE_VANILLA_FALLBACK_RECIPES = builder.comment("Enable direct vanilla crafting/smelting shortcuts. Disabled by default so materials must use the processing chain or powered machines.").define("enableVanillaFallbackRecipes", false);
        builder.pop();
        builder.push("metal_slimes");
        ENABLE_METAL_SLIMES = builder.comment("Enable colored RichStuff metal slimes that drop matching metal nuggets.").define("enableMetalSlimes", true);
        METAL_SLIME_SPAWN_MULTIPLIER = builder.comment("Global multiplier applied to metal slime spawn-rule chances. Biome modifier weights remain data-pack controlled.").defineInRange("metalSlimeSpawnMultiplier", 1.0D, 0.0D, 64.0D);
        builder.pop();
        builder.comment("Rich Gear accepts every RichStuff material family. Levels are earned by use; every five levels unlocks another material modifier slot.")
            .push("native_rich_gear");
        RICH_GEAR_MAX_LEVEL = builder.comment("Maximum effective Rich Gear level. Existing higher-level data is retained but operates at this cap.")
                .defineInRange("maximumLevel", 10, 1, 1000);
        RICH_GEAR_REPAIR_AMOUNT = builder.comment("Minimum durability restored by one matching primary-material item.")
                .defineInRange("repairAmount", 1, 0, 1000000);
        RICH_GEAR_REPAIR_PERCENT = builder.comment("Fraction of maximum durability restored by one matching primary-material item. The larger of this and repairAmount is used.")
                .defineInRange("repairPercent", 0.25D, 0.0D, 1.0D);
        builder.pop();
        builder.comment("Rich Tank capacities and Foundry shared storage, measured in millibuckets.").push("rich_tanks_and_foundry");
        FOUNDRY_BASE_CAPACITY = builder.comment("Base shared molten-fluid capacity of a completed Foundry before Rich Tanks are counted.")
                .defineInRange("foundryBaseCapacity", 12960, 1000, Integer.MAX_VALUE);
        RICH_TANKS_RETAIN_FLUID = builder.comment("Rich Tank item stacks retain their standalone fluid contents when broken.")
                .define("retainTankFluidWhenBroken", true);
        int[] defaults = {8000, 16000, 32000, 64000, 128000, 256000, 512000};
        for (int i = 0; i < defaults.length; i++) {
            RICH_TANK_CAPACITIES[i] = builder.comment("Capacity of Rich Tank tier " + (i + 1) + ".")
                    .defineInRange("richTank" + (i + 1) + "Capacity", defaults[i], 1000, Integer.MAX_VALUE);
        }
        builder.pop();
        SPEC = builder.build();
    }


    public static int richTankCapacity(int tier) {
        int index = Math.max(0, Math.min(RICH_TANK_CAPACITIES.length - 1, tier - 1));
        return RICH_TANK_CAPACITIES[index].get();
    }

    private RichStuffConfig() {}
}
