package com.richetoku.richstuff;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Seven-tier material progression shared by recipes and RPM machines. */
public final class RichStuffProgression {
    private static final Map<String, Integer> MATERIAL_TIERS = load();
    private static final int[] MIN_RPM = {0, 16, 32, 64, 128, 256, 512, 1024};

    private RichStuffProgression() {}

    public static int tierForMaterial(String material) {
        return MATERIAL_TIERS.getOrDefault(material, 1);
    }

    public static int tierFor(ItemStack stack) {
        if (stack.isEmpty()) return 1;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 1;
        String path = id.getPath();
        int bestTier = 1;
        int bestLength = -1;
        for (Map.Entry<String, Integer> entry : MATERIAL_TIERS.entrySet()) {
            String material = entry.getKey();
            if (path.equals(material) || path.startsWith(material + "_") || path.endsWith("_" + material)
                    || path.contains("_" + material + "_")) {
                if (material.length() > bestLength) {
                    bestLength = material.length();
                    bestTier = entry.getValue();
                }
            }
        }
        return bestTier;
    }

    public static int minimumRpm(int tier) {
        return MIN_RPM[Math.max(1, Math.min(7, tier))];
    }

    private static Map<String, Integer> load() {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (var input = RichStuffProgression.class.getResourceAsStream("/data/richstuff/progression/tiers.json")) {
            if (input == null) return result;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject tiers = root.getAsJsonObject("tiers");
            for (Map.Entry<String, JsonElement> tierEntry : tiers.entrySet()) {
                int tier = Integer.parseInt(tierEntry.getKey());
                for (JsonElement material : tierEntry.getValue().getAsJsonObject().getAsJsonArray("materials")) {
                    result.put(material.getAsString(), tier);
                }
            }
        } catch (Exception error) {
            RichStuff.LOGGER.error("Failed to load seven-tier progression. Tier 1 fallback will be used.", error);
        }
        return Map.copyOf(result);
    }
}
