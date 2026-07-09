package com.richetoku.richstuff;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RichStuffConditions {
    private RichStuffConditions() {}

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RichStuff.MODID);
    public static final Supplier<MapCodec<ServerConfigCondition>> SERVER_CONFIG = CONDITION_CODECS.register("server_config", () -> ServerConfigCondition.CODEC);

    public record ServerConfigCondition(String option, boolean expected) implements ICondition {
        public static final MapCodec<ServerConfigCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("option").forGetter(ServerConfigCondition::option),
            Codec.BOOL.optionalFieldOf("expected", true).forGetter(ServerConfigCondition::expected)
        ).apply(instance, ServerConfigCondition::new));

        @Override
        public boolean test(ICondition.IContext context) {
            boolean value = switch (option) {
                case "enableMoltenSilentGearCasting" -> RichStuffConfig.ENABLE_MOLTEN_SILENT_GEAR_CASTING.get();
                case "convertVanillaToolRecipesToSilentGear" -> RichStuffConfig.CONVERT_VANILLA_TOOL_RECIPES_TO_SILENT_GEAR.get();
                case "enableMetalSlimes" -> RichStuffConfig.ENABLE_METAL_SLIMES.get();
                case "enableCrystalGrowth" -> RichStuffConfig.ENABLE_CRYSTAL_GROWTH.get();
                default -> false;
            };
            return value == expected;
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
