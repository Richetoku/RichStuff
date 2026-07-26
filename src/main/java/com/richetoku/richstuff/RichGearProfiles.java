package com.richetoku.richstuff;

import com.richetoku.richcore.MaterialDef;
import com.richetoku.richcore.RichStuffCatalog;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Native material profile registry for modular Rich Gear equipment. */
public final class RichGearProfiles {
    private static final Map<String, RichGearProfile> PROFILES = new LinkedHashMap<>();
    static {
        for (MaterialDef material : RichStuffCatalog.MATERIALS) PROFILES.put(material.name(), create(material));
    }
    private RichGearProfiles() {}
    public static RichGearProfile get(String material) { return PROFILES.getOrDefault(material, unbound()); }
    public static boolean contains(String material) { return PROFILES.containsKey(material); }
    public static Map<String,RichGearProfile> all() { return Map.copyOf(PROFILES); }
    public static RichGearProfile unbound() { return new RichGearProfile("unbound","material",1,"unbound","adaptable",1,0,0,1,0); }

    private static RichGearProfile create(MaterialDef material) {
        int hash = Math.abs(material.name().hashCode());
        float tier = Math.max(1, material.tier());
        float spreadA = ((hash % 11) - 5) / 25.0F;
        float spreadB = (((hash / 11) % 11) - 5) / 30.0F;
        String family = switch (material.kind().toLowerCase(Locale.ROOT)) {
            case "metal" -> "tempered";
            case "alloy" -> "synergistic";
            case "gem" -> "precise";
            case "crystal" -> "resonant";
            case "dust" -> "reactive";
            case "fuel" -> "energetic";
            case "utility" -> "adaptive";
            default -> "versatile";
        };
        String signature = "signature_" + material.name();
        float mining = 0.80F + tier * 0.13F + spreadA;
        float attack = tier * 0.18F + (material.kind().equals("gem") ? 0.35F : 0.0F) + spreadB;
        float protection = tier * 0.012F + (material.kind().equals("crystal") ? 0.025F : 0.0F) + spreadA * 0.02F;
        float durability = 0.85F + tier * 0.08F + (material.kind().equals("metal") || material.kind().equals("alloy") ? 0.15F : 0.0F) + spreadB;
        float utility = Math.min(0.35F, tier * 0.025F + (hash % 7) * 0.008F);
        return new RichGearProfile(material.name(), material.kind(), material.tier(), signature, family,
                Math.max(0.65F,mining), Math.max(0.0F,attack), Math.max(0.0F,protection), Math.max(0.7F,durability), utility);
    }
}
