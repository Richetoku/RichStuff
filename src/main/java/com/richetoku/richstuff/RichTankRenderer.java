package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws the live fluid atlas texture as a connected, amount-aware volume inside Rich Tanks. */
public final class RichTankRenderer implements BlockEntityRenderer<RichTankBlockEntity> {
    public RichTankRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(RichTankBlockEntity tank, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FluidStack fluid = tank.visualFluid();
        int amount = tank.visualAmount();
        if (fluid.isEmpty() || amount <= 0) return;

        IClientFluidTypeExtensions visual = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = visual.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);

        float ratio = Math.min(1.0F, amount / (float) Math.max(1, tank.capacity()));
        ratio = Math.max(1.0F / 16.0F, ratio);
        BlockState state = tank.getBlockState();
        boolean west = state.hasProperty(RichTankBlock.WEST) && state.getValue(RichTankBlock.WEST);
        boolean east = state.hasProperty(RichTankBlock.EAST) && state.getValue(RichTankBlock.EAST);
        boolean north = state.hasProperty(RichTankBlock.NORTH) && state.getValue(RichTankBlock.NORTH);
        boolean south = state.hasProperty(RichTankBlock.SOUTH) && state.getValue(RichTankBlock.SOUTH);
        boolean down = state.hasProperty(RichTankBlock.DOWN) && state.getValue(RichTankBlock.DOWN);
        boolean up = state.hasProperty(RichTankBlock.UP) && state.getValue(RichTankBlock.UP);

        float minX = west ? 0.001F : 0.127F;
        float maxX = east ? 0.999F : 0.873F;
        float minZ = north ? 0.001F : 0.127F;
        float maxZ = south ? 0.999F : 0.873F;
        float minY = down ? 0.001F : 0.018F;
        float maxY = ratio >= 0.999F && up ? 0.999F : Math.min(0.982F, minY + (0.964F - minY) * ratio);

        int tint = visual.getTintColor(fluid);
        int alpha = tint >>> 24 & 0xFF;
        if (alpha == 0) alpha = 220;
        int red = tint >>> 16 & 0xFF;
        int green = tint >>> 8 & 0xFF;
        int blue = tint & 0xFF;
        int fluidLight = Math.max(packedLight, fluid.getFluidType().getLightLevel(fluid) << 4);

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Render the fluid shell double-sided so the bottom surface remains visible when viewed from above
        // inside the tank and the top surface stays visible when the camera clips slightly into the volume.
        if (!up || ratio < 0.999F) doubleSidedQuad(consumer, pose,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                u0, v0, u1, v1, red, green, blue, alpha, fluidLight, 0, 1, 0);
        if (!down) doubleSidedQuad(consumer, pose,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                u0, v0, u1, v1, red, green, blue, alpha, fluidLight, 0, -1, 0);

        if (!north) doubleSidedQuad(consumer, pose,
                maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ,
                u0, v1, u1, v0, red, green, blue, alpha, fluidLight, 0, 0, -1);
        if (!south) doubleSidedQuad(consumer, pose,
                minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ,
                u0, v1, u1, v0, red, green, blue, alpha, fluidLight, 0, 0, 1);
        if (!west) doubleSidedQuad(consumer, pose,
                minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ,
                u0, v1, u1, v0, red, green, blue, alpha, fluidLight, -1, 0, 0);
        if (!east) doubleSidedQuad(consumer, pose,
                maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ,
                u0, v1, u1, v0, red, green, blue, alpha, fluidLight, 1, 0, 0);
    }

    private static void doubleSidedQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                        float x1, float y1, float z1, float x2, float y2, float z2,
                                        float x3, float y3, float z3, float x4, float y4, float z4,
                                        float u0, float v0, float u1, float v1,
                                        int red, int green, int blue, int alpha, int light,
                                        float normalX, float normalY, float normalZ) {
        quad(consumer, pose, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                u0, v0, u1, v1, red, green, blue, alpha, light, normalX, normalY, normalZ);
        quad(consumer, pose, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1,
                u0, v0, u1, v1, red, green, blue, alpha, light, -normalX, -normalY, -normalZ);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float v0, float u1, float v1,
                             int red, int green, int blue, int alpha, int light,
                             float normalX, float normalY, float normalZ) {
        vertex(consumer, pose, x1, y1, z1, u0, v1, red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x2, y2, z2, u1, v1, red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x3, y3, z3, u1, v0, red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x4, y4, z4, u0, v0, red, green, blue, alpha, light, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int red, int green, int blue, int alpha, int light,
                               float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override public int getViewDistance() { return 128; }
    @Override public boolean shouldRenderOffScreen(RichTankBlockEntity tank) { return true; }
}
