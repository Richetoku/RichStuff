package com.richetoku.richstuff.rikumimita.ai.autonomy;

import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.RikumiMode;
import com.richetoku.richstuff.rikumimita.RikumiAction;
import com.richetoku.richstuff.rikumimita.ai.speech.RikumiSpeechService;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
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
    private long nextWorkTick;
    private BlockPos activeLightPos;
    private final RikumiProgressionPlanner progressionPlanner = new RikumiProgressionPlanner();

    public void tick(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar, boolean agentActive) {
        updateHeldLight(level, player, avatar);
        RikumiMode mode = avatar.getMode();
        if (agentActive || mode == RikumiMode.STAY) {
            RikumiMiningController.clear(level, avatar);
            if (player.isUsingItem()) player.stopUsingItem();
            return;
        }
        boolean workingMode = mode == RikumiMode.ASSIST || mode == RikumiMode.AUTO;
        if (workingMode && !avatar.hasHome()) {
            RikumiMiningController.clear(level, avatar);
            avatar.getNavigation().stop();
            avatar.setGoalStatus("Set Home", "Set a home before Rikumi begins mining, building, placing, or utility work");
            avatar.setTaskStatus("Waiting", "Use Set Home in Rikumi's GUI");
            return;
        }
        if (workingMode && !avatar.isWithinHomeWorkArea()) {
            RikumiMiningController.clear(level, avatar);
            BlockPos home = avatar.getHomePosition();
            if (home != null) avatar.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.05D);
            avatar.setGoalStatus("Return Home", "Return to the configured home work area");
            avatar.setTaskStatus("Traveling", "Walking back to home before resuming work");
            avatar.setActionState(RikumiAction.WALK, 12);
            return;
        }
        // An active break session owns navigation and movement until it finishes or is cancelled.
        // Running progression or lighting first can retarget Rikumi between damage ticks, which makes
        // her pace back and forth while the server repeatedly resets block-destruction progress.
        if (RikumiMiningController.continueActive(level, player, avatar)) return;

        // Foundational crafting and tool upgrades take precedence only when no block is being worked.
        if (workingMode && progressionPlanner.needsFoundationalProgression(player)
                && progressionPlanner.tick(level, player, avatar)) return;
        if (workingMode && progressionPlanner.prioritizeLighting(level, player, avatar)) return;
        long now = level.getGameTime();
        if (now < nextGeneralTick) return;
        nextGeneralTick = now + 10L;

        Player owner = avatar.getOwner() instanceof Player ownerPlayer ? ownerPlayer : null;
        equipUsefulItems(player);

        if (mode == RikumiMode.PATROL) {
            if (defend(level, player, owner)) {
                avatar.setCurrentTask("Attacking a hostile mob");
                avatar.setActionState(RikumiAction.ATTACK, 12);
            }
            maintainShield(level, player);
            return;
        }
        if (mode == RikumiMode.FOLLOW) {
            maintainShield(level, player);
            eatWhenNeeded(player);
            return;
        }

        if (avatar.tickBuildProject(level, player)) return;
        if (progressionPlanner.tick(level, player, avatar)) return;

        boolean collected = collectNearbyItems(level, player, avatar);
        if (collected) {
            avatar.setCurrentTask("Collected nearby supplies");
            avatar.setActionState(RikumiAction.USE_ITEM, 8);
            if (now >= nextVoiceTick) {
                nextVoiceTick = now + 100L;
                RikumiSpeechService.playPreset(avatar, "found");
            }
        }
        if (defend(level, player, owner)) {
            avatar.setCurrentTask("Defending against a hostile mob");
            avatar.setActionState(RikumiAction.ATTACK, 12);
            if (now >= nextVoiceTick) {
                nextVoiceTick = now + 100L;
                RikumiSpeechService.playPreset(avatar, "warning");
            }
            return;
        }
        maintainShield(level, player);
        eatWhenNeeded(player);

        if (now >= nextCraftTick) {
            nextCraftTick = now + 100L;
            if (craftUsefulSupplies(player)) {
                avatar.setCurrentTask("Crafting useful supplies");
                avatar.setActionState(RikumiAction.CRAFT, 16);
                RikumiSpeechService.playPreset(avatar, "crafting");
            }
        }
        if (now >= nextWorkTick) {
            nextWorkTick = now + 60L;
            if (cookNearby(level, player, avatar)) return;
            // ProgressionPlanner owns deterministic shaft mining; never fall back to random nearby ore destruction.
            if (tryFishing(level, player, avatar)) return;
        }
        if (now >= nextUtilityTick) {
            nextUtilityTick = now + 160L;
            if (placeLightWhenNeeded(level, player, avatar)) {
                avatar.setCurrentTask("Placing a light source");
                avatar.setActionState(RikumiAction.BUILD, 10);
            } else if (useNearbyRichUtility(level, player, avatar)) {
                avatar.setCurrentTask("Using a nearby Rich machine or tank");
                avatar.setActionState(RikumiAction.USE_ITEM, 12);
            }
        }
    }


    private static boolean cookNearby(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        int foodSlot = findSlot(player, RikumiAutonomousHelper::isRawCookableFood);
        if (foodSlot < 0) return false;
        int fuelSlot = findSlot(player, stack -> stack.is(ItemTags.COALS));
        if (fuelSlot < 0) return false;
        BlockPos origin = avatar.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 2, 4))) {
            if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)
                    || container.getContainerSize() < 3) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if (id == null || !(id.getPath().contains("furnace") || id.getPath().contains("smoker"))) continue;
            if (!avatar.canWorkAt(pos)) continue;
            if (avatar.distanceToSqr(pos.getCenter()) > 9.0D) {
                avatar.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D);
                avatar.setCurrentTask("Walking to a furnace to cook food");
                avatar.setActionState(RikumiAction.WALK, 12);
                return true;
            }
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
            ItemStack raw = player.getInventory().getItem(foodSlot);
            if (container.getItem(0).isEmpty()) {
                container.setItem(0, raw.copyWithCount(1));
                raw.shrink(1);
                if (raw.isEmpty()) player.getInventory().setItem(foodSlot, ItemStack.EMPTY);
            }
            ItemStack fuel = player.getInventory().getItem(fuelSlot);
            if (container.getItem(1).isEmpty() && !fuel.isEmpty()) {
                container.setItem(1, fuel.copyWithCount(1));
                fuel.shrink(1);
                if (fuel.isEmpty()) player.getInventory().setItem(fuelSlot, ItemStack.EMPTY);
            }
            container.setChanged();
            player.getInventory().setChanged();
            avatar.setCurrentGoal("Prepare useful food and supplies");
            avatar.setCurrentTask("Cooking food in " + level.getBlockState(pos).getBlock().getName().getString());
            avatar.setActionState(RikumiAction.CRAFT, 18);
            return true;
        }
        return false;
    }

    private static boolean tryFishing(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        int rodSlot = findSlot(player, stack -> stack.is(Items.FISHING_ROD));
        if (rodSlot < 0 || player.fishing != null) return false;
        BlockPos origin = avatar.blockPosition();
        BlockPos water = null;
        outer: for (int radius = 2; radius <= 6; radius++) {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 1, radius))) {
                if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) { water = pos.immutable(); break outer; }
            }
        }
        if (water == null || !avatar.canWorkAt(water)) return false;
        if (avatar.distanceToSqr(water.getCenter()) > 9.0D) {
            avatar.getNavigation().moveTo(water.getX() + 0.5D, water.getY() + 1.0D, water.getZ() + 0.5D, 0.9D);
            avatar.setCurrentGoal("Catch food by fishing");
            avatar.setCurrentTask("Walking to nearby water");
            avatar.setActionState(RikumiAction.WALK, 12);
            return true;
        }
        avatar.getNavigation().stop();
        avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
        swapIntoSelected(player, rodSlot);
        face(player, water.getCenter());
        player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        avatar.setCurrentGoal("Catch food by fishing");
        avatar.setCurrentTask("Fishing");
        avatar.setActionState(RikumiAction.FISH, 30);
        return true;
    }

    private static boolean isRawCookableFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN)
                || stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD)
                || stack.is(Items.SALMON) || stack.is(Items.POTATO) || stack.is(Items.KELP);
    }

    private static boolean collectNearbyItems(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.5D),
                item -> item.isAlive() && !item.getItem().isEmpty() && !item.hasPickUpDelay());
        items.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (items.isEmpty()) return false;
        ItemEntity nearest = items.get(0);
        if (player.distanceToSqr(nearest) <= 3.0D) {
            ItemStack remaining = nearest.getItem();
            int before = remaining.getCount();
            player.getInventory().add(remaining);
            int pickedUp = Math.max(0, before - remaining.getCount());
            if (pickedUp <= 0) return false;
            // Use the visible Rikumi entity as the collector so the vanilla pickup animation flies
            // into her model instead of the hidden survival fake player.
            level.getChunkSource().broadcast(nearest,
                    new ClientboundTakeItemEntityPacket(nearest.getId(), avatar.getId(), pickedUp));
            if (remaining.isEmpty()) nearest.discard();
            else nearest.setItem(remaining);
            player.getInventory().setChanged();
            return true;
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

    private void updateHeldLight(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (!avatar.hasHome() || !avatar.canWorkAt(player.blockPosition())) {
            clearHeldLight(level);
            return;
        }
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

    private static boolean placeLightWhenNeeded(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        BlockPos work = avatar.getLastMinedBlock() == null ? avatar.getMiningShaft() : avatar.getLastMinedBlock();
        if (work == null || level.getMaxLocalRawBrightness(work) > 7) return false;
        BlockPos last = avatar.getLastTorchPosition();
        if (last != null && last.distSqr(work) < 36.0D) return false;
        InteractionHand hand = isLightSource(player.getOffhandItem()) ? InteractionHand.OFF_HAND
                : isLightSource(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : null;
        if (hand == null) return false;
        for (BlockPos candidate : new BlockPos[]{work, work.above(), player.blockPosition(), player.blockPosition().above()}) {
            if (!avatar.canWorkAt(candidate) || !level.getBlockState(candidate).canBeReplaced()
                    || level.getMaxLocalRawBrightness(candidate) > 7) continue;
            for (Direction wall : Direction.Plane.HORIZONTAL) {
                BlockPos support = candidate.relative(wall);
                Direction face = wall.getOpposite();
                if (!level.getBlockState(support).isFaceSturdy(level, support, face)) continue;
                avatar.getNavigation().stop();
                avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5D)),
                        face, support, false);
                boolean placed = player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit).consumesAction();
                if (placed) avatar.rememberTorch(candidate);
                return placed;
            }
        }
        return false;
    }

    private static boolean useNearbyRichUtility(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) return false;
        BlockPos origin = player.blockPosition();
        for (BlockPos target : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2))) {
            if (!avatar.canWorkAt(target) || avatar.distanceToSqr(target.getCenter()) > 9.0D) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock());
            if (id == null || !(id.getNamespace().equals("richstuff") || id.getNamespace().equals("richmachines"))) continue;
            if (!(id.getPath().contains("tank") || id.getPath().contains("machine") || id.getPath().contains("extractor")
                    || id.getPath().contains("foundry") || id.getPath().contains("juicer")
                    || id.getPath().contains("separator") || id.getPath().contains("cutter"))) continue;
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
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
