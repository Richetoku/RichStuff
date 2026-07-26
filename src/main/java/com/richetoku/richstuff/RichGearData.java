package com.richetoku.richstuff;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Persistent material composition, experience, levels, durability and modifier slots for native Rich Gear. */
public final class RichGearData {
    private static final String ROOT = "RichGear";
    private RichGearData() {}

    public static boolean isGear(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof RichGearMarker; }
    public static String primary(ItemStack stack) { return data(stack).getString("Primary"); }
    public static int storedLevel(ItemStack stack) { return Math.max(1, data(stack).getInt("Level")); }
    public static int level(ItemStack stack) { return Math.min(storedLevel(stack), maxLevel()); }
    public static int experience(ItemStack stack) { return storedLevel(stack) >= maxLevel() ? 0 : Math.max(0, data(stack).getInt("Experience")); }
    public static int slots(ItemStack stack) { return 1 + Math.max(0, (level(stack) - 1) / 5); }
    public static int maxLevel() { return Math.max(1, RichStuffConfig.RICH_GEAR_MAX_LEVEL.get()); }

    public static List<String> modifiers(ItemStack stack) {
        List<String> out = new ArrayList<>();
        ListTag list = data(stack).getList("Modifiers", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static void bind(ItemStack stack, String material) {
        CompoundTag root = root(stack);
        CompoundTag data = root.getCompound(ROOT);
        data.putString("Primary", material);
        if (!data.contains("Level")) data.putInt("Level", 1);
        if (!data.contains("Damage")) data.putInt("Damage", 0);
        root.put(ROOT, data);
        save(stack, root);
    }

    public static boolean addModifier(ItemStack stack, String material) {
        if (material.isBlank() || material.equals(primary(stack))) return false;
        List<String> values = modifiers(stack);
        if (values.contains(material) || values.size() >= slots(stack)) return false;
        values.add(material);
        CompoundTag root = root(stack);
        CompoundTag data = root.getCompound(ROOT);
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        data.put("Modifiers", list);
        root.put(ROOT, data);
        save(stack, root);
        return true;
    }

    /** Adds use XP and returns true when one or more levels were gained. */
    public static boolean addExperience(ItemStack stack, int amount, @Nullable Player player) {
        if (!isGear(stack) || amount <= 0 || isBroken(stack)) return false;
        CompoundTag root = root(stack);
        CompoundTag data = root.getCompound(ROOT);
        int level = Math.max(1, data.getInt("Level"));
        int cap = maxLevel();
        // A lowered server cap limits effective behavior without erasing an existing item's higher-level data.
        if (level >= cap) return false;

        int oldLevel = level;
        int xp = Math.max(0, data.getInt("Experience")) + amount;
        while (level < cap && xp >= required(level)) {
            xp -= required(level);
            level++;
        }
        if (level >= cap) xp = 0;
        data.putInt("Level", level);
        data.putInt("Experience", xp);
        root.put(ROOT, data);
        save(stack, root);

        if (level > oldLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(RichStuff.RICH_GEAR_LEVEL_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F + Math.min(0.25F, level * 0.01F));
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.richstuff.rich_gear.level_up", stack.getHoverName(), level), true);
        }
        return level > oldLevel;
    }

    public static void addExperience(ItemStack stack, int amount) { addExperience(stack, amount, null); }
    public static int required(int level) { return 100 + Math.max(0, level - 1) * 35; }

    public static int maxDurability(ItemStack stack) {
        if (!isGear(stack)) return Math.max(1, stack.getMaxDamage());
        int baseline = stack.getItem() instanceof RichGearArmorItem ? 240 : 512;
        return Math.max(1, Math.round(baseline * combinedProfile(stack).durability()));
    }

    public static int damage(ItemStack stack) { return Math.max(0, Math.min(maxDurability(stack), data(stack).getInt("Damage"))); }
    public static int durabilityRemaining(ItemStack stack) { return Math.max(0, maxDurability(stack) - damage(stack)); }
    public static boolean isBroken(ItemStack stack) { return isGear(stack) && damage(stack) >= maxDurability(stack); }

    public static void damage(ItemStack stack, int amount) {
        if (!isGear(stack) || amount <= 0 || isBroken(stack)) return;
        setDamage(stack, Math.min(maxDurability(stack), damage(stack) + amount));
    }

    public static void repair(ItemStack stack, int amount) {
        if (!isGear(stack) || amount <= 0) return;
        setDamage(stack, Math.max(0, damage(stack) - amount));
    }

    public static void setDamage(ItemStack stack, int value) {
        CompoundTag root = root(stack);
        CompoundTag data = root.getCompound(ROOT);
        data.putInt("Damage", Math.max(0, Math.min(maxDurability(stack), value)));
        root.put(ROOT, data);
        save(stack, root);
        // Native damage is kept at zero so vanilla cannot destroy the retained Rich Gear stack.
        if (stack.getDamageValue() != 0) stack.setDamageValue(0);
    }

    /** Converts any vanilla wear applied during an item action into retained Rich Gear wear. */
    public static void absorbVanillaDamage(ItemStack stack) {
        if (!isGear(stack)) return;
        int vanillaDamage = Math.max(0, stack.getDamageValue());
        if (vanillaDamage > 0) {
            stack.setDamageValue(0);
            damage(stack, vanillaDamage);
        }
    }

    public static int repairAmount(ItemStack stack) {
        int flat = Math.max(0, RichStuffConfig.RICH_GEAR_REPAIR_AMOUNT.get());
        double percent = Math.max(0.0D, RichStuffConfig.RICH_GEAR_REPAIR_PERCENT.get());
        return Math.max(1, Math.max(flat, (int) Math.round(maxDurability(stack) * percent)));
    }

    public static RichGearProfile combinedProfile(ItemStack stack) {
        RichGearProfile base = RichGearProfiles.get(primary(stack));
        float mining = base.miningSpeed(), attack = base.attackBonus(), protection = base.protection(), durability = base.durability(), utility = base.utility();
        for (String modifier : modifiers(stack)) {
            RichGearProfile add = RichGearProfiles.get(modifier);
            mining += Math.max(0, add.miningSpeed() - 1) * 0.35F;
            attack += add.attackBonus() * 0.35F;
            protection += add.protection() * 0.35F;
            durability += Math.max(0, add.durability() - 1) * 0.35F;
            utility += add.utility() * 0.35F;
        }
        float levelBoost = 1.0F + (level(stack) - 1) * 0.0125F;
        return new RichGearProfile(base.material(), base.kind(), base.tier(), base.signatureTrait(), base.familyTrait(),
                mining * levelBoost, attack * levelBoost, protection * levelBoost, durability * levelBoost, utility * levelBoost);
    }

    private static CompoundTag data(ItemStack stack) {
        CompoundTag root = root(stack);
        return root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
    }
    private static CompoundTag root(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static void save(ItemStack stack, CompoundTag root) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root)); }
}
