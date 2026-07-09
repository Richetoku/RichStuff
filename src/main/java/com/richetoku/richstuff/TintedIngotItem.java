package com.richetoku.richstuff;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TintedIngotItem extends Item {
    private final int tintColor;

    public TintedIngotItem(Properties properties, int tintColor) {
        super(properties);
        this.tintColor = tintColor;
    }

    public int getTintColor() {
        return tintColor;
    }

    public static void registerItemProperties() {
        // Register custom item properties for tinted ingots
        // This will be called from client setup
    }
}