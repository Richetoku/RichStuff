package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.richetoku.richcore.api.RichFluidItemHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

/** Three-dimensional universal jar with a live registered-fluid texture inside its glass shell. */
public final class RichJarItemRenderer extends BlockEntityWithoutLevelRenderer {
    public RichJarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override public void onResourceManagerReload(@NotNull ResourceManager manager) { }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context,
                             @NotNull PoseStack pose, @NotNull MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        float scale = switch (context) {
            case GUI -> 0.86F;
            case GROUND -> 0.56F;
            case FIXED -> 0.78F;
            default -> 0.72F;
        };
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5D, -0.5D, -0.5D);

        FluidStack fluid = RichFluidItemHandler.getFluid(stack);
        if (!fluid.isEmpty()) renderFluid(fluid, pose, buffer, packedLight);

        // A wide body, shoulder and narrow neck make the inventory item read as a real jar rather
        // than a flat icon. The shell renders after the fluid so the liquid remains behind glass.
        glassPiece(pose, buffer, packedLight, 0.20F, 0.11F, 0.20F, 0.60F, 0.06F, 0.60F);
        glassPiece(pose, buffer, packedLight, 0.18F, 0.15F, 0.18F, 0.06F, 0.58F, 0.64F);
        glassPiece(pose, buffer, packedLight, 0.76F, 0.15F, 0.18F, 0.06F, 0.58F, 0.64F);
        glassPiece(pose, buffer, packedLight, 0.24F, 0.15F, 0.18F, 0.52F, 0.58F, 0.06F);
        glassPiece(pose, buffer, packedLight, 0.24F, 0.15F, 0.76F, 0.52F, 0.58F, 0.06F);
        glassPiece(pose, buffer, packedLight, 0.24F, 0.69F, 0.24F, 0.52F, 0.06F, 0.52F);
        glassPiece(pose, buffer, packedLight, 0.34F, 0.73F, 0.34F, 0.06F, 0.17F, 0.32F);
        glassPiece(pose, buffer, packedLight, 0.60F, 0.73F, 0.34F, 0.06F, 0.17F, 0.32F);
        glassPiece(pose, buffer, packedLight, 0.40F, 0.73F, 0.34F, 0.20F, 0.17F, 0.06F);
        glassPiece(pose, buffer, packedLight, 0.40F, 0.73F, 0.60F, 0.20F, 0.17F, 0.06F);
        metalPiece(pose, buffer, packedLight, 0.33F, 0.89F, 0.33F, 0.34F, 0.07F, 0.34F);
        pose.popPose();
    }

    private static void glassPiece(PoseStack pose, MultiBufferSource buffer, int light,
                                   float x, float y, float z, float sx, float sy, float sz) {
        renderPiece(Blocks.GLASS.defaultBlockState(), pose, buffer, light, x, y, z, sx, sy, sz);
    }

    private static void metalPiece(PoseStack pose, MultiBufferSource buffer, int light,
                                   float x, float y, float z, float sx, float sy, float sz) {
        renderPiece(Blocks.IRON_BLOCK.defaultBlockState(), pose, buffer, light, x, y, z, sx, sy, sz);
    }

    private static void renderPiece(net.minecraft.world.level.block.state.BlockState state,
                                    PoseStack pose, MultiBufferSource buffer, int light,
                                    float x, float y, float z, float sx, float sy, float sz) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(sx, sy, sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, pose, buffer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void renderFluid(FluidStack fluid, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        IClientFluidTypeExtensions visual = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = visual.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int tint = visual.getTintColor(fluid);
        int alpha = tint >>> 24 & 255;
        if (alpha == 0) alpha = 225;
        int red = tint >>> 16 & 255;
        int green = tint >>> 8 & 255;
        int blue = tint & 255;
        int light = Math.max(packedLight, fluid.getFluidType().getLightLevel(fluid) << 4);
        float ratio = Math.max(1.0F / 16.0F, Math.min(1.0F, fluid.getAmount() / (float) RichJarItem.CAPACITY));
        float minX = 0.245F, maxX = 0.755F, minZ = 0.245F, maxZ = 0.755F;
        float minY = 0.18F, maxY = minY + 0.48F * ratio;
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        PoseStack.Pose current = pose.last();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        quad2(consumer, current, minX,maxY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, minX,maxY,maxZ,
                u0,v0,u1,v1, red,green,blue,alpha,light, 0,1,0);
        quad2(consumer, current, minX,minY,maxZ, maxX,minY,maxZ, maxX,minY,minZ, minX,minY,minZ,
                u0,v0,u1,v1, red,green,blue,alpha,light, 0,-1,0);
        quad2(consumer, current, maxX,minY,minZ, maxX,maxY,minZ, minX,maxY,minZ, minX,minY,minZ,
                u0,v1,u1,v0, red,green,blue,alpha,light, 0,0,-1);
        quad2(consumer, current, minX,minY,maxZ, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,minY,maxZ,
                u0,v1,u1,v0, red,green,blue,alpha,light, 0,0,1);
        quad2(consumer, current, minX,minY,minZ, minX,maxY,minZ, minX,maxY,maxZ, minX,minY,maxZ,
                u0,v1,u1,v0, red,green,blue,alpha,light, -1,0,0);
        quad2(consumer, current, maxX,minY,maxZ, maxX,maxY,maxZ, maxX,maxY,minZ, maxX,minY,minZ,
                u0,v1,u1,v0, red,green,blue,alpha,light, 1,0,0);
    }

    private static void quad2(VertexConsumer out, PoseStack.Pose pose,
                              float x1,float y1,float z1,float x2,float y2,float z2,
                              float x3,float y3,float z3,float x4,float y4,float z4,
                              float u0,float v0,float u1,float v1,
                              int r,int g,int b,int a,int light,float nx,float ny,float nz) {
        quad(out,pose,x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,u0,v0,u1,v1,r,g,b,a,light,nx,ny,nz);
        quad(out,pose,x4,y4,z4,x3,y3,z3,x2,y2,z2,x1,y1,z1,u0,v0,u1,v1,r,g,b,a,light,-nx,-ny,-nz);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             float u0,float v0,float u1,float v1,
                             int r,int g,int b,int a,int light,float nx,float ny,float nz) {
        vertex(out,pose,x1,y1,z1,u0,v1,r,g,b,a,light,nx,ny,nz);
        vertex(out,pose,x2,y2,z2,u1,v1,r,g,b,a,light,nx,ny,nz);
        vertex(out,pose,x3,y3,z3,u1,v0,r,g,b,a,light,nx,ny,nz);
        vertex(out,pose,x4,y4,z4,u0,v0,r,g,b,a,light,nx,ny,nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x,float y,float z,float u,float v,
                               int r,int g,int b,int a,int light,float nx,float ny,float nz) {
        out.addVertex(pose.pose(),x,y,z).setColor(r,g,b,a).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
