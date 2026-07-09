package com.richetoku.richstuff;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

@Mod(RichStuff.MODID)
public class RichStuff {
    public static final String MODID = "richstuff";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCK_REGISTRY = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEM_REGISTRY = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Map<String, DeferredBlock<Block>> BLOCKS = new HashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new HashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>>> METAL_SLIMES = new LinkedHashMap<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RICHSTUFF_TAB = CREATIVE_TABS.register("richstuff", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.richstuff"))
        .withTabsBefore(CreativeModeTabs.INGREDIENTS)
        .icon(() -> item("green_heart").get().getDefaultInstance())
        .displayItems((params, out) -> {
            ITEMS.values().forEach(h -> out.accept(h.get()));
        }).build());

    public RichStuff(IEventBus modEventBus, ModContainer modContainer) {
        registerGeneratedContent();
        registerMetalSlimes();
        BLOCK_REGISTRY.register(modEventBus);
        ITEM_REGISTRY.register(modEventBus);
        ENTITY_REGISTRY.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        RichStuffConditions.CONDITION_CODECS.register(modEventBus);
        modEventBus.addListener(RichStuff::registerSpawnPlacements);
        modEventBus.addListener(RichStuff::registerEntityAttributes);
        NeoForge.EVENT_BUS.register(RichStuffSilentGearEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(RichStuffClient::registerRenderers);
        }
        modContainer.registerConfig(ModConfig.Type.COMMON, RichStuffConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, FeatureConfig.SPEC);
        LOGGER.info("RichStuff registered {} blocks, {} direct items, and {} metal slimes.", BLOCKS.size(), ITEMS.size(), METAL_SLIMES.size());
    }

    public static DeferredItem<? extends Item> item(String id) { return ITEMS.get(id); }

    public static String materialForSlime(EntityType<?> type) {
        RichStuffSlimeCatalog.MetalSlimeDef def = profileForSlime(type);
        return def == null ? "unknown" : def.material();
    }

    public static RichStuffSlimeCatalog.MetalSlimeDef profileForSlime(EntityType<?> type) {
        for (RichStuffSlimeCatalog.MetalSlimeDef def : RichStuffSlimeCatalog.METAL_SLIMES) {
            DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>> holder = METAL_SLIMES.get(def.material());
            if (holder != null && holder.get() == type) return def;
        }
        return null;
    }


    private static void registerMetalSlimes() {
        for (RichStuffSlimeCatalog.MetalSlimeDef def : RichStuffSlimeCatalog.METAL_SLIMES) {
            DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>> holder = ENTITY_REGISTRY.register(def.entityId(), () -> EntityType.Builder.<RichStuffMetalSlime>of(RichStuffMetalSlime::new, MobCategory.MONSTER)
                .sized(1.04F, 1.04F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build(def.entityId()));
            METAL_SLIMES.put(def.material(), holder);
        }
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        METAL_SLIMES.values().forEach(holder -> event.register(holder.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RichStuffMetalSlime::checkMetalSlimeSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        METAL_SLIMES.values().forEach(holder -> event.put(holder.get(), RichStuffMetalSlime.createAttributes().build()));
    }

    private static void registerGeneratedContent() {
        Set<String> jarSet = new HashSet<>(Set.of(RichStuffCatalog.STACKABLE_JARS));
        Set<String> jugSet = new HashSet<>(Set.of(RichStuffCatalog.STACKABLE_JUGS));
        Set<String> blockSet = new HashSet<>(Set.of(RichStuffCatalog.BLOCK_IDS));

        for (String id : RichStuffCatalog.BLOCK_IDS) {
            DeferredBlock<Block> block = BLOCK_REGISTRY.register(id, blockFactory(id, jarSet.contains(id), jugSet.contains(id)));
            BLOCKS.put(id, block);
            DeferredItem<? extends Item> blockItem = ITEM_REGISTRY.register(id, () -> new BlockItem(block.get(), itemProperties(id)));
            ITEMS.put(id, blockItem);
        }

        for (String id : RichStuffCatalog.ITEM_ONLY_IDS) {
            if (blockSet.contains(id) || ITEMS.containsKey(id)) continue;
            DeferredItem<? extends Item> item = ITEM_REGISTRY.register(id, () -> new Item(itemProperties(id)));
            ITEMS.put(id, item);
        }
    }

    private static Supplier<Block> blockFactory(String id, boolean jar, boolean jug) {
        return () -> {
            BlockBehaviour.Properties p = blockProperties(id);
            if (jar) return new StackableVesselBlock(p, "jar");
            if (jug) return new StackableVesselBlock(p, "jug");
            if (id.startsWith("budding_") && id.endsWith("_crystal")) {
                String material = id.substring(8, id.length() - 8);
                return new BuddingRichCrystalBlock(p, material);
            }
            return new Block(p);
        };
    }

    private static BlockBehaviour.Properties blockProperties(String id) {
        if (id.endsWith("_crate")) return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.2f).sound(SoundType.WOOD);
        if (id.endsWith("_bag")) return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.7f).sound(SoundType.WOOL);
        if (id.contains("jar") || id.endsWith("_jug")) return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.4f).sound(SoundType.GLASS).noOcclusion();
        if (id.contains("mold")) return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(1.0f, 3.0f).sound(SoundType.STONE).noOcclusion();
        if (id.contains("machine") || id.equals("centrifuge") || id.equals("laser") || id.equals("belt_sander") || id.equals("vibrating_table") || id.equals("compressor")) return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 8.0f).sound(SoundType.METAL);
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static Item.Properties itemProperties(String id) {
        Item.Properties p = new Item.Properties();
        if (id.equals("green_heart")) {
            p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().fast().build());
        } else if (id.equals("pie_crust") || id.equals("bread_slice")) {
            p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).fast().build());
        }
        return p;
    }
}
