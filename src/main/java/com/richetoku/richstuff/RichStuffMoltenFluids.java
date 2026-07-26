package com.richetoku.richstuff;

import com.richetoku.richcore.MaterialDef;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Native molten fluids generated from the same material catalog as parts and molds. */
public final class RichStuffMoltenFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RichStuff.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, RichStuff.MODID);
    public static final Map<String, MoltenSet> MOLTEN = new LinkedHashMap<>();

    private RichStuffMoltenFluids() {}

    public static boolean supports(MaterialDef material) {
        return switch (material.kind()) {
            case "metal", "alloy", "gem", "crystal", "dust", "material" -> true;
            default -> false;
        };
    }

    /** Called while each material family is registered, keeping bucket order beside its parts. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(MaterialDef material) {
        if (!supports(material) || MOLTEN.containsKey(material.name())) return;

        String materialName = material.name();
        String fluidName = "molten_" + materialName;
        int rgb = Integer.parseInt(material.color().substring(1), 16);
        int tint = 0xFF000000 | rgb;
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(
                RichStuff.MODID, "block/fluid/" + fluidName + "_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(
                RichStuff.MODID, "block/fluid/" + fluidName + "_flow");

        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(fluidName, () ->
                new FluidType(FluidType.Properties.create()
                        .density(material.kind().equals("dust") ? 2200 : 3000)
                        .viscosity(material.kind().equals("dust") ? 5000 : 7000)
                        .temperature(material.kind().equals("gem") || material.kind().equals("crystal") ? 1500 : 1300)
                        .lightLevel(8)) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override public ResourceLocation getStillTexture() { return still; }
                            @Override public ResourceLocation getFlowingTexture() { return flowing; }
                            @Override public int getTintColor() { return tint; }
                        });
                    }
                });

        final Supplier<FlowingFluid>[] sourceRef = new Supplier[1];
        final Supplier<FlowingFluid>[] flowingRef = new Supplier[1];
        final Supplier<LiquidBlock>[] blockRef = new Supplier[1];
        final Supplier<BucketItem>[] bucketRef = new Supplier[1];

        Supplier<BaseFlowingFluid.Properties> properties = () -> new BaseFlowingFluid.Properties(
                type, sourceRef[0], flowingRef[0])
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .block(blockRef[0])
                .bucket(bucketRef[0]);

        DeferredHolder<Fluid, FlowingFluid> source = FLUIDS.register(fluidName,
                () -> new BaseFlowingFluid.Source(properties.get()));
        DeferredHolder<Fluid, FlowingFluid> flowingFluid = FLUIDS.register("flowing_" + fluidName,
                () -> new BaseFlowingFluid.Flowing(properties.get()));
        sourceRef[0] = source;
        flowingRef[0] = flowingFluid;

        DeferredBlock<LiquidBlock> block = RichStuff.BLOCK_REGISTRY.register(fluidName,
                () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.of()
                        .mapColor(MapColor.FIRE)
                        .replaceable()
                        .noCollission()
                        .strength(100.0F)
                        .pushReaction(PushReaction.DESTROY)
                        .noLootTable()
                        .liquid()
                        .lightLevel(state -> 8)));
        DeferredItem<BucketItem> bucket = RichStuff.ITEM_REGISTRY.register(fluidName + "_bucket",
                () -> new BucketItem(source.get(), moltenBucketProperties(material)));
        blockRef[0] = block;
        bucketRef[0] = bucket;

        RichStuff.BLOCKS.put(fluidName, (DeferredBlock) block);
        RichStuff.ITEMS.put(fluidName + "_bucket", bucket);
        MOLTEN.put(materialName, new MoltenSet(type, source, flowingFluid, block, bucket));
    }

    private static Item.Properties moltenBucketProperties(MaterialDef material) {
        CompoundTag data = new CompoundTag();
        data.putString("RichStuffMaterial", material.name());
        data.putString("RichStuffFluidTag", "c:molten/" + material.name());
        data.putString("RichGearMaterial", material.name());
        data.putBoolean("RichStuffMoltenMaterial", true);
        data.putBoolean("RichStuffPreserveGrade", true);
        return new Item.Properties()
                .craftRemainder(Items.BUCKET)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public record MoltenSet(
            Supplier<FluidType> type,
            Supplier<FlowingFluid> source,
            Supplier<FlowingFluid> flowing,
            Supplier<LiquidBlock> block,
            Supplier<BucketItem> bucket) {}
}
