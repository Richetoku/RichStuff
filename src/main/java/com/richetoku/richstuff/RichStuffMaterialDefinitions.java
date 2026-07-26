package com.richetoku.richstuff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime view of data-pack material-family definitions.
 *
 * <p>Minecraft item and block registries are frozen before server data packs reload, so the
 * bundled catalog remains a pre-registered superset. These JSON files control which registered
 * material families and forms are active for data-driven integrations. The build-time generator
 * uses the same files to add or remove registry resources safely.</p>
 */
public final class RichStuffMaterialDefinitions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicReference<Map<String, MaterialDefinition>> SNAPSHOT =
            new AtomicReference<>(Map.of());

    private RichStuffMaterialDefinitions() {}

    public static void apply(Map<ResourceLocation, JsonElement> resources) {
        Map<String, MaterialDefinition> definitions = new LinkedHashMap<>();
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    try {
                        String inferred = materialFromResource(entry.getKey());
                        MaterialDefinition definition;
                        if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().size() == 0) {
                            definition = new MaterialDefinition();
                            definition.material = inferred;
                            definition.enabled = false;
                        } else {
                            definition = GSON.fromJson(entry.getValue(), MaterialDefinition.class);
                            if (definition == null) return;
                            if (definition.material == null || definition.material.isBlank()) definition.material = inferred;
                        }
                        definition.normalize();
                        if (!definition.material.isBlank()) definitions.put(definition.material, definition);
                    } catch (RuntimeException error) {
                        RichStuff.LOGGER.error("Invalid RichStuff material definition {}.", entry.getKey(), error);
                    }
                });
        SNAPSHOT.set(Map.copyOf(definitions));
        RichStuff.LOGGER.info("Loaded {} RichStuff data-pack material definitions.", definitions.size());
    }

    public static Map<String, MaterialDefinition> snapshot() {
        return SNAPSHOT.get();
    }

    public static boolean isMaterialEnabled(String material) {
        MaterialDefinition definition = SNAPSHOT.get().get(normalize(material));
        return definition == null || definition.enabled;
    }

    public static boolean isFormEnabled(String material, String formId) {
        MaterialDefinition definition = SNAPSHOT.get().get(normalize(material));
        return definition == null || definition.allowsForm(formId);
    }

    /** Returns whether a registered item/block form remains enabled by the active material JSON. */
    public static boolean isRegisteredFormEnabled(String formId) {
        String normalizedForm = normalize(formId);
        return SNAPSHOT.get().values().stream()
                .sorted((left, right) -> Integer.compare(right.material.length(), left.material.length()))
                .filter(definition -> definition.ownsForm(normalizedForm))
                .findFirst()
                .map(definition -> definition.allowsForm(normalizedForm))
                .orElse(true);
    }

    private static String materialFromResource(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return normalize(slash >= 0 ? path.substring(slash + 1) : path);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }

    public static final class MaterialDefinition {
        public int schema = 1;
        public String material = "";
        public String category = "utility";
        public boolean enabled = true;
        public List<String> forms = new ArrayList<>();
        public List<String> skipForms = new ArrayList<>();
        public boolean crystalGrowthOnly = false;

        void normalize() {
            material = RichStuffMaterialDefinitions.normalize(material);
            category = RichStuffMaterialDefinitions.normalize(category);
            if (category.isBlank()) category = "utility";
            forms = normalizeList(forms);
            skipForms = normalizeList(skipForms);
        }

        public boolean allowsForm(String formId) {
            if (!enabled) return false;
            String normalized = RichStuffMaterialDefinitions.normalize(formId);
            if (matches(skipForms, normalized)) return false;
            return forms.isEmpty() || matches(forms, normalized);
        }

        boolean ownsForm(String formId) {
            if (matches(forms, formId)) return true;
            if (material.isBlank()) return false;
            return ("_" + formId + "_").contains("_" + material + "_");
        }

        private static List<String> normalizeList(List<String> values) {
            if (values == null) return List.of();
            return values.stream().filter(value -> value != null && !value.isBlank())
                    .map(RichStuffMaterialDefinitions::normalize).toList();
        }

        private static boolean matches(List<String> patterns, String value) {
            for (String pattern : patterns) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
                if (value.matches(regex)) return true;
            }
            return false;
        }
    }
}
