package com.richetoku.richstuff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class RichFoundryRecipeReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public RichFoundryRecipeReloadListener() { super(GSON, "rich_foundry"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                                   ProfilerFiller profiler) {
        RichFoundryRecipes.apply(resources);
    }
}
