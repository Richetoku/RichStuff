package com.richetoku.richstuff;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RichBarrelScreen extends AbstractContainerScreen<RichBarrelMenu> {
    public RichBarrelScreen(RichBarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RichBarrelMenu.WIDTH;
        imageHeight = RichBarrelMenu.HEIGHT;
        inventoryLabelX = 8;
        inventoryLabelY = 146;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF282A2D);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF4B3B2D);
        slots(g, 17, 20, RichBarrelMenu.BARREL_COLUMNS, RichBarrelMenu.BARREL_ROWS);
        slots(g, 8, 158, 9, 3);
        slots(g, 8, 216, 9, 1);
    }

    private void slots(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int sx = leftPos + x + c * 18, sy = topPos + y + r * 18;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF17191C);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF6B5847);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0xFFFFFFFF, false);
        g.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, 0xFFE5D8C9, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderUniformCounts(g);
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderUniformCounts(GuiGraphics g) {
        final float scale = 0.40F;
        for (int slotIndex = 0; slotIndex < RichBarrelBlockEntity.SLOTS && slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || stack.getCount() <= 1) continue;
            String count = Integer.toString(stack.getCount());
            int slotRight = leftPos + slot.x + 17;
            int slotBottom = topPos + slot.y + 18;
            int scaledWidth = Math.max(1, Math.round(font.width(count) * scale));
            int textLeft = slotRight - scaledWidth - 1;
            int textTop = slotBottom - 5;
            // Cover the normal-size vanilla count before drawing every barrel count at one fixed size.
            g.fill(leftPos + slot.x + 6, topPos + slot.y + 10, slotRight, slotBottom, 0xB0101010);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(0.0F, 0.0F, 250.0F);
            pose.scale(scale, scale, 1.0F);
            g.drawString(font, count, Math.round(textLeft / scale), Math.round(textTop / scale), 0xFFFFFFFF, true);
            pose.popPose();
        }
    }
}
