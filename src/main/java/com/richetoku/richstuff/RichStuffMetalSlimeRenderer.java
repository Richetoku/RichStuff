package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RichStuffMetalSlimeRenderer extends MobRenderer<RichStuffMetalSlime, SlimeModel<RichStuffMetalSlime>> {
    public RichStuffMetalSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(RichStuffMetalSlime entity) {
        return ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "textures/entity/slime/" + entity.materialId() + "_slime.png");
    }

    @Nullable
    @Override
    protected RenderType getRenderType(RichStuffMetalSlime entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    public void render(RichStuffMetalSlime entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        ItemStack nugget = entity.nuggetStack();
        if (nugget.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, Math.max(0.22D, entity.getBbHeight() * 0.28D), 0.0D);
        float scale = Math.max(0.24F, Math.min(0.48F, entity.getBbWidth() * 0.33F));
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                nugget, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
