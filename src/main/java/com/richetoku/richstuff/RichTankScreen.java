package com.richetoku.richstuff;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Lightweight tank status screen backed only by synchronized menu integers. */
public final class RichTankScreen extends AbstractContainerScreen<RichTankMenu> {
    public RichTankScreen(RichTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RichTankMenu.WIDTH;
        imageHeight = RichTankMenu.HEIGHT;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelY = 1000; // This read-only status screen intentionally has no inventory slots.
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17191C);
        graphics.fill(leftPos + 10, topPos + 24, leftPos + 42, topPos + 106, 0xFF090A0C);
        graphics.renderOutline(leftPos + 9, topPos + 23, 34, 84, 0xFF7F8994);
        int fill = Math.round(80.0F * Math.min(1.0F, menu.amount() / (float) menu.capacity()));
        if (fill > 0) graphics.fill(leftPos + 12, topPos + 104 - fill, leftPos + 40, topPos + 104, 0xFF2989B9);
        graphics.fill(leftPos + 50, topPos + 24, leftPos + 166, topPos + 106, 0xFF252A30);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        Fluid fluid = menu.fluidRegistryId() < 0 ? null : BuiltInRegistries.FLUID.byId(menu.fluidRegistryId());
        Component contents = fluid == null || fluid == Fluids.EMPTY
                ? Component.translatable("gui.richstuff.rich_tank.empty")
                : fluid.getFluidType().getDescription();
        graphics.drawString(font, contents, 50, 29, 0xE8F4FF, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rich_tank.contents",
                formatFluid(menu.amount()), formatFluid(menu.capacity())), 50, 45, 0xD5D9DE, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rich_tank.structure",
                menu.width(), menu.depth(), menu.height()), 50, 62, 0xD5D9DE, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rich_tank.blocks", menu.tankCount()),
                50, 79, 0xD5D9DE, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rich_tank.layer_hint"),
                50, 95, 0x9DA6AF, false);
    }


    private static String formatFluid(int amount) {
        if (amount < 1000) return amount + " mB";
        if (amount % 1000 == 0) return (amount / 1000) + " B";
        if (amount < 1_000_000) return String.format(java.util.Locale.ROOT, "%.2f B", amount / 1000.0D);
        return String.format(java.util.Locale.ROOT, "%.2f kB", amount / 1_000_000.0D);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
