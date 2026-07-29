package com.richetoku.richstuff;

import com.richetoku.richcore.RichStuffCatalog;
import com.richetoku.richcore.RichProduceCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Data-driven fallback fluids for legacy jar contents. Runtime compatibility always prefers an
 * external fluid with a matching semantic/tag identity; these namespaced definitions guarantee a
 * usable last resort without replacing or remapping another mod's registry objects.
 */
public final class RichStuffFallbackFluids {
    public static final Map<String, FluidSet> FLUIDS = new LinkedHashMap<>();
    private RichStuffFallbackFluids() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerAll() {
        if (!FLUIDS.isEmpty()) return;
        Set<String> contents = new LinkedHashSet<>();
        for (String id : RichStuffCatalog.STACKABLE_JARS) addContent(contents, id, "_jar");
        for (String produce : RichProduceCatalog.JUICEABLE_IDS) contents.add(produce + "_juice");
        contents.add("cream");
        contents.add("cream_frosting");
        contents.remove("empty");
        contents.remove("water");
        contents.remove("lava");

        for (String key : contents) {
            int tint = tintFor(key);
            int alpha = key.contains("oil") || key.contains("syrup") || key.contains("honey") ? 0xD8 : 0xF0;
            ResourceLocation still = ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "block/fluid/fallback_still");
            ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "block/fluid/fallback_flow");
            DeferredHolder<FluidType, FluidType> type = RichStuffMoltenFluids.FLUID_TYPES.register(key, () -> new FluidType(
                    FluidType.Properties.create().density(density(key)).viscosity(viscosity(key)).temperature(temperature(key))
                            .lightLevel(key.contains("glow") ? 8 : 0)) {
                @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return still; }
                        @Override public ResourceLocation getFlowingTexture() { return flowing; }
                        @Override public int getTintColor() { return alpha << 24 | tint; }
                    });
                }
            });
            final Supplier<FlowingFluid>[] sourceRef = new Supplier[1];
            final Supplier<FlowingFluid>[] flowingRef = new Supplier[1];
            final Supplier<LiquidBlock>[] blockRef = new Supplier[1];
            Supplier<BaseFlowingFluid.Properties> props = () -> new BaseFlowingFluid.Properties(type, sourceRef[0], flowingRef[0])
                    .slopeFindDistance(2).levelDecreasePerBlock(2).block(blockRef[0]);
            DeferredHolder<Fluid, FlowingFluid> source = RichStuffMoltenFluids.FLUIDS.register(key,
                    () -> new BaseFlowingFluid.Source(props.get()));
            DeferredHolder<Fluid, FlowingFluid> flowingFluid = RichStuffMoltenFluids.FLUIDS.register("flowing_" + key,
                    () -> new BaseFlowingFluid.Flowing(props.get()));
            sourceRef[0] = source;
            flowingRef[0] = flowingFluid;
            DeferredBlock<LiquidBlock> block = RichStuff.BLOCK_REGISTRY.register("fluid_" + key,
                    () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                            .replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                            .noLootTable().liquid().lightLevel(state -> key.contains("glow") ? 8 : 0)));
            blockRef[0] = block;
            FLUIDS.put(key, new FluidSet(type, source, flowingFluid, block));
        }
    }

    private static void addContent(Set<String> out, String id, String suffix) {
        if (id.endsWith(suffix)) out.add(id.substring(0, id.length() - suffix.length()));
    }
    private static int density(String key) { return key.contains("oil") ? 850 : key.contains("milk") || key.equals("cream") ? 1030 : 1200; }
    private static int viscosity(String key) { return key.contains("jam") || key.contains("jelly") || key.contains("frosting") ? 6000 : key.contains("syrup") || key.contains("honey") ? 4000 : key.contains("oil") ? 1600 : 1200; }
    private static int temperature(String key) { return 300; }
    private static int tintFor(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        if (k.contains("milk") || k.equals("cream") || k.contains("cream_frosting")) return 0xFFF8E7;
        if (k.contains("chocolate")) return 0x6B351F;
        if (k.contains("honey") || k.contains("syrup")) return 0xD99018;
        if (k.contains("oil")) return 0xB8A83A;
        if (k.contains("blueberry") || k.contains("blackberry") || k.contains("elderberry")) return 0x45225E;
        if (k.contains("lime") || k.contains("kiwi") || k.contains("melon")) return 0x75A83D;
        if (k.contains("orange") || k.contains("apricot") || k.contains("mango") || k.contains("peach")) return 0xE57A2D;
        if (k.contains("lemon") || k.contains("pineapple") || k.contains("banana")) return 0xE4C840;
        if (k.contains("grape") || k.contains("plum")) return 0x713B7E;
        int hash = Math.abs(k.hashCode());
        int r = 96 + hash % 144, g = 48 + (hash / 17) % 144, b = 48 + (hash / 47) % 144;
        return r << 16 | g << 8 | b;
    }

    public record FluidSet(Supplier<FluidType> type, Supplier<FlowingFluid> source, Supplier<FlowingFluid> flowing, Supplier<LiquidBlock> block) {}
}
