package com.richetoku.richstuff.rikumimita.ai.actor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.richetoku.richstuff.rikumimita.ai.RikumiAiLifecycle;
import com.richetoku.richstuff.rikumimita.ai.survival.RikumiSurvivalValidator;
import com.richetoku.richstuff.rikumimita.ai.transport.CompanionApiClient;
import com.richetoku.richstuff.rikumimita.ai.transport.ProtocolEnvelope;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * RichStuff-owned NeoForge fake player used as Rikumi's server-authoritative action executor.
 * The visible {@code RikumiMitaEntity} mirrors this actor while external AI control is active.
 */
public final class RikumiFakePlayerActor {
    private final MinecraftServer server;
    private final UUID accountUuid;
    private final String accountName;
    private final GameProfile profile;
    private final CompanionApiClient transport;
    private FakePlayer player;
    private ServerLevel level;
    private int telemetryTicks;

    public RikumiFakePlayerActor(MinecraftServer server, UUID accountUuid, String accountName,
                                 CompanionApiClient transport) {
        this.server = server;
        this.accountUuid = accountUuid;
        this.accountName = accountName;
        this.profile = new GameProfile(accountUuid, accountName);
        this.transport = transport;
    }

    public UUID accountUuid() { return accountUuid; }
    public String accountName() { return accountName; }
    public boolean isOnline() { return player != null && level != null; }
    public Optional<FakePlayer> player() { return Optional.ofNullable(player); }

    public synchronized FakePlayer spawn(ServerLevel targetLevel, BlockPos position) {
        if (isOnline() && level != targetLevel) {
            despawn();
        }
        player = FakePlayerFactory.get(targetLevel, profile);
        level = targetLevel;
        player.setGameMode(GameType.SURVIVAL);
        RikumiSurvivalValidator.enforce(player);
        player.setInvisible(true);
        player.setSilent(true);
        if (player.getHealth() <= 0.0F) {
            player.setHealth(player.getMaxHealth());
        }
        player.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        return player;
    }

    /** Clears RichStuff's strong reference; NeoForge owns and reuses the cached FakePlayer instance. */
    public synchronized void despawn() {
        player = null;
        level = null;
        telemetryTicks = 0;
    }

    public void tick() {
        if (!isOnline()) return;
        RikumiSurvivalValidator.enforce(player);
        telemetryTicks++;
        if (transport != null && telemetryTicks >= 20) {
            telemetryTicks = 0;
            transport.send(ProtocolEnvelope.Type.ACTOR_STATUS, telemetry(), UUID.randomUUID());
        }
    }

    public RikumiActorSnapshot snapshot() {
        if (!isOnline()) {
            return RikumiActorSnapshot.offline(accountUuid, accountName, server.overworld().getGameTime());
        }
        return new RikumiActorSnapshot(
                accountUuid,
                accountName,
                true,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.getAirSupply(),
                player.getArmorValue(),
                player.totalExperience,
                player.experienceLevel,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.level().dimension().location().toString(),
                player.level().getGameTime(),
                Instant.now());
    }

    /** Detailed agent-facing state, including the exact inventory exposed by Rikumi's menu. */
    public JsonObject telemetry() {
        JsonObject json = snapshot().toJson();
        JsonArray inventory = new JsonArray();
        JsonArray craftable = new JsonArray();
        if (isOnline()) {
            for (int visibleSlot = 0; visibleSlot < 27; visibleSlot++) {
                addStack(inventory, "storage", visibleSlot, player.getInventory().getItem(9 + visibleSlot));
            }
            addStack(inventory, "main_hand", 27, player.getMainHandItem());
            addStack(inventory, "off_hand", 28, player.getOffhandItem());
            int coal = count(Items.COAL) + count(Items.CHARCOAL);
            int sticks = count(Items.STICK);
            int wheat = count(Items.WHEAT);
            if (coal > 0 && sticks > 0) craftable.add("minecraft:torch");
            if (wheat >= 3) craftable.add("minecraft:bread");
            json.addProperty("selected_hotbar_slot", player.getInventory().selected);
        }
        json.add("inventory", inventory);
        json.add("craftable_utility_items", craftable);
        json.addProperty("inventory_storage_slots", 27);
        json.addProperty("main_hand_slot", 27);
        json.addProperty("off_hand_slot", 28);
        json.addProperty("local_autonomy_available", true);
        json.addProperty("local_autonomy_active", isOnline() && !RikumiAiLifecycle.externalAgentActive(level));
        return json;
    }

    private void addStack(JsonArray inventory, String role, int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        JsonObject entry = new JsonObject();
        entry.addProperty("role", role);
        entry.addProperty("slot", slot);
        entry.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        entry.addProperty("count", stack.getCount());
        entry.addProperty("max_stack", stack.getMaxStackSize());
        entry.addProperty("display_name", stack.getHoverName().getString());
        inventory.add(entry);
    }

    private int count(net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    public ServerLevel level() {
        if (level == null) throw new IllegalStateException("Rikumi fake player is offline");
        return level;
    }

    public FakePlayer requirePlayer() {
        if (player == null) throw new IllegalStateException("Rikumi fake player is offline");
        return player;
    }
}
