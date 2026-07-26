package com.richetoku.richstuff;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RichStuffMetalSlimeRenderer extends MobRenderer<RichStuffMetalSlime, SlimeModel<RichStuffMetalSlime>> {
    public RichStuffMetalSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(RichStuffMetalSlime entity) {
        return ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "textures/entity/slime/" + entity.materialId() + "_slime.png");
    }
}
