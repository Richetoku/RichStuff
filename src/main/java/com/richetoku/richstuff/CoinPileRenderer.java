package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders one through nine coins directly on top of one another with no visual gaps. */
public final class CoinPileRenderer implements BlockEntityRenderer<CoinPileBlockEntity> {
    private static final float COIN_DIAMETER_SCALE = 1.00F;
    // Generated item models already extrude their sprite edges. Stretching the model's depth makes
    // those textured edges the exact visual thickness used by the collision shape.
    private static final float COIN_EDGE_THICKNESS_SCALE = 3.00F;
    private static final double NORMAL_COIN_HEIGHT = CoinPileBlock.COIN_HEIGHT_PIXELS / 16.0D;

    public CoinPileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CoinPileBlockEntity pile, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stored = pile.coin();
        if (stored.isEmpty()) return;
        ItemStack coin = stored.copyWithCount(1);

        int count = pile.count();
        // The ninth coin completes a compact Coin Stack. It still renders as nine coins, but the
        // finished stack occupies the complete block height so its top surface aligns with the
        // next block space. Partial piles retain the original 1.5-pixel coin thickness.
        double coinHeight = count >= CoinPileBlockEntity.MAX_COINS
                ? 1.0D / CoinPileBlockEntity.MAX_COINS
                : NORMAL_COIN_HEIGHT;
        float edgeScale = count >= CoinPileBlockEntity.MAX_COINS
                ? (float) (COIN_EDGE_THICKNESS_SCALE * (coinHeight / NORMAL_COIN_HEIGHT))
                : COIN_EDGE_THICKNESS_SCALE;

        for (int index = 0; index < count; index++) {
            poseStack.pushPose();
            poseStack.translate(0.5D, (index + 0.5D) * coinHeight, 0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(COIN_DIAMETER_SCALE, COIN_DIAMETER_SCALE, edgeScale);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    coin, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, pile.getLevel(), index);
            poseStack.popPose();
        }
    }
}
