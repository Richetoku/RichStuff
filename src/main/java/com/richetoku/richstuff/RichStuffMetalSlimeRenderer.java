package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

/** Translucent material-colored shell with a separately rendered 3D nugget core. */
public final class RichStuffMetalSlimeRenderer extends MobRenderer<RichStuffMetalSlime, SlimeModel<RichStuffMetalSlime>> {
    public RichStuffMetalSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(RichStuffMetalSlime entity) {
        return ResourceLocation.fromNamespaceAndPath(RichStuff.MODID,
                "textures/entity/slime/" + entity.materialId() + "_slime.png");
    }

    @Nullable
    @Override
    protected RenderType getRenderType(RichStuffMetalSlime entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    protected void scale(RichStuffMetalSlime entity, PoseStack poseStack, float partialTick) {
        // SlimeModel uses vanilla's unit slime mesh. Apply the runtime slime size and the smooth
        // tier multiplier so the visible shell follows the entity dimensions instead of remaining tiny.
        float scale = entity.shellRenderScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void render(RichStuffMetalSlime entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        shadowRadius = Math.max(0.22F, entity.getBbWidth() * 0.42F);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        ItemStack nugget = entity.nuggetStack();
        if (nugget.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.50D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 2.2F));
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        float nuggetScale = Math.max(0.18F, Math.min(0.72F, entity.getBbWidth() * 0.15F));
        poseStack.scale(nuggetScale, nuggetScale, nuggetScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                nugget, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
