package com.richetoku.richstuff;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/** Adds a persistent red frame and cross to the inventory icon of retained broken Rich Gear. */
public final class RichGearItemDecorator implements IItemDecorator {
    @Override public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!RichGearData.isBroken(stack)) return false;
        int red = 0xFFE02B2B;
        graphics.renderOutline(xOffset, yOffset, 16, 16, red);
        for (int i = 3; i <= 12; i++) {
            graphics.fill(xOffset + i, yOffset + i, xOffset + i + 1, yOffset + i + 1, red);
            graphics.fill(xOffset + 15 - i, yOffset + i, xOffset + 16 - i, yOffset + i + 1, red);
        }
        return true;
    }
}
