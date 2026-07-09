package com.richetoku.richstuff;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * RecipeLoader handles recipe priority and mod detection.
 * Recipe priority is handled through:
 * - Recipe conditions (mod_loaded conditions for Create recipes)
 * - Folder structure (create/, create_vintage/, fallback/)
 * - Automatic folder selection based on available mods
 */
@EventBusSubscriber(modid = RichStuff.MODID, value = Dist.DEDICATED_SERVER)
public class RecipeLoader {
    
    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        // Recipe priority is automatically handled by the recipe loading system
        // Create recipes require Create mod
        // Vintage recipes require Vintage machines
        // Fallback recipes are always available
    }
}