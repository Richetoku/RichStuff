package com.richetoku.richstuff.rikumimita.ai.actor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.UUID;

/** Immutable status payload for RichStuff's externally controlled Rikumi fake player. */
public record RikumiActorSnapshot(
        UUID accountUuid,
        String accountName,
        boolean online,
        double health,
        int hunger,
        float saturation,
        int air,
        int armor,
        int experience,
        int experienceLevel,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String dimension,
        long gameTime,
        Instant recordedAt) {

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("schema", "richstuff.rikumi_actor_status.v1");
        json.addProperty("backend", "neoforge_fake_player");
        json.addProperty("account_uuid", accountUuid.toString());
        json.addProperty("account_name", accountName);
        json.addProperty("online", online);
        json.addProperty("health", health);
        json.addProperty("hunger", hunger);
        json.addProperty("saturation", saturation);
        json.addProperty("air", air);
        json.addProperty("armor", armor);
        json.addProperty("experience", experience);
        json.addProperty("experience_level", experienceLevel);
        JsonArray position = new JsonArray();
        position.add(x);
        position.add(y);
        position.add(z);
        json.add("position", position);
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        json.addProperty("dimension", dimension);
        json.addProperty("game_time", gameTime);
        json.addProperty("recorded_at", recordedAt.toString());
        return json;
    }

    public static RikumiActorSnapshot offline(UUID uuid, String name, long gameTime) {
        return new RikumiActorSnapshot(uuid, name, false, 0.0D, 0, 0.0F, 0, 0, 0, 0,
                0.0D, 0.0D, 0.0D, 0.0F, 0.0F, "minecraft:overworld", gameTime, Instant.now());
    }
}
