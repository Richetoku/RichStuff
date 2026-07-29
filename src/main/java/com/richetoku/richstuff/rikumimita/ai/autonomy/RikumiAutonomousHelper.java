package com.richetoku.richstuff.rikumimita.ai.autonomy;

import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.ai.speech.RikumiSpeechService;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Conservative local helper behavior used only while no external agent is issuing commands.
 * It never duplicates items and performs all interactions through the survival fake player.
 */
public final class RikumiAutonomousHelper {
    private long nextGeneralTick;
    private long nextCraftTick;
    private long nextUtilityTick;
    private long nextVoiceTick;
    private BlockPos activeLightPos;

    public void tick(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar, boolean agentActive) {
        updateHeldLight(level, player);
        if (agentActive || avatar.isOrderedToSit()) {
            if (player.isUsingItem()) player.stopUsingItem();
            return;
        }
        long now = level.getGameTime();
        if (now < nextGeneralTick) return;
        nextGeneralTick = now + 10L;

        Player owner = avatar.getOwner() instanceof Player ownerPlayer ? ownerPlayer : null;
        boolean collected = collectNearbyItems(level, player);
        equipUsefulItems(player);
        if (collected && now >= nextVoiceTick) {
            nextVoiceTick = now + 100L;
            RikumiSpeechService.playPreset(avatar, "found");
        }
        if (defend(level, player, owner)) {
            if (now >= nextVoiceTick) {
                nextVoiceTick = now + 100L;
                RikumiSpeechService.playPreset(avatar, "warning");
            }
            return;
        }
        maintainShield(level, player);
        eatWhenNeeded(player);
        followOwner(player, owner);

        if (now >= nextCraftTick) {
            nextCraftTick = now + 100L;
            if (craftUsefulSupplies(player)) RikumiSpeechService.playPreset(avatar, "crafting");
        }
        if (now >= nextUtilityTick) {
            nextUtilityTick = now + 160L;
            if (!placeLightWhenNeeded(level, player)) useNearbyRichUtility(level, player);
        }
    }

    private static boolean collectNearbyItems(ServerLevel level, FakePlayer player) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.5D),
                item -> item.isAlive() && !item.getItem().isEmpty());
        items.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (items.isEmpty()) return false;
        ItemEntity nearest = items.get(0);
        if (player.distanceToSqr(nearest) <= 3.0D) {
            int before = nearest.getItem().getCount();
            nearest.playerTouch(player);
            return !nearest.isAlive() || nearest.getItem().getCount() < before;
        }
        moveToward(player, nearest.position(), 0.18D);
        return false;
    }

    private static boolean defend(ServerLevel level, FakePlayer player, Player owner) {
        Vec3 center = owner == null ? player.position() : owner.position();
        AABB area = new AABB(center, center).inflate(7.0D, 4.0D, 7.0D);
        List<Monster> threats = level.getEntitiesOfClass(Monster.class, area,
                mob -> mob.isAlive() && (mob.getTarget() == owner || mob.getTarget() == player
                        || mob.distanceToSqr(center) < 16.0D));
        threats.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (threats.isEmpty()) return false;
        equipShield(player);
        Monster target = threats.get(0);
        double distance = player.distanceToSqr(target);
        face(player, target.position());
        if (distance <= 10.0D && player.hasLineOfSight(target)) {
            player.attack(target);
            player.swing(InteractionHand.MAIN_HAND, true);
        } else {
            moveToward(player, target.position(), 0.23D);
        }
        return true;
    }

    private static void maintainShield(ServerLevel level, FakePlayer player) {
        boolean threatClose = !level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(4.0D),
                Entity::isAlive).isEmpty();
        if (threatClose && player.getOffhandItem().is(Items.SHIELD)) {
            if (!player.isUsingItem()) player.startUsingItem(InteractionHand.OFF_HAND);
        } else if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            player.stopUsingItem();
        }
    }

    private static void eatWhenNeeded(FakePlayer player) {
        if (!player.getFoodData().needsFood() || player.isUsingItem()) return;
        int foodSlot = findSlot(player, stack -> stack.has(DataComponents.FOOD));
        if (foodSlot < 0) return;
        swapIntoSelected(player, foodSlot);
        player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    private static void followOwner(FakePlayer player, Player owner) {
        if (owner == null || owner.level() != player.level()) return;
        double distance = player.distanceToSqr(owner);
        if (distance > 36.0D) moveToward(player, owner.position(), 0.25D);
        else if (distance > 16.0D) moveToward(player, owner.position(), 0.18D);
    }

    private static void equipUsefulItems(FakePlayer player) {
        int weapon = findSlot(player, stack -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES));
        if (weapon >= 0 && !isWeapon(player.getMainHandItem())) swapIntoSelected(player, weapon);

        int light = findSlot(player, RikumiAutonomousHelper::isLightSource);
        if (light >= 0 && !isLightSource(player.getOffhandItem())) swapIntoOffhand(player, light);
    }

    private static void equipShield(FakePlayer player) {
        if (player.getOffhandItem().is(Items.SHIELD)) return;
        int shield = findSlot(player, stack -> stack.is(Items.SHIELD));
        if (shield >= 0) swapIntoOffhand(player, shield);
    }

    private static boolean craftUsefulSupplies(FakePlayer player) {
        int torches = count(player, stack -> stack.is(Items.TORCH));
        if (torches < 8) {
            int coal = findSlot(player, stack -> stack.is(ItemTags.COALS));
            int stick = findSlot(player, stack -> stack.is(Items.STICK));
            if (coal >= 0 && stick >= 0 && canAdd(player, new ItemStack(Items.TORCH, 4))) {
                consumeOne(player, coal);
                if (stick == coal && player.getInventory().getItem(stick).isEmpty()) {
                    stick = findSlot(player, stack -> stack.is(Items.STICK));
                }
                consumeOne(player, stick);
                player.getInventory().add(new ItemStack(Items.TORCH, 4));
                player.getInventory().setChanged();
                return true;
            }
        }
        if (player.getFoodData().getFoodLevel() < 16) {
            int wheat = count(player, stack -> stack.is(Items.WHEAT));
            if (wheat >= 3 && canAdd(player, new ItemStack(Items.BREAD))) {
                consume(player, stack -> stack.is(Items.WHEAT), 3);
                player.getInventory().add(new ItemStack(Items.BREAD));
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private void updateHeldLight(ServerLevel level, FakePlayer player) {
        ItemStack offhand = player.getOffhandItem();
        int lightLevel = offhand.getItem() instanceof BlockItem blockItem
                ? blockItem.getBlock().defaultBlockState().getLightEmission() : 0;
        BlockPos target = player.blockPosition();
        if (lightLevel <= 0) {
            clearHeldLight(level);
            return;
        }
        if (activeLightPos != null && !activeLightPos.equals(target)) clearHeldLight(level);
        if (!level.getBlockState(target).isAir() && !level.getBlockState(target).is(Blocks.LIGHT)) return;
        int value = Math.max(1, Math.min(15, lightLevel));
        level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, value), 3);
        activeLightPos = target.immutable();
    }

    public void clearHeldLight(ServerLevel level) {
        if (activeLightPos != null && level.getBlockState(activeLightPos).is(Blocks.LIGHT)) {
            level.setBlock(activeLightPos, Blocks.AIR.defaultBlockState(), 3);
        }
        activeLightPos = null;
    }

    private static boolean placeLightWhenNeeded(ServerLevel level, FakePlayer player) {
        if (level.getMaxLocalRawBrightness(player.blockPosition()) >= 7) return false;
        InteractionHand hand = isLightSource(player.getOffhandItem()) ? InteractionHand.OFF_HAND
                : isLightSource(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : null;
        if (hand == null) return false;
        BlockPos base = player.blockPosition().below();
        if (!level.getBlockState(base).isFaceSturdy(level, base, Direction.UP)
                || !level.getBlockState(base.above()).canBeReplaced()) return false;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(base).add(0.0D, 0.5D, 0.0D),
                Direction.UP, base, false);
        return player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit).consumesAction();
    }

    private static boolean useNearbyRichUtility(ServerLevel level, FakePlayer player) {
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) return false;
        BlockPos origin = player.blockPosition();
        for (BlockPos target : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2))) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock());
            if (id == null || !(id.getNamespace().equals("richstuff") || id.getNamespace().equals("richmachines"))) continue;
            if (!(id.getPath().contains("tank") || id.getPath().contains("machine") || id.getPath().contains("extractor")
                    || id.getPath().contains("foundry") || id.getPath().contains("juicer")
                    || id.getPath().contains("separator") || id.getPath().contains("cutter"))) continue;
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
            InteractionHand hand = player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit).consumesAction()) return true;
        }
        return false;
    }

    private static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES));
    }

    private static boolean isLightSource(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    private static void moveToward(FakePlayer player, Vec3 destination, double speed) {
        Vec3 delta = destination.subtract(player.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() < 0.0001D) return;
        face(player, destination);
        player.move(MoverType.SELF, horizontal.normalize().scale(speed));
    }

    private static void face(FakePlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static int findSlot(FakePlayer player, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (predicate.test(player.getInventory().getItem(slot))) return slot;
        }
        return -1;
    }

    private static int count(FakePlayer player, java.util.function.Predicate<ItemStack> predicate) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static void swapIntoSelected(FakePlayer player, int slot) {
        int selected = player.getInventory().selected;
        if (slot == selected) return;
        ItemStack target = player.getInventory().getItem(slot).copy();
        ItemStack current = player.getInventory().getItem(selected).copy();
        player.getInventory().setItem(selected, target);
        player.getInventory().setItem(slot, current);
        player.getInventory().setChanged();
    }

    private static void swapIntoOffhand(FakePlayer player, int slot) {
        if (slot < 0) return;
        ItemStack target = player.getInventory().getItem(slot).copy();
        ItemStack current = player.getOffhandItem().copy();
        player.getInventory().setItem(slot, current);
        player.setItemInHand(InteractionHand.OFF_HAND, target);
        player.getInventory().setChanged();
    }

    private static boolean canAdd(FakePlayer player, ItemStack result) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize()) return true;
        }
        return false;
    }

    private static void consumeOne(FakePlayer player, int slot) {
        if (slot < 0) return;
        ItemStack stack = player.getInventory().getItem(slot);
        stack.shrink(1);
        if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
    }

    private static void consume(FakePlayer player, java.util.function.Predicate<ItemStack> predicate, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!predicate.test(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
    }
}
