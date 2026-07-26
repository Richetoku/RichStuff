package com.richetoku.richstuff;

import net.minecraft.network.chat.Component;

/** Deterministic, balanced native gear statistics for every RichStuff material category. */
public record RichGearProfile(String material, String kind, int tier, String signatureTrait,
                              String familyTrait, float miningSpeed, float attackBonus,
                              float protection, float durability, float utility) {
    public Component signatureName() { return Component.translatable("modifier.richstuff." + signatureTrait); }
    public Component familyName() { return Component.translatable("modifier.richstuff." + familyTrait); }
}
