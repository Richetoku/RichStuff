package com.richetoku.richstuff;

import com.richetoku.richcore.MaterialDef;
import com.richetoku.richcore.RichContentPartition;
import com.richetoku.richcore.RichStuffCatalog;
import com.mojang.logging.LogUtils;
import com.richetoku.richcore.api.RichTierProgressionApi;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.RikumiMitaMenu;
import com.richetoku.richstuff.rikumimita.RikumiMitaPresentBlock;
import com.richetoku.richstuff.rikumimita.RikumiMitaPresentBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * Rich Stuff core module: seven-tier materials, molten fluids, molds, crystals, coins,
 * native modular gear, standard foundry, alloy foundry, pouring and casting.
 */
@Mod(RichStuff.MODID)
public final class RichStuff {
    public static final String MODID = "richstuff";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCK_REGISTRY = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEM_REGISTRY = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_REGISTRY = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final Map<String, DeferredBlock<Block>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> CRYSTAL_GROWTH_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>>> METAL_SLIMES = new LinkedHashMap<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> RICH_GEAR_LEVEL_UP = SOUND_REGISTRY.register("rich_gear_level_up",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "rich_gear_level_up")));

    public static final DeferredBlock<CoinPileBlock> COIN_PILE = BLOCK_REGISTRY.register("coin_pile", () -> new CoinPileBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.25F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoinPileBlockEntity>> COIN_PILE_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("coin_pile", () -> BlockEntityType.Builder.of(CoinPileBlockEntity::new, COIN_PILE.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<RikumiMitaEntity>> RIKUMI_MITA_ENTITY = ENTITY_REGISTRY.register(
            "rikumi_mita", () -> EntityType.Builder.<RikumiMitaEntity>of(RikumiMitaEntity::new, MobCategory.CREATURE)
                    .sized(0.72F, 2.15F).clientTrackingRange(12).updateInterval(2)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "rikumi_mita").toString()));
    public static final DeferredHolder<MenuType<?>, MenuType<RikumiMitaMenu>> RIKUMI_MITA_MENU =
            MENU_REGISTRY.register("rikumi_mita", () -> IMenuTypeExtension.create(RikumiMitaMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RichFoundryMenu>> FOUNDRY_MENU =
            MENU_REGISTRY.register("foundry", () -> IMenuTypeExtension.create(RichFoundryMenu::new));
    public static final DeferredBlock<RikumiMitaPresentBlock> RIKUMI_MITA_PRESENT = BLOCK_REGISTRY.register(
            "rikumi_mita_present", () -> new RikumiMitaPresentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA).strength(0.6F).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RikumiMitaPresentBlockEntity>> RIKUMI_MITA_PRESENT_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("rikumi_mita_present", () -> BlockEntityType.Builder.of(
                    RikumiMitaPresentBlockEntity::new, RIKUMI_MITA_PRESENT.get()).build(null));
    public static final DeferredItem<BlockItem> RIKUMI_MITA_PACKAGE = ITEM_REGISTRY.register(
            "rikumi_mita_package", () -> new BlockItem(RIKUMI_MITA_PRESENT.get(), new Item.Properties().stacksTo(1)));

    /** Rich Stuff owns the optional visualizer item; common visualization APIs live in RichCore. */
    public static final DeferredItem<RichVisualizerItem> RICH_VISUALIZER = ITEM_REGISTRY.register(
            "rich_visualizer", () -> new RichVisualizerItem(new Item.Properties()));

    public static final DeferredBlock<RichFoundryCasingBlock> FOUNDRY_CASING = BLOCK_REGISTRY.register(
            "foundry_casing", () -> new RichFoundryCasingBlock(foundryProperties()));
    public static final DeferredBlock<RichFoundryDrainBlock> FOUNDRY_DRAIN = BLOCK_REGISTRY.register(
            "foundry_drain", () -> new RichFoundryDrainBlock(foundryProperties()));
    public static final DeferredBlock<RichFoundryFaucetBlock> FOUNDRY_FAUCET = BLOCK_REGISTRY.register(
            "foundry_faucet", () -> new RichFoundryFaucetBlock(foundryProperties().noOcclusion()));
    public static final DeferredBlock<RichFoundryBlock> FOUNDRY_CONTROLLER = BLOCK_REGISTRY.register(
            "foundry_controller", () -> new RichFoundryBlock(foundryProperties(), false));
    public static final DeferredBlock<RichFoundryBlock> ALLOY_FOUNDRY_CONTROLLER = BLOCK_REGISTRY.register(
            "alloy_foundry_controller", () -> new RichFoundryBlock(foundryProperties(), true));
    public static final DeferredBlock<RichCastingBlock> CASTING_TABLE = BLOCK_REGISTRY.register(
            "casting_table", () -> new RichCastingBlock(foundryProperties().noOcclusion(), false));
    public static final DeferredBlock<RichCastingBlock> CASTING_BASIN = BLOCK_REGISTRY.register(
            "casting_basin", () -> new RichCastingBlock(foundryProperties().noOcclusion(), true));

    public static final List<DeferredBlock<RichTankBlock>> RICH_TANKS = new ArrayList<>();
    public static final List<DeferredItem<RichTankBlockItem>> RICH_TANK_ITEMS = new ArrayList<>();
    static {
        for (int tier = 1; tier <= 7; tier++) {
            final int tankTier = tier;
            String id = "rich_tank_" + roman(tier).toLowerCase(Locale.ROOT);
            DeferredBlock<RichTankBlock> block = BLOCK_REGISTRY.register(id,
                    () -> new RichTankBlock(foundryProperties().noOcclusion(), tankTier));
            DeferredItem<RichTankBlockItem> item = ITEM_REGISTRY.register(id,
                    () -> new RichTankBlockItem(block.get(), new Item.Properties(), tankTier));
            RICH_TANKS.add(block);
            RICH_TANK_ITEMS.add(item);
        }
    }
    public static final List<DeferredBlock<RichBarrelBlock>> RICH_BARRELS = new ArrayList<>();
    public static final List<DeferredItem<RichBarrelBlockItem>> RICH_BARREL_ITEMS = new ArrayList<>();
    static {
        for (int tier = 1; tier <= 7; tier++) {
            final int barrelTier = tier;
            String id = "rich_barrel_" + roman(tier).toLowerCase(Locale.ROOT);
            DeferredBlock<RichBarrelBlock> block = BLOCK_REGISTRY.register(id,
                    () -> new RichBarrelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(3.5F, 8.0F).sound(SoundType.WOOD), barrelTier));
            DeferredItem<RichBarrelBlockItem> item = ITEM_REGISTRY.register(id,
                    () -> new RichBarrelBlockItem(block.get(), new Item.Properties(), barrelTier));
            RICH_BARRELS.add(block); RICH_BARREL_ITEMS.add(item);
        }
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RichBarrelBlockEntity>> RICH_BARREL_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("rich_barrel", () -> BlockEntityType.Builder.of(
                    RichBarrelBlockEntity::new, RICH_BARRELS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RichBarrelMenu>> RICH_BARREL_MENU =
            MENU_REGISTRY.register("rich_barrel", () -> IMenuTypeExtension.create(RichBarrelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RichTankMenu>> RICH_TANK_MENU =
            MENU_REGISTRY.register("rich_tank", () -> IMenuTypeExtension.create(RichTankMenu::new));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RichFoundryBlockEntity>> FOUNDRY_CONTROLLER_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("foundry_controller", () -> BlockEntityType.Builder.of(
                    RichFoundryBlockEntity::new, FOUNDRY_CONTROLLER.get(), ALLOY_FOUNDRY_CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RichCastingBlockEntity>> CASTING_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("casting", () -> BlockEntityType.Builder.of(
                    RichCastingBlockEntity::new, CASTING_TABLE.get(), CASTING_BASIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RichTankBlockEntity>> RICH_TANK_ENTITY =
            BLOCK_ENTITY_REGISTRY.register("rich_tank", () -> BlockEntityType.Builder.of(
                    RichTankBlockEntity::new, RICH_TANKS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredItem<BlockItem> FOUNDRY_CASING_ITEM = blockItem("foundry_casing", FOUNDRY_CASING);
    public static final DeferredItem<BlockItem> FOUNDRY_DRAIN_ITEM = blockItem("foundry_drain", FOUNDRY_DRAIN);
    public static final DeferredItem<BlockItem> FOUNDRY_FAUCET_ITEM = blockItem("foundry_faucet", FOUNDRY_FAUCET);
    public static final DeferredItem<BlockItem> FOUNDRY_CONTROLLER_ITEM = blockItem("foundry_controller", FOUNDRY_CONTROLLER);
    public static final DeferredItem<BlockItem> ALLOY_FOUNDRY_CONTROLLER_ITEM = blockItem("alloy_foundry_controller", ALLOY_FOUNDRY_CONTROLLER);
    public static final DeferredItem<BlockItem> CASTING_TABLE_ITEM = blockItem("casting_table", CASTING_TABLE);
    public static final DeferredItem<BlockItem> CASTING_BASIN_ITEM = blockItem("casting_basin", CASTING_BASIN);

    public static final DeferredItem<RichGearToolItems.Pickaxe> RICH_PICKAXE = ITEM_REGISTRY.register("rich_pickaxe", () -> new RichGearToolItems.Pickaxe(new Item.Properties()));
    public static final DeferredItem<RichGearToolItems.Axe> RICH_AXE = ITEM_REGISTRY.register("rich_axe", () -> new RichGearToolItems.Axe(new Item.Properties()));
    public static final DeferredItem<RichGearToolItems.Shovel> RICH_SHOVEL = ITEM_REGISTRY.register("rich_shovel", () -> new RichGearToolItems.Shovel(new Item.Properties()));
    public static final DeferredItem<RichGearToolItems.Hoe> RICH_HOE = ITEM_REGISTRY.register("rich_hoe", () -> new RichGearToolItems.Hoe(new Item.Properties()));
    public static final DeferredItem<RichGearToolItems.Sword> RICH_SWORD = ITEM_REGISTRY.register("rich_sword", () -> new RichGearToolItems.Sword(new Item.Properties()));
    public static final DeferredItem<RichGearArmorItem> RICH_HELMET = ITEM_REGISTRY.register("rich_helmet", () -> new RichGearArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredItem<RichGearArmorItem> RICH_CHESTPLATE = ITEM_REGISTRY.register("rich_chestplate", () -> new RichGearArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final DeferredItem<RichGearArmorItem> RICH_LEGGINGS = ITEM_REGISTRY.register("rich_leggings", () -> new RichGearArmorItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final DeferredItem<RichGearArmorItem> RICH_BOOTS = ITEM_REGISTRY.register("rich_boots", () -> new RichGearArmorItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RICHSTUFF_TAB = CREATIVE_TABS.register("richstuff", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.richstuff")).withTabsBefore(CreativeModeTabs.INGREDIENTS)
            .icon(() -> FOUNDRY_CONTROLLER_ITEM.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(RIKUMI_MITA_PACKAGE.get());
                ITEMS.forEach((id, holder) -> { if (RichStuffMaterialDefinitions.isRegisteredFormEnabled(id)) out.accept(holder.get()); });
            }).build());

    public RichStuff(IEventBus modBus, ModContainer container) {
        RichTierProgressionApi.registerResolver(MODID, RichStuffProgression::tierFor);
        registerManualContent();
        registerGeneratedContent();
        registerMetalSlimes();
        RichStuffFallbackFluids.registerAll();
        BLOCK_REGISTRY.register(modBus); ITEM_REGISTRY.register(modBus); RichStuffMoltenFluids.FLUID_TYPES.register(modBus);
        RichStuffMoltenFluids.FLUIDS.register(modBus); ENTITY_REGISTRY.register(modBus); BLOCK_ENTITY_REGISTRY.register(modBus);
        MENU_REGISTRY.register(modBus); CREATIVE_TABS.register(modBus); SOUND_REGISTRY.register(modBus); RichStuffConditions.CONDITION_CODECS.register(modBus);
        modBus.addListener(RichStuff::registerSpawnPlacements); modBus.addListener(RichStuff::registerEntityAttributes);
        modBus.addListener(RichStuff::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(RichStuff::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(RichGearEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(RichGearEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(RichGearEvents::onAnvil);
        NeoForge.EVENT_BUS.addListener(RichGearEvents::onPlayerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(RichStuffClient::clientSetup); modBus.addListener(RichStuffClient::registerLayerDefinitions);
            modBus.addListener(RichStuffClient::registerRenderers); modBus.addListener(RichStuffClient::registerItemDecorations); modBus.addListener(RichStuffClient::registerMenuScreens);
        }
        container.registerConfig(ModConfig.Type.COMMON, RichStuffConfig.SPEC);
        LOGGER.info("Rich Stuff core registered {} blocks, {} items, {} molten families and {} native gear material profiles.",
                BLOCKS.size(), ITEMS.size(), RichStuffMoltenFluids.MOLTEN.size(), RichGearProfiles.all().size());
    }

    private static void registerManualContent() {
        putBlockItem("foundry_casing", FOUNDRY_CASING, FOUNDRY_CASING_ITEM); putBlockItem("foundry_drain", FOUNDRY_DRAIN, FOUNDRY_DRAIN_ITEM);
        putBlockItem("foundry_faucet", FOUNDRY_FAUCET, FOUNDRY_FAUCET_ITEM); putBlockItem("foundry_controller", FOUNDRY_CONTROLLER, FOUNDRY_CONTROLLER_ITEM);
        putBlockItem("alloy_foundry_controller", ALLOY_FOUNDRY_CONTROLLER, ALLOY_FOUNDRY_CONTROLLER_ITEM);
        putBlockItem("casting_table", CASTING_TABLE, CASTING_TABLE_ITEM); putBlockItem("casting_basin", CASTING_BASIN, CASTING_BASIN_ITEM);
        for (int i = 0; i < RICH_TANKS.size(); i++) putBlockItem("rich_tank_" + roman(i + 1).toLowerCase(Locale.ROOT), RICH_TANKS.get(i), RICH_TANK_ITEMS.get(i));
        for (int i = 0; i < RICH_BARRELS.size(); i++) putBlockItem("rich_barrel_" + roman(i + 1).toLowerCase(Locale.ROOT), RICH_BARRELS.get(i), RICH_BARREL_ITEMS.get(i));
        ITEMS.put("rich_pickaxe", RICH_PICKAXE); ITEMS.put("rich_axe", RICH_AXE); ITEMS.put("rich_shovel", RICH_SHOVEL);
        ITEMS.put("rich_hoe", RICH_HOE); ITEMS.put("rich_sword", RICH_SWORD); ITEMS.put("rich_helmet", RICH_HELMET);
        ITEMS.put("rich_chestplate", RICH_CHESTPLATE); ITEMS.put("rich_leggings", RICH_LEGGINGS); ITEMS.put("rich_boots", RICH_BOOTS);
        ITEMS.put("rich_visualizer", RICH_VISUALIZER);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RichStuffMaterialReloadListener());
        event.addListener(new RichFoundryRecipeReloadListener());
    }

    public static DeferredItem<? extends Item> item(String id) { return ITEMS.get(id); }
    public static Block blockOrNull(String id) { DeferredBlock<Block> holder = BLOCKS.get(id); return holder == null ? null : holder.get(); }
    public static RichStuffSlimeCatalog.MetalSlimeDef profileForSlime(EntityType<?> type) {
        for (RichStuffSlimeCatalog.MetalSlimeDef def : RichStuffSlimeCatalog.METAL_SLIMES) { var holder=METAL_SLIMES.get(def.material()); if(holder!=null&&holder.get()==type)return def; } return null;
    }
    public static String materialForSlime(EntityType<?> type) { var def=profileForSlime(type); return def==null?"unknown":def.material(); }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RICH_TANK_ENTITY.get(), (tank, side) -> tank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RICH_BARREL_ENTITY.get(),
                (barrel, side) -> barrel.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FOUNDRY_CONTROLLER_ENTITY.get(), (foundry, side) -> foundry.formed() ? foundry : null);
        Item[] tankItems = RICH_TANK_ITEMS.stream().map(DeferredItem::get).toArray(Item[]::new);
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new com.richetoku.richcore.api.RichFluidItemHandler(stack,
                        stack.getItem() instanceof RichTankBlockItem tank ? tank.capacity() : 1000), tankItems);
    }

    private static String roman(int value) {
        return switch (value) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; case 6 -> "VI"; default -> "VII"; };
    }

    private static void registerMetalSlimes() {
        for (RichStuffSlimeCatalog.MetalSlimeDef def : RichStuffSlimeCatalog.METAL_SLIMES) METAL_SLIMES.put(def.material(), ENTITY_REGISTRY.register(def.entityId(), () ->
                EntityType.Builder.<RichStuffMetalSlime>of(RichStuffMetalSlime::new, MobCategory.MONSTER).sized(1.04F,1.04F)
                        .clientTrackingRange(10).updateInterval(3).build(ResourceLocation.fromNamespaceAndPath(MODID,def.entityId()).toString())));
    }
    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) { METAL_SLIMES.values().forEach(holder -> event.register(holder.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RichStuffMetalSlime::checkMetalSlimeSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)); }
    private static void registerEntityAttributes(EntityAttributeCreationEvent event) { METAL_SLIMES.values().forEach(holder -> event.put(holder.get(), RichStuffMetalSlime.createAttributes().build())); event.put(RIKUMI_MITA_ENTITY.get(), RikumiMitaEntity.createAttributes().build()); }

    private static void registerGeneratedContent() {
        Set<String> jars = new HashSet<>(Set.of(RichStuffCatalog.STACKABLE_JARS));
        Set<String> jugs = new HashSet<>(Set.of(RichStuffCatalog.STACKABLE_JUGS));
        Set<String> blocks = new HashSet<>(Set.of(RichStuffCatalog.BLOCK_IDS));
        Set<String> items = new HashSet<>(Set.of(RichStuffCatalog.ITEM_ONLY_IDS));
        for (MaterialDef material : RichStuffCatalog.MATERIALS) {
            List<String> family = new ArrayList<>();
            blocks.stream().filter(id -> belongsToFamily(id, material.name())).forEach(family::add);
            items.stream().filter(id -> belongsToFamily(id, material.name())).forEach(family::add);
            family.stream().distinct().sorted().forEach(id -> { boolean block=blocks.remove(id); items.remove(id); if(RichContentPartition.isCoreId(id)) registerCatalogId(id,block,jars.contains(id),jugs.contains(id)); });
            if (material.kind().equals("dust")) {
                if (material.name().equals("redstone")) { registerCatalogId("redstone_small_dust",true,false,false); registerCatalogId("redstone_tiny_dust",true,false,false); }
                else if (!material.name().equals("glowstone")) { registerGeneratedItem(material.name()+"_small_dust"); registerGeneratedItem(material.name()+"_tiny_dust"); }
            }
            RichStuffMoltenFluids.register(material); if(material.isCrystal()) registerCrystalGrowthFamily(material);
        }
        List<String> rest=new ArrayList<>(); rest.addAll(blocks); rest.addAll(items); rest.stream().distinct().sorted().forEach(id->{ if(RichContentPartition.isCoreId(id)) registerCatalogId(id,blocks.contains(id),jars.contains(id),jugs.contains(id)); });
    }

    private static boolean belongsToFamily(String id,String material){ return id.equals(material)||id.startsWith(material+"_")||((id.startsWith("base_")||id.startsWith("filled_")||id.startsWith("unfired_"))&&id.contains("_"+material+"_")); }
    private static void registerCatalogId(String id,boolean isBlock,boolean jar,boolean jug) {
        if(ITEMS.containsKey(id))return;
        if(isBlock||isPlaceableMoldId(id)||isRedstoneWireId(id)) {
            DeferredBlock<Block> block=BLOCK_REGISTRY.register(id,blockFactory(id)); BLOCKS.put(id,block);
            DeferredItem<? extends Item> item=ITEM_REGISTRY.register(id,()->new BlockItem(block.get(),new Item.Properties())); ITEMS.put(id,item);
        } else if(id.endsWith("_coin_stack")) { String coinId=id.substring(0,id.length()-6); ResourceLocation key=ResourceLocation.fromNamespaceAndPath(MODID,coinId); ITEMS.put(id,ITEM_REGISTRY.register(id,()->new CoinStackItem(new Item.Properties(),key))); }
        else if(id.endsWith("_coin")) ITEMS.put(id,ITEM_REGISTRY.register(id,()->new CoinItem(new Item.Properties())));
        else registerGeneratedItem(id);
    }
    private static void registerGeneratedItem(String id){ if(!ITEMS.containsKey(id))ITEMS.put(id,ITEM_REGISTRY.register(id,()->new Item(new Item.Properties()))); }
    private static void registerCrystalGrowthFamily(MaterialDef material){ if(!ITEMS.containsKey(material.name())&&!ITEMS.containsKey(material.name()+"_shard"))registerGeneratedItem(material.name()+"_shard"); registerCrystalStage("small_"+material.name()+"_crystal_bud",material.name(),1,3,4); registerCrystalStage("medium_"+material.name()+"_crystal_bud",material.name(),2,4,3); registerCrystalStage("large_"+material.name()+"_crystal_bud",material.name(),3,5,3); registerCrystalStage(material.name()+"_crystal_cluster",material.name(),4,7,3); }
    private static void registerCrystalStage(String id,String material,int stage,float height,float offset){ if(BLOCKS.containsKey(id))return; DeferredBlock<Block> block=BLOCK_REGISTRY.register(id,()->new RichCrystalGrowthBlock(height,offset,BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(stage==4?1.5F:0.5F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion(),material,stage)); BLOCKS.put(id,block); CRYSTAL_GROWTH_BLOCKS.put(id,block); ITEMS.put(id,ITEM_REGISTRY.register(id,()->new BlockItem(block.get(),new Item.Properties()))); }
    private static Supplier<Block> blockFactory(String id){ return ()->{
        BlockBehaviour.Properties properties=coreBlockProperties(id);
        if(isPlaceableMoldId(id))return new MoldBlock(properties,id);
        if(isRedstoneWireId(id)){int max=id.equals("redstone_tiny_dust")?5:id.equals("redstone_small_dust")?10:15;return new RichRedstoneWireBlock(BlockBehaviour.Properties.of().noCollission().instabreak().replaceable().sound(SoundType.EMPTY),max);}
        if(id.equals("redstone_block"))return new RichRedstonePowerBlock(properties);
        if(id.equals("glowstone_block"))return new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.3F).sound(SoundType.GLASS).lightLevel(s->15));
        if(id.equals("melon_block"))return new RichProduceBundleBlock(properties, () -> Items.MELON);
        if(id.equals("pumpkin_block"))return new RichProduceBundleBlock(properties, () -> Items.PUMPKIN);
        if(id.startsWith("budding_")&&id.endsWith("_crystal")){String material=id.substring(8,id.length()-8);return new BuddingRichCrystalBlock(properties,material);}
        CrystalStage stage=crystalStage(id);
        if(stage!=null)return new RichCrystalGrowthBlock(stage.height(),stage.offset(),BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(stage.stage()==4?1.5F:0.5F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion(),stage.material(),stage.stage());
        return new Block(properties);
    }; }
    private static CrystalStage crystalStage(String id){
        if(id.startsWith("small_")&&id.endsWith("_crystal_bud"))return new CrystalStage(id.substring(6,id.length()-12),1,3,4);
        if(id.startsWith("medium_")&&id.endsWith("_crystal_bud"))return new CrystalStage(id.substring(7,id.length()-12),2,4,3);
        if(id.startsWith("large_")&&id.endsWith("_crystal_bud"))return new CrystalStage(id.substring(6,id.length()-12),3,5,3);
        if(id.endsWith("_crystal_cluster"))return new CrystalStage(id.substring(0,id.length()-16),4,7,3);
        return null;
    }
    private record CrystalStage(String material,int stage,float height,float offset){}
    private static BlockBehaviour.Properties coreBlockProperties(String id){ if(id.startsWith("budding_"))return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F,6).sound(SoundType.AMETHYST).requiresCorrectToolForDrops(); if(id.contains("mold"))return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(1,3).sound(SoundType.STONE).noOcclusion(); return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5,10).sound(SoundType.METAL).requiresCorrectToolForDrops(); }
    private static boolean isPlaceableMoldId(String id){return id.endsWith("_mold")&&!id.endsWith("_mold_press");}
    private static boolean isRedstoneWireId(String id){return id.equals("redstone_dust")||id.equals("redstone_small_dust")||id.equals("redstone_tiny_dust");}
    private static BlockBehaviour.Properties foundryProperties(){return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,18).sound(SoundType.METAL).requiresCorrectToolForDrops();}
    private static <T extends Block> DeferredItem<BlockItem> blockItem(String id,DeferredBlock<T> block){return ITEM_REGISTRY.register(id,()->new BlockItem(block.get(),new Item.Properties()));}
    @SuppressWarnings({"unchecked","rawtypes"}) private static void putBlockItem(String id,DeferredBlock<? extends Block> block,DeferredItem<? extends Item> item){BLOCKS.put(id,(DeferredBlock)block);ITEMS.put(id,item);}
}
