package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Baseline iron-capable tool bodies whose material composition and retained progression live in stack data. */
public final class RichGearToolItems {
    private RichGearToolItems() {}
    private static Item.Properties pickaxe(Item.Properties p) { return p.durability(512).attributes(PickaxeItem.createAttributes(Tiers.IRON, 1.0F, -2.8F)); }
    private static Item.Properties axe(Item.Properties p) { return p.durability(512).attributes(AxeItem.createAttributes(Tiers.IRON, 6.0F, -3.1F)); }
    private static Item.Properties shovel(Item.Properties p) { return p.durability(512).attributes(ShovelItem.createAttributes(Tiers.IRON, 1.5F, -3.0F)); }
    private static Item.Properties hoe(Item.Properties p) { return p.durability(512).attributes(HoeItem.createAttributes(Tiers.IRON, -2.0F, -1.0F)); }
    private static Item.Properties sword(Item.Properties p) { return p.durability(512).attributes(SwordItem.createAttributes(Tiers.IRON, 3.0F, -2.4F)); }

    private static boolean mine(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user, MineAction action) {
        if (RichGearData.isBroken(stack)) return false;
        boolean result = action.run();
        RichGearData.absorbVanillaDamage(stack);
        if (!level.isClientSide) RichGearData.addExperience(stack, 2, user instanceof Player player ? player : null);
        return result;
    }

    private static boolean attack(ItemStack stack, LivingEntity target, LivingEntity user, AttackAction action, int xp) {
        if (RichGearData.isBroken(stack)) return false;
        boolean result = action.run();
        RichGearData.absorbVanillaDamage(stack);
        RichGearData.addExperience(stack, xp, user instanceof Player player ? player : null);
        return result;
    }

    private static InteractionResult guardedUse(ItemStack stack, UseAction action) {
        if (RichGearData.isBroken(stack)) return InteractionResult.FAIL;
        InteractionResult result = action.run();
        RichGearData.absorbVanillaDamage(stack);
        return result;
    }

    private static void tooltip(ItemStack stack, Item.TooltipContext context, List<Component> output, TooltipFlag flag, TooltipAction parent) {
        RichGearTooltips.append(stack, output::add);
        parent.run();
    }

    private static boolean barVisible(ItemStack stack) { return RichGearData.damage(stack) > 0; }
    private static int barWidth(ItemStack stack) { return Math.round(13.0F * RichGearData.durabilityRemaining(stack) / Math.max(1.0F, RichGearData.maxDurability(stack))); }
    private static int barColor(ItemStack stack) {
        if (RichGearData.isBroken(stack)) return 0xD01818;
        float ratio = RichGearData.durabilityRemaining(stack) / Math.max(1.0F, RichGearData.maxDurability(stack));
        return net.minecraft.util.Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }

    @FunctionalInterface private interface MineAction { boolean run(); }
    @FunctionalInterface private interface AttackAction { boolean run(); }
    @FunctionalInterface private interface UseAction { InteractionResult run(); }
    @FunctionalInterface private interface TooltipAction { void run(); }

    public static final class Pickaxe extends PickaxeItem implements RichGearMarker {
        public Pickaxe(Item.Properties p) { super(Tiers.IRON, pickaxe(p)); }
        @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) { return mine(s, l, st, p, e, () -> super.mineBlock(s, l, st, p, e)); }
        @Override public boolean isBarVisible(ItemStack s) { return barVisible(s); }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> out, TooltipFlag f) { tooltip(s, c, out, f, () -> super.appendHoverText(s, c, out, f)); }
    }

    public static final class Axe extends AxeItem implements RichGearMarker {
        public Axe(Item.Properties p) { super(Tiers.IRON, axe(p)); }
        @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) { return mine(s, l, st, p, e, () -> super.mineBlock(s, l, st, p, e)); }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) { return attack(s, t, a, () -> super.hurtEnemy(s, t, a), 3); }
        @Override public InteractionResult useOn(UseOnContext c) { return RichGearToolItems.guardedUse(c.getItemInHand(), () -> super.useOn(c)); }
        @Override public boolean isBarVisible(ItemStack s) { return barVisible(s); }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> out, TooltipFlag f) { tooltip(s, c, out, f, () -> super.appendHoverText(s, c, out, f)); }
    }

    public static final class Shovel extends ShovelItem implements RichGearMarker {
        public Shovel(Item.Properties p) { super(Tiers.IRON, shovel(p)); }
        @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) { return mine(s, l, st, p, e, () -> super.mineBlock(s, l, st, p, e)); }
        @Override public InteractionResult useOn(UseOnContext c) { return RichGearToolItems.guardedUse(c.getItemInHand(), () -> super.useOn(c)); }
        @Override public boolean isBarVisible(ItemStack s) { return barVisible(s); }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> out, TooltipFlag f) { tooltip(s, c, out, f, () -> super.appendHoverText(s, c, out, f)); }
    }

    public static final class Hoe extends HoeItem implements RichGearMarker {
        public Hoe(Item.Properties p) { super(Tiers.IRON, hoe(p)); }
        @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) { return mine(s, l, st, p, e, () -> super.mineBlock(s, l, st, p, e)); }
        @Override public InteractionResult useOn(UseOnContext c) { return RichGearToolItems.guardedUse(c.getItemInHand(), () -> super.useOn(c)); }
        @Override public boolean isBarVisible(ItemStack s) { return barVisible(s); }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> out, TooltipFlag f) { tooltip(s, c, out, f, () -> super.appendHoverText(s, c, out, f)); }
    }

    public static final class Sword extends SwordItem implements RichGearMarker {
        public Sword(Item.Properties p) { super(Tiers.IRON, sword(p)); }
        @Override public int getMaxDamage(ItemStack stack) { return RichGearData.maxDurability(stack); }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) { return attack(s, t, a, () -> super.hurtEnemy(s, t, a), 4); }
        @Override public boolean isBarVisible(ItemStack s) { return barVisible(s); }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> out, TooltipFlag f) { tooltip(s, c, out, f, () -> super.appendHoverText(s, c, out, f)); }
    }
}
