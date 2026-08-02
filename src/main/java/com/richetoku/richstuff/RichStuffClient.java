package com.richetoku.richstuff;

import com.richetoku.richcore.client.RichFluidItemDecorator;
import com.richetoku.richstuff.rikumimita.client.RikumiMitaModel;
import com.richetoku.richstuff.rikumimita.client.RikumiMitaRenderer;
import com.richetoku.richstuff.rikumimita.client.RikumiMitaScreen;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/** Client registrations owned by the Rich Stuff core module only. */
public final class RichStuffClient {
    private static final AtomicBoolean MENU_SCREENS_REGISTERED = new AtomicBoolean();
    private RichStuffClient() {}
    public static void clientSetup(FMLClientSetupEvent event) { }
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RikumiMitaModel.LAYER_LOCATION, RikumiMitaModel::createBodyLayer);
    }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        RichStuff.METAL_SLIMES.values().forEach(holder -> event.registerEntityRenderer(holder.get(), RichStuffMetalSlimeRenderer::new));
        event.registerEntityRenderer(RichStuff.RIKUMI_MITA_ENTITY.get(), RikumiMitaRenderer::new);
        event.registerBlockEntityRenderer(RichStuff.COIN_PILE_ENTITY.get(), CoinPileRenderer::new);
        event.registerBlockEntityRenderer(RichStuff.RICH_TANK_ENTITY.get(), RichTankRenderer::new);
    }
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        RichFluidItemDecorator decorator = new RichFluidItemDecorator(stack ->
                stack.getItem() instanceof RichTankBlockItem tank ? tank.capacity() : 1, false);
        RichStuff.RICH_TANK_ITEMS.forEach(item -> event.register(item.get(), decorator));
        RichFluidItemDecorator vesselDecorator = new RichFluidItemDecorator(stack -> 1000, true);
        RichStuff.ITEMS.forEach((id, holder) -> {
            if (RichStuff.isFluidVesselId(id)) event.register(holder.get(), vesselDecorator);
        });
        RichGearItemDecorator brokenDecorator = new RichGearItemDecorator();
        RichStuff.ITEMS.values().forEach(holder -> {
            if (holder.get() instanceof RichGearMarker) event.register(holder.get(), brokenDecorator);
        });
    }
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            private RichJarItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new RichJarItemRenderer();
                return renderer;
            }
        };
        var jars = RichStuff.ITEMS.entrySet().stream().filter(entry -> RichStuff.isFluidVesselId(entry.getKey()))
                .map(entry -> entry.getValue().get()).toArray(net.minecraft.world.item.Item[]::new);
        if (jars.length > 0) event.registerItem(extensions, jars);
    }
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        if (!MENU_SCREENS_REGISTERED.compareAndSet(false, true)) {
            RichStuff.LOGGER.warn("Ignoring duplicate RichStuff menu-screen registration event");
            return;
        }
        event.register(RichStuff.RIKUMI_MITA_MENU.get(), RikumiMitaScreen::new);
        event.register(RichStuff.FOUNDRY_MENU.get(), RichFoundryScreen::new);
        event.register(RichStuff.RICH_BARREL_MENU.get(), RichBarrelScreen::new);
        event.register(RichStuff.RICH_TANK_MENU.get(), RichTankScreen::new);
    }
}
