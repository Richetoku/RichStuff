package com.richetoku.richstuff.rikumimita.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.ai.actor.RikumiFakePlayerActor;
import com.richetoku.richstuff.rikumimita.ai.autonomy.RikumiAutonomousHelper;
import com.richetoku.richstuff.rikumimita.ai.control.RikumiExternalControl;
import com.richetoku.richstuff.rikumimita.ai.transport.CompanionApiClient;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematicRegistry;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** RichStuff-owned lifecycle for the integrated Rikumi AI fake-player controller. */
public final class RikumiAiLifecycle {
    private static boolean registered;
    private static RikumiAiSettings settings;
    private static CompanionApiClient transport;
    private static RikumiFakePlayerActor actor;
    private static boolean autoSpawnSuppressed;
    private static long lastExternalControlTick = Long.MIN_VALUE;
    private static final RikumiAutonomousHelper AUTONOMOUS_HELPER = new RikumiAutonomousHelper();
    private static WeakReference<RikumiMitaEntity> avatar = new WeakReference<>(null);

    private RikumiAiLifecycle() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(RikumiAiLifecycle::onServerStarting);
        NeoForge.EVENT_BUS.addListener(RikumiAiLifecycle::onServerStopping);
        NeoForge.EVENT_BUS.addListener(RikumiAiLifecycle::onServerTick);
    }

    private static synchronized void onServerStarting(ServerStartingEvent event) {
        cleanup();
        var server = event.getServer();
        settings = RikumiAiSettings.load();
        RikumiSchematicRegistry.reload(server);
        autoSpawnSuppressed = false;
        if (!settings.enabled()) {
            RichStuff.LOGGER.info("RichStuff Rikumi AI integration is disabled.");
            return;
        }

        JsonObject registration = new JsonObject();
        registration.addProperty("schema", "richstuff.rikumi_registration.v1");
        registration.addProperty("mod_id", RichStuff.MODID);
        registration.addProperty("account_uuid", settings.accountUuid().toString());
        registration.addProperty("account_name", settings.accountName());
        registration.addProperty("backend", "neoforge_fake_player");
        registration.addProperty("minecraft_version", "1.21.1");
        registration.addProperty("ts", Instant.now().toString());
        registration.addProperty("inventory_storage_slots", 27);
        registration.addProperty("main_hand_slot", true);
        registration.addProperty("off_hand_slot", true);
        registration.addProperty("local_autonomy_when_agent_idle", true);
        registration.add("external_actions", arrayOf("spawn", "despawn", "snapshot", "move_relative", "path_to", "look",
                "select_slot", "swing", "use_item", "use_item_on", "attack", "break_block",
                "interact_entity", "set_mode", "set_goal", "set_task", "list_schematics",
                "build_schematic", "cancel_project", "chat", "speak", "tts"));
        registration.add("behavior_modes", arrayOf("stay", "follow", "assist", "auto", "patrol"));
        registration.add("local_helper_actions", arrayOf("mob_pathfinding", "follow_owner_at_safe_distance",
                "collect_items", "defend_owner", "patrol", "mine", "fish", "cook",
                "equip_tools_and_weapons", "use_shield", "place_light", "eat_food",
                "craft_recipe_aware_supplies", "use_rich_tanks_and_machines", "build_schematic_one_block_at_a_time"));
        registration.add("schematic_formats", arrayOf("richstuff_datapack_json", "vanilla_structure_nbt",
                "create_schematic_nbt", "minecolonies_structurize_blueprint"));
        registration.add("tts_modes", arrayOf("pregenerated_preset", "registered_sound_event", "chat_fallback"));

        if (settings.hasTransport()) {
            transport = new CompanionApiClient(settings.apiUrl(), settings.apiToken(), registration);
        } else {
            RichStuff.LOGGER.info("Rikumi fake-player actor enabled in passive mode; set COMPANION_API_URL and COMPANION_API_TOKEN to accept external AI commands.");
        }

        actor = new RikumiFakePlayerActor(server, settings.accountUuid(), settings.accountName(), transport);
        if (transport != null) {
            var control = new RikumiExternalControl(server, actor, transport, settings);
            transport.addListener("richstuff.rikumi.external_control", control);
            transport.start();
        }
        RichStuff.LOGGER.info("Integrated Rikumi AI actor initialized as {} ({}).", settings.accountName(), settings.accountUuid());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        RikumiFakePlayerActor active = actor;
        if (active == null) return;
        try {
            active.tick();
            avatar().ifPresent(entity -> entity.getInventoryHandler().syncWithActor());
            RikumiMitaEntity visible = avatar().orElse(null);
            if (visible == null || !visible.isAlive()) return;
            if (active.player().isPresent() && active.player().get().level() instanceof ServerLevel helperLevel) {
                boolean agentActive = externalAgentActive(helperLevel);
                if (!agentActive) alignActorToAvatar(visible, active.player().get());
                AUTONOMOUS_HELPER.tick(helperLevel, active.player().get(), visible, agentActive);
                visible.getInventoryHandler().syncWithActor();
                visible.syncDisplayedHands(active.player().get());
                if (settings != null && settings.mirrorAvatar() && agentActive) mirrorVisibleAvatar(visible, active);
                else visible.setNoAi(false);
            }

            if (!active.isOnline() && settings != null && settings.autoSpawn() && !autoSpawnSuppressed
                    && visible.level() instanceof ServerLevel level) {
                spawnActor(level, visible.blockPosition());
            }
        } catch (RuntimeException exception) {
            RichStuff.LOGGER.warn("Rikumi AI tick failed: {}", exception.getMessage());
        }
    }

    private static synchronized void onServerStopping(ServerStoppingEvent event) {
        cleanup();
    }

    public static void bindAvatar(RikumiMitaEntity entity) {
        if (entity.level().isClientSide()) return;
        RikumiMitaEntity current = avatar.get();
        if (current == entity) return;
        if (current != null && current.isAlive() && !current.isRemoved()
                && !current.getUUID().equals(entity.getUUID())) return;

        if (current != null) current.getInventoryHandler().detachFromActor();
        avatar = new WeakReference<>(entity);
        player().ifPresent(entity.getInventoryHandler()::attachToActor);
    }

    private static void alignActorToAvatar(RikumiMitaEntity visible, FakePlayer player) {
        if (player.level() != visible.level()) return;
        player.moveTo(visible.getX(), visible.getY(), visible.getZ(), visible.getYRot(), visible.getXRot());
        player.setYHeadRot(visible.getYHeadRot());
        player.setOnGround(visible.onGround());
        player.fallDistance = visible.fallDistance;
    }

    private static void mirrorVisibleAvatar(RikumiMitaEntity visible, RikumiFakePlayerActor active) {
        Optional<FakePlayer> playerOptional = active.player();
        if (playerOptional.isEmpty()) {
            visible.setNoAi(false);
            return;
        }
        FakePlayer player = playerOptional.get();
        if (player.level() != visible.level()) {
            visible.setNoAi(false);
            return;
        }
        visible.setNoAi(true);
        visible.setDeltaMovement(Vec3.ZERO);
        visible.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        visible.setYHeadRot(player.getYHeadRot());
        visible.setHealth(Math.min(visible.getMaxHealth(), Math.max(1.0F, player.getHealth())));
    }

    public static Optional<RikumiMitaEntity> avatar() {
        RikumiMitaEntity entity = avatar.get();
        return entity != null && entity.isAlive() ? Optional.of(entity) : Optional.empty();
    }

    public static Optional<FakePlayer> player() {
        return actor == null ? Optional.empty() : actor.player();
    }

    public static synchronized FakePlayer spawnActor(ServerLevel level, BlockPos position) {
        RikumiFakePlayerActor active = actor;
        if (active == null) throw new IllegalStateException("Rikumi AI actor is not initialized");
        autoSpawnSuppressed = false;
        FakePlayer player = active.spawn(level, position);
        attachInventory(player);
        return player;
    }

    public static synchronized void despawnActor() {
        autoSpawnSuppressed = true;
        detachInventory();
        if (actor != null) {
            actor.player().ifPresent(player -> {
                if (player.level() instanceof ServerLevel level) AUTONOMOUS_HELPER.clearHeldLight(level);
            });
            actor.despawn();
        }
    }

    private static void attachInventory(FakePlayer player) {
        avatar().ifPresent(entity -> entity.getInventoryHandler().attachToActor(player));
    }

    private static void detachInventory() {
        avatar().ifPresent(entity -> entity.getInventoryHandler().detachFromActor());
    }

    public static void markExternalControl() {
        FakePlayer player = actor == null ? null : actor.player().orElse(null);
        if (player != null) lastExternalControlTick = player.level().getGameTime();
    }

    public static boolean externalAgentActive(ServerLevel level) {
        return lastExternalControlTick != Long.MIN_VALUE
                && level.getGameTime() - lastExternalControlTick <= 100L;
    }

    /** High-level mode/project commands hand movement back to Rikumi's local navigator immediately. */
    public static void releaseExternalControl() {
        lastExternalControlTick = Long.MIN_VALUE;
    }

    private static JsonArray arrayOf(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    private static synchronized void cleanup() {
        detachInventory();
        if (transport != null) {
            transport.removeListener("richstuff.rikumi.external_control");
            transport.stop();
        }
        if (actor != null) {
            actor.player().ifPresent(player -> {
                if (player.level() instanceof ServerLevel level) AUTONOMOUS_HELPER.clearHeldLight(level);
            });
            actor.despawn();
        }
        transport = null;
        actor = null;
        autoSpawnSuppressed = false;
        lastExternalControlTick = Long.MIN_VALUE;
        settings = null;
        avatar = new WeakReference<>(null);
    }
}
