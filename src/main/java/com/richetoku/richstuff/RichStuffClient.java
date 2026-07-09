package com.richetoku.richstuff;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RichStuffClient {
    private RichStuffClient() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        RichStuff.METAL_SLIMES.values().forEach(holder -> event.registerEntityRenderer(holder.get(), RichStuffMetalSlimeRenderer::new));
    }
}
