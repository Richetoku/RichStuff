package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Amount-aware rendering of the fluid's actual registered still/flow texture inside the tank frame. */
public final class RichTankRenderer implements BlockEntityRenderer<RichTankBlockEntity> {
    public RichTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(RichTankBlockEntity tank, float partialTick, PoseStack pose,
                                 MultiBufferSource buffer, int light, int overlay) {
        FluidStack fluid = tank.visualFluid();
        int amount = tank.visualAmount();
        if (fluid.isEmpty() || amount <= 0) return;
        BlockState visual = fluid.getFluid().defaultFluidState().createLegacyBlock();
        if (visual.isAir()) return;
        float ratio = Math.min(1.0F, amount / (float)Math.max(1, tank.capacity()));
        // Any non-empty tank shows at least one model pixel of fluid.
        ratio = Math.max(1.0F / 16.0F, ratio);
        BlockState tankState = tank.getBlockState();
        boolean west = tankState.hasProperty(RichTankBlock.WEST) && tankState.getValue(RichTankBlock.WEST);
        boolean east = tankState.hasProperty(RichTankBlock.EAST) && tankState.getValue(RichTankBlock.EAST);
        boolean north = tankState.hasProperty(RichTankBlock.NORTH) && tankState.getValue(RichTankBlock.NORTH);
        boolean south = tankState.hasProperty(RichTankBlock.SOUTH) && tankState.getValue(RichTankBlock.SOUTH);
        boolean down = tankState.hasProperty(RichTankBlock.DOWN) && tankState.getValue(RichTankBlock.DOWN);
        boolean up = tankState.hasProperty(RichTankBlock.UP) && tankState.getValue(RichTankBlock.UP);
        double minX = west ? 0.0D : 0.126D;
        double maxX = east ? 1.0D : 0.874D;
        double minZ = north ? 0.0D : 0.126D;
        double maxZ = south ? 1.0D : 0.874D;
        double minY = down ? 0.0D : 0.002D;
        double maxY = ratio >= 0.999F && up ? 1.0D : Math.min(0.998D, minY + (1.0D - minY) * ratio);
        pose.pushPose();
        // Valid connected faces extend to the block boundary, producing one continuous tank volume.
        pose.translate(minX, minY, minZ);
        pose.scale((float)(maxX - minX), Math.max(1.0F / 16.0F, (float)(maxY - minY)), (float)(maxZ - minZ));
        int fluidLight = fluid.getFluidType().getLightLevel(fluid) << 4;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(visual, pose, buffer,
                Math.max(light, fluidLight), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
