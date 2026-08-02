package com.richetoku.richstuff.rikumimita.client;

import com.richetoku.richstuff.rikumimita.OutfitRegistry;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class RikumiMitaRenderer extends MobRenderer<RikumiMitaEntity, RikumiMitaModel> {
    public RikumiMitaRenderer(EntityRendererProvider.Context context) {
        super(context, new RikumiMitaModel(context.bakeLayer(RikumiMitaModel.LAYER_LOCATION)), 0.50F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RikumiMitaEntity entity) {
        return OutfitRegistry.byIndex(entity.getOutfitIndex()).texture();
    }

}
