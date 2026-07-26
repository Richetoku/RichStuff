package com.richetoku.richstuff.rikumimita.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.richetoku.richstuff.rikumimita.OutfitRegistry;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RikumiMitaRenderer extends MobRenderer<RikumiMitaEntity, RikumiMitaModel> {
    public RikumiMitaRenderer(EntityRendererProvider.Context context) {
        super(context, new RikumiMitaModel(context.bakeLayer(RikumiMitaModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(RikumiMitaEntity entity) {
        return OutfitRegistry.byIndex(entity.getOutfitIndex()).texture();
    }

    @Override
    protected void scale(RikumiMitaEntity entity, PoseStack poseStack, float partialTick) {
        // The supplied model is authored taller than a vanilla player. This scale recreates
        // the character proportions shown in the reference image while keeping a practical hitbox.
        poseStack.scale(0.72F, 0.72F, 0.72F);
    }
}
