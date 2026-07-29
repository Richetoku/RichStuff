package com.richetoku.richstuff.rikumimita.ai.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.time.Instant;

/** Compact Gson codec used by the integrated Rikumi WebSocket transport. */
public final class JsonCodec {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .serializeNulls()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .create();

    private JsonCodec() {}

    public static Gson compact() {
        return GSON;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    private static final class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonSyntaxException {
            return json == null || json.isJsonNull() ? null : Instant.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(Instant value, Type type, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.toString());
        }
    }
}
