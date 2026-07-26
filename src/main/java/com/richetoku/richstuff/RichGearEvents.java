package com.richetoku.richstuff;

import com.richetoku.richcore.RichContentPartition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Runtime use, retained durability, repair and composition rules for RichStuff's built-in modular gear. */
public final class RichGearEvents {
    private RichGearEvents() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!RichGearData.isGear(stack)) return;
        if (RichGearData.isBroken(stack)) {
            event.setNewSpeed(0.05F);
            return;
        }
        float multiplier = Math.max(0.25F, RichGearData.combinedProfile(stack).miningSpeed());
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        float adjustedDamage = event.getNewDamage();
        if (adjustedDamage <= 0.0F) return;

        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (RichGearData.isGear(weapon) && !RichGearData.isBroken(weapon)) {
                RichGearProfile profile = RichGearData.combinedProfile(weapon);
                adjustedDamage = Math.max(0.0F, adjustedDamage + profile.attackBonus());
            }
        }

        if (event.getEntity() instanceof Player wearer) {
            float reduction = 0.0F;
            for (ItemStack armor : wearer.getArmorSlots()) {
                if (!RichGearData.isGear(armor) || RichGearData.isBroken(armor)) continue;
                reduction += RichGearData.combinedProfile(armor).protection();
                int wear = Math.max(1, Math.round(adjustedDamage));
                RichGearData.addExperience(armor, wear, wearer);
            }
            adjustedDamage *= Math.max(0.20F, 1.0F - Math.min(0.80F, reduction));
        }

        event.setNewDamage(Math.max(0.0F, adjustedDamage));
    }

    /** Clears incidental vanilla damage so Rich Gear cannot disappear at zero retained durability. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        for (ItemStack stack : player.getInventory().items) RichGearData.absorbVanillaDamage(stack);
        for (ItemStack stack : player.getInventory().armor) RichGearData.absorbVanillaDamage(stack);
        for (ItemStack stack : player.getInventory().offhand) RichGearData.absorbVanillaDamage(stack);
    }

    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!RichGearData.isGear(left) || right.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(right.getItem());
        if (id == null) return;
        String material = RichContentPartition.materialForItemId(id.getPath());
        if (material.isBlank() || !RichGearProfiles.contains(material)) return;

        ItemStack output = left.copy();
        String primary = RichGearData.primary(output);
        if (primary.isBlank()) {
            RichGearData.bind(output, material);
        } else if (primary.equals(material)) {
            if (RichGearData.damage(output) <= 0) return;
            RichGearData.repair(output, RichGearData.repairAmount(output));
        } else if (!RichGearData.addModifier(output, material)) {
            return;
        }

        event.setOutput(output);
        event.setMaterialCost(1);
        event.setCost(Math.max(1, RichGearData.level(output)));
    }
}
