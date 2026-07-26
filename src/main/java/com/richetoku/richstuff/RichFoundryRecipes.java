package com.richetoku.richstuff;

import com.richetoku.richcore.RichContentPartition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Datapack recipes for melting, non-destructive molten storage, alloying, pouring and casting. */
public final class RichFoundryRecipes {
    public enum Kind { MELTING, ALLOY, CASTING }

    public record IngredientRef(@Nullable ResourceLocation item, @Nullable ResourceLocation tag) {
        public boolean test(ItemStack stack) {
            if (stack.isEmpty()) return false;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (item != null) return item.equals(key);
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
    }
    public record FluidAmount(ResourceLocation fluid, int amount) {}
    public record Melting(ResourceLocation source, IngredientRef input, ResourceLocation fluid, int amount, int ticks) {}
    public record Alloy(ResourceLocation source, List<FluidAmount> inputs, ResourceLocation fluid, int amount, int ticks) {}
    public record Casting(ResourceLocation source, ResourceLocation fluid, int amount,
                          @Nullable IngredientRef mold, ResourceLocation result, int count, boolean basin, int ticks) {}

    private static volatile List<Melting> MELTING = List.of();
    private static volatile List<Alloy> ALLOYS = List.of();
    private static volatile List<Casting> CASTING = List.of();

    private RichFoundryRecipes() {}

    public static void apply(Map<ResourceLocation, JsonElement> resources) {
        List<Melting> melting = new ArrayList<>();
        List<Alloy> alloys = new ArrayList<>();
        List<Casting> casting = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                Kind kind = Kind.valueOf(required(json, "type").toUpperCase(Locale.ROOT));
                switch (kind) {
                    case MELTING -> melting.add(parseMelting(entry.getKey(), json));
                    case ALLOY -> alloys.add(parseAlloy(entry.getKey(), json));
                    case CASTING -> casting.add(parseCasting(entry.getKey(), json));
                }
            } catch (RuntimeException exception) {
                RichStuff.LOGGER.error("Ignoring invalid Rich Foundry recipe {}: {}", entry.getKey(), exception.getMessage());
            }
        }
        MELTING = List.copyOf(melting);
        ALLOYS = List.copyOf(alloys);
        CASTING = List.copyOf(casting);
        RichStuff.LOGGER.info("Loaded {} melting, {} alloying, and {} casting Rich Foundry recipes.",
                melting.size(), alloys.size(), casting.size());
    }

    private static Melting parseMelting(ResourceLocation source, JsonObject json) {
        return new Melting(source, ingredient(json.getAsJsonObject("input")), location(json, "fluid"),
                positive(json, "amount", 144), positive(json, "ticks", 120));
    }

    private static Alloy parseAlloy(ResourceLocation source, JsonObject json) {
        JsonArray array = json.getAsJsonArray("inputs");
        if (array == null || array.size() == 0) throw new IllegalArgumentException("alloy requires inputs");
        List<FluidAmount> inputs = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject input = element.getAsJsonObject();
            inputs.add(new FluidAmount(location(input, "fluid"), positive(input, "amount", 144)));
        }
        return new Alloy(source, List.copyOf(inputs), location(json, "fluid"),
                positive(json, "amount", inputs.stream().mapToInt(FluidAmount::amount).sum()),
                positive(json, "ticks", 80));
    }

    private static Casting parseCasting(ResourceLocation source, JsonObject json) {
        IngredientRef mold = json.has("mold") ? ingredient(json.getAsJsonObject("mold")) : null;
        return new Casting(source, location(json, "fluid"), positive(json, "amount", 144), mold,
                location(json, "result"), positive(json, "count", 1),
                json.has("basin") && json.get("basin").getAsBoolean(), positive(json, "ticks", 80));
    }

    private static IngredientRef ingredient(JsonObject json) {
        if (json == null) throw new IllegalArgumentException("missing ingredient");
        if (json.has("item")) return new IngredientRef(ResourceLocation.parse(json.get("item").getAsString()), null);
        if (json.has("tag")) return new IngredientRef(null, ResourceLocation.parse(json.get("tag").getAsString()));
        throw new IllegalArgumentException("ingredient requires item or tag");
    }

    @Nullable public static Melting findMelting(ItemStack stack) {
        for (Melting recipe : MELTING) if (recipe.input().test(stack)) return recipe;
        return fallbackMelting(stack);
    }

    @Nullable public static Alloy findAlloy(Map<ResourceLocation, Integer> tank) {
        for (Alloy recipe : ALLOYS) {
            boolean matches = true;
            for (FluidAmount input : recipe.inputs()) {
                if (tank.getOrDefault(input.fluid(), 0) < input.amount()) { matches = false; break; }
            }
            if (matches) return recipe;
        }
        return null;
    }

    @Nullable public static Casting findCasting(ResourceLocation fluid, ItemStack mold, boolean basin) {
        for (Casting recipe : CASTING) {
            if (!recipe.fluid().equals(fluid) || recipe.basin() != basin) continue;
            if (basin || recipe.mold() == null || recipe.mold().test(mold)) return recipe;
        }
        return fallbackCasting(fluid, mold, basin);
    }

    @Nullable private static Melting fallbackMelting(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return null;
        String material = RichContentPartition.materialForItemId(itemId.getPath());
        if (material.isEmpty()) material = vanillaMaterial(itemId);
        if (material.isEmpty()) return null;
        ResourceLocation fluid = ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "molten_" + material);
        if (!BuiltInRegistries.FLUID.containsKey(fluid)) return null;
        int amount = amountForForm(itemId.getPath());
        return new Melting(ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "fallback/" + itemId.getPath()),
                new IngredientRef(itemId, null), fluid, amount, 100 + Math.max(0, amount / 9));
    }

    @Nullable private static Casting fallbackCasting(ResourceLocation fluid, ItemStack mold, boolean basin) {
        if (!fluid.getNamespace().equals(RichStuff.MODID) || !fluid.getPath().startsWith("molten_")) return null;
        String material = fluid.getPath().substring("molten_".length());
        String form;
        int amount;
        if (basin) {
            form = "block";
            amount = 1296;
        } else {
            ResourceLocation moldId = BuiltInRegistries.ITEM.getKey(mold.getItem());
            if (moldId == null || mold.getItem() == Items.AIR) return null;
            Form selected = formForMold(moldId.getPath());
            if (selected == null) return null;
            form = selected.form();
            amount = selected.amount();
        }
        ResourceLocation resultId = resultFor(material, form);
        Item result = BuiltInRegistries.ITEM.get(resultId);
        if (result == null || result == Items.AIR) return null;
        return new Casting(ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "fallback_casting/" + material + "/" + form),
                fluid, amount, basin ? null : new IngredientRef(BuiltInRegistries.ITEM.getKey(mold.getItem()), null),
                resultId, 1, basin, Math.max(30, amount / 3));
    }

    private static ResourceLocation resultFor(String material, String form) {
        String id = form.equals("material") ? material : material + "_" + form;
        ResourceLocation rich = ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, id);
        if (BuiltInRegistries.ITEM.get(rich) != Items.AIR) return rich;
        if (material.equals("iron") || material.equals("gold") || material.equals("copper") || material.equals("diamond")
                || material.equals("emerald") || material.equals("quartz") || material.equals("coal") || material.equals("redstone")) {
            ResourceLocation vanilla = ResourceLocation.withDefaultNamespace(id.replace("quartz_material", "quartz"));
            if (BuiltInRegistries.ITEM.get(vanilla) != Items.AIR) return vanilla;
        }
        return rich;
    }

    @Nullable private static Form formForMold(String id) {
        String path = id.toLowerCase(Locale.ROOT);
        if (!path.contains("mold")) return null;
        String[][] forms = {
                {"nuggets_mold", "nugget", "144"}, {"nugget_mold", "nugget", "16"},
                {"ingot_mold", "ingot", "144"}, {"block_mold", "block", "1296"},
                {"brick_mold", "brick", "144"}, {"plate_mold", "plate", "144"},
                {"rod_mold", "rod", "72"}, {"wire_mold", "wire", "48"},
                {"ring_mold", "ring", "72"}, {"coin_mold", "coin", "16"},
                {"gear_mold", "gear", "576"}, {"blisk_mold", "blisk", "576"},
                {"shards_mold", "shard", "144"}, {"shard_mold", "shard", "72"},
                {"gem_mold", "material", "144"}
        };
        for (String[] value : forms) if (path.endsWith(value[0]) || path.contains("_" + value[0])) {
            return new Form(value[1], Integer.parseInt(value[2]));
        }
        return null;
    }

    private static int amountForForm(String path) {
        if (path.endsWith("_block")) return 1296;
        if (path.endsWith("_gear") || path.endsWith("_blisk")) return 576;
        if (path.endsWith("_nugget") || path.endsWith("_coin")) return 16;
        if (path.endsWith("_rod") || path.endsWith("_ring") || path.endsWith("_shard")) return 72;
        if (path.endsWith("_wire")) return 48;
        return 144;
    }

    private static String vanillaMaterial(ResourceLocation item) {
        if (!item.getNamespace().equals("minecraft")) return "";
        String path = item.getPath();
        for (String material : List.of("iron", "gold", "copper", "diamond", "emerald", "coal", "redstone", "quartz")) {
            if (path.equals(material) || path.startsWith(material + "_") || path.endsWith("_" + material)) return material;
        }
        return "";
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("missing " + key);
        return json.get(key).getAsString();
    }
    private static ResourceLocation location(JsonObject json, String key) { return ResourceLocation.parse(required(json, key)); }
    private static int positive(JsonObject json, String key, int fallback) {
        return Math.max(1, json.has(key) ? json.get(key).getAsInt() : fallback);
    }

    private record Form(String form, int amount) {}
}
