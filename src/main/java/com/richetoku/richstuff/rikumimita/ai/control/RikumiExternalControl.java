package com.richetoku.richstuff.rikumimita.ai.control;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.ai.RikumiAiLifecycle;
import com.richetoku.richstuff.rikumimita.ai.RikumiAiSettings;
import com.richetoku.richstuff.rikumimita.ai.actor.RikumiFakePlayerActor;
import com.richetoku.richstuff.rikumimita.ai.chat.RikumiChatPrivacyFilter;
import com.richetoku.richstuff.rikumimita.ai.survival.RikumiSurvivalValidator;
import com.richetoku.richstuff.rikumimita.ai.speech.RikumiSpeechService;
import com.richetoku.richstuff.rikumimita.ai.transport.CompanionApiClient;
import com.richetoku.richstuff.rikumimita.ai.transport.ProtocolEnvelope;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Executes authenticated external AI actions through RichStuff's survival-mode fake player. */
public final class RikumiExternalControl implements CompanionApiClient.InboundListener {
    private static final double MAX_RELATIVE_MOVE = 1.25D;

    private final MinecraftServer server;
    private final RikumiFakePlayerActor actor;
    private final CompanionApiClient transport;
    private final RikumiAiSettings settings;

    public RikumiExternalControl(MinecraftServer server, RikumiFakePlayerActor actor,
                                 CompanionApiClient transport, RikumiAiSettings settings) {
        this.server = server;
        this.actor = actor;
        this.transport = transport;
        this.settings = settings;
    }

    @Override
    public void onInbound(ProtocolEnvelope envelope) {
        if (!ProtocolEnvelope.Type.ACTOR_CONTROL.equals(envelope.type())
                && !ProtocolEnvelope.Type.CHAT_MESSAGE.equals(envelope.type())) return;
        server.execute(() -> dispatch(envelope));
    }

    private void dispatch(ProtocolEnvelope envelope) {
        RikumiAiLifecycle.markExternalControl();
        String action = ProtocolEnvelope.Type.CHAT_MESSAGE.equals(envelope.type())
                ? "chat" : string(envelope.payload(), "action", "").toLowerCase(Locale.ROOT);
        try {
            JsonObject detail = switch (action) {
                case "spawn" -> spawn(envelope.payload());
                case "despawn" -> despawn();
                case "snapshot", "status" -> snapshot();
                case "move_relative", "move" -> moveRelative(envelope.payload());
                case "look" -> look(envelope.payload());
                case "select_slot" -> selectSlot(envelope.payload());
                case "swing" -> swing(envelope.payload());
                case "use_item" -> useItem(envelope.payload());
                case "use_item_on" -> useItemOn(envelope.payload());
                case "attack" -> attack(envelope.payload());
                case "break_block" -> breakBlock(envelope.payload());
                case "interact_entity" -> interactEntity(envelope.payload());
                case "teleport" -> teleport(envelope.payload());
                case "chat", "say" -> chat(envelope.payload());
                case "speak", "tts" -> speak(envelope.payload());
                default -> throw new IllegalArgumentException("Unknown Rikumi actor action: " + action);
            };
            sendResult(envelope, action, true, "ok", detail);
        } catch (RuntimeException exception) {
            RichStuff.LOGGER.warn("Rejected Rikumi external action '{}': {}", action, exception.getMessage());
            sendResult(envelope, action, false, safeMessage(exception), new JsonObject());
        }
    }

    private JsonObject spawn(JsonObject payload) {
        ServerLevel level = RikumiAiLifecycle.avatar()
                .filter(entity -> entity.level() instanceof ServerLevel)
                .map(entity -> (ServerLevel) entity.level())
                .orElse(server.overworld());
        BlockPos fallback = RikumiAiLifecycle.avatar().map(Entity::blockPosition)
                .orElse(level.getSharedSpawnPos());
        BlockPos position = blockPos(payload, fallback);
        RikumiAiLifecycle.spawnActor(level, position);
        return actor.telemetry();
    }

    private JsonObject despawn() {
        RikumiAiLifecycle.despawnActor();
        return actor.telemetry();
    }

    private JsonObject snapshot() {
        return actor.telemetry();
    }

    private JsonObject moveRelative(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        double dx = number(payload, "dx", 0.0D);
        double dy = number(payload, "dy", 0.0D);
        double dz = number(payload, "dz", 0.0D);
        Vec3 requested = new Vec3(dx, dy, dz);
        if (requested.length() > MAX_RELATIVE_MOVE) {
            requested = requested.normalize().scale(MAX_RELATIVE_MOVE);
        }
        player.move(MoverType.SELF, requested);
        return actor.telemetry();
    }

    private JsonObject look(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        float yaw = (float) number(payload, "yaw", player.getYRot());
        float pitch = (float) Math.max(-90.0D, Math.min(90.0D, number(payload, "pitch", player.getXRot())));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        JsonObject result = new JsonObject();
        result.addProperty("yaw", yaw);
        result.addProperty("pitch", pitch);
        return result;
    }

    private JsonObject selectSlot(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        int slot = integer(payload, "slot", player.getInventory().selected);
        if (slot < 0 || slot > 8) throw new IllegalArgumentException("Hotbar slot must be between 0 and 8");
        int previous = player.getInventory().selected;
        if (previous != slot) {
            var held = player.getInventory().getItem(previous).copy();
            var target = player.getInventory().getItem(slot).copy();
            player.getInventory().setItem(slot, held);
            player.getInventory().setItem(previous, target);
            player.getInventory().selected = slot;
            player.getInventory().setChanged();
        }
        JsonObject result = new JsonObject();
        result.addProperty("selected_slot", slot);
        return result;
    }

    private JsonObject swing(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        InteractionHand hand = hand(payload);
        player.swing(hand, true);
        return handResult(hand);
    }

    private JsonObject useItem(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        InteractionHand hand = hand(payload);
        RikumiSurvivalValidator.enforce(player);
        var interaction = player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
        JsonObject result = handResult(hand);
        result.addProperty("interaction_result", interaction.toString());
        return result;
    }

    private JsonObject useItemOn(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        BlockPos target = blockPos(payload, player.blockPosition());
        RikumiSurvivalValidator.assertReachable(player, target);
        InteractionHand hand = hand(payload);
        Direction face = Direction.byName(string(payload, "face", "up"));
        if (face == null) face = Direction.UP;
        Vec3 hitLocation = Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5D));
        BlockHitResult hit = new BlockHitResult(hitLocation, face, target, false);
        var interaction = player.gameMode.useItemOn(player, player.level(), player.getItemInHand(hand), hand, hit);
        JsonObject result = handResult(hand);
        result.addProperty("interaction_result", interaction.toString());
        result.addProperty("target", target.toShortString());
        return result;
    }

    private JsonObject attack(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        Entity target = targetEntity(player.serverLevel(), payload);
        RikumiSurvivalValidator.assertReachable(player, target.position());
        player.attack(target);
        player.swing(InteractionHand.MAIN_HAND, true);
        JsonObject result = new JsonObject();
        result.addProperty("entity_id", target.getId());
        result.addProperty("entity_uuid", target.getUUID().toString());
        return result;
    }

    private JsonObject interactEntity(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        Entity target = targetEntity(player.serverLevel(), payload);
        RikumiSurvivalValidator.assertReachable(player, target.position());
        InteractionHand hand = hand(payload);
        var interaction = player.interactOn(target, hand);
        JsonObject result = handResult(hand);
        result.addProperty("interaction_result", interaction.toString());
        result.addProperty("entity_id", target.getId());
        return result;
    }

    private JsonObject breakBlock(JsonObject payload) {
        FakePlayer player = actor.requirePlayer();
        BlockPos target = blockPos(payload, player.blockPosition());
        RikumiSurvivalValidator.assertCanBreak(player, target);
        boolean destroyed = player.gameMode.destroyBlock(target);
        JsonObject result = new JsonObject();
        result.addProperty("destroyed", destroyed);
        result.addProperty("target", target.toShortString());
        return result;
    }

    private JsonObject teleport(JsonObject payload) {
        if (!settings.allowUnsafeTeleport()) {
            throw new IllegalStateException("Unsafe teleport is disabled in richstuff-common.toml");
        }
        FakePlayer player = actor.requirePlayer();
        double x = number(payload, "x", player.getX());
        double y = number(payload, "y", player.getY());
        double z = number(payload, "z", player.getZ());
        player.moveTo(x, y, z, player.getYRot(), player.getXRot());
        return actor.telemetry();
    }

    private JsonObject chat(JsonObject payload) {
        String raw = string(payload, "text", "");
        RikumiChatPrivacyFilter.Decision decision = RikumiChatPrivacyFilter.filter(raw);
        if (!decision.accepted()) throw new IllegalArgumentException(decision.reason());
        Component line = Component.translatable("chat.type.text",
                Component.literal(actor.accountName()), Component.literal(decision.text()));
        for (var recipient : server.getPlayerList().getPlayers()) recipient.sendSystemMessage(line);
        JsonObject result = new JsonObject();
        result.addProperty("text", decision.text());
        return result;
    }

    private JsonObject speak(JsonObject payload) {
        String text = string(payload, "text", "");
        RikumiChatPrivacyFilter.Decision decision = RikumiChatPrivacyFilter.filter(text);
        if (!decision.accepted()) throw new IllegalArgumentException(decision.reason());
        String preset = string(payload, "preset", "neutral");
        String soundEvent = string(payload, "sound_event", "");
        float volume = (float) number(payload, "volume", 0.8D);
        float pitch = (float) number(payload, "pitch", 1.0D);
        return RikumiSpeechService.speak(decision.text(), preset, soundEvent, volume, pitch);
    }

    private Entity targetEntity(ServerLevel level, JsonObject payload) {
        Entity target = null;
        if (payload != null && payload.has("entity_uuid")) {
            target = level.getEntity(parseUuid(payload.get("entity_uuid").getAsString()));
        }
        if (target == null && payload != null && payload.has("entity_id")) {
            target = level.getEntity(payload.get("entity_id").getAsInt());
        }
        if (target == null || !target.isAlive()) throw new IllegalArgumentException("Target entity is unavailable");
        if (target == actor.requirePlayer()) throw new IllegalArgumentException("Rikumi cannot target herself");
        return target;
    }

    private void sendResult(ProtocolEnvelope source, String action, boolean ok, String message, JsonObject detail) {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", "richstuff.rikumi_action_result.v1");
        payload.addProperty("action", action);
        payload.addProperty("ok", ok);
        payload.addProperty("message", message);
        payload.add("detail", detail == null ? new JsonObject() : detail);
        transport.send(ProtocolEnvelope.Type.ACTION_RESULT, payload, source.corrId());
    }

    private static BlockPos blockPos(JsonObject payload, BlockPos fallback) {
        return new BlockPos(
                integer(payload, "x", fallback.getX()),
                integer(payload, "y", fallback.getY()),
                integer(payload, "z", fallback.getZ()));
    }

    private static InteractionHand hand(JsonObject payload) {
        return "off_hand".equalsIgnoreCase(string(payload, "hand", "main_hand"))
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static JsonObject handResult(InteractionHand hand) {
        JsonObject result = new JsonObject();
        result.addProperty("hand", hand == InteractionHand.OFF_HAND ? "off_hand" : "main_hand");
        return result;
    }

    private static String string(JsonObject payload, String key, String fallback) {
        if (payload == null || !payload.has(key)) return fallback;
        JsonElement value = payload.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static int integer(JsonObject payload, String key, int fallback) {
        if (payload == null || !payload.has(key)) return fallback;
        return payload.get(key).getAsInt();
    }

    private static double number(JsonObject payload, String key, double fallback) {
        if (payload == null || !payload.has(key)) return fallback;
        return payload.get(key).getAsDouble();
    }

    private static UUID parseUuid(String raw) {
        String value = raw.trim();
        if (value.matches("[0-9a-fA-F]{32}")) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-"
                    + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }
        return UUID.fromString(value);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
