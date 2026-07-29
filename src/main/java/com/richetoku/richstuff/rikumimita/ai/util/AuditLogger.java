package com.richetoku.richstuff.rikumimita.ai.util;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Emits structured, best-effort audit records for Rikumi's external control transport. */
public final class AuditLogger {
    public static final String LOGGER_NAME = "rikumi.audit";
    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private AuditLogger() {}

    public enum Severity {
        INFO("info"), NOTICE("notice"), WARNING("warning"), ERROR("error"), CRITICAL("critical");

        private final String wire;

        Severity(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public enum ActorRole {
        SYSTEM("system");

        private final String wire;

        ActorRole(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public static void log(String actor, ActorRole role, String action, String target,
                           String reason, Severity severity, UUID corrId,
                           JsonObject before, JsonObject after,
                           String ip, String userAgent) {
        try {
            JsonObject event = new JsonObject();
            event.addProperty("schema", "rikumi.audit_event.v1");
            event.addProperty("id", UUID.randomUUID().toString());
            event.addProperty("timestamp", Instant.now().toString());
            event.addProperty("actor", actor == null ? "system" : actor);
            nullable(event, "actor_role", role == null ? null : role.wire());
            event.addProperty("action", truncate(action, 100));
            event.addProperty("target", target == null ? "" : target);
            nullable(event, "before", before);
            nullable(event, "after", after);
            event.addProperty("reason", truncate(reason, 1000));
            nullable(event, "corr_id", corrId == null ? null : corrId.toString());
            nullable(event, "ip", ip);
            nullable(event, "user_agent", userAgent);
            event.addProperty("severity", severity.wire());

            String json = JsonCodec.compact().toJson(event);
            switch (severity) {
                case CRITICAL, ERROR -> LOG.error(json);
                case WARNING -> LOG.warn(json);
                case NOTICE, INFO -> LOG.info(json);
            }
        } catch (RuntimeException exception) {
            LOG.error("Failed to emit audit event (action={}, target={}): {}",
                    action, target, exception.toString());
        }
    }

    private static void nullable(JsonObject object, String key, String value) {
        if (value == null) object.add(key, JsonNull.INSTANCE);
        else object.addProperty(key, value);
    }

    private static void nullable(JsonObject object, String key, JsonObject value) {
        if (value == null) object.add(key, JsonNull.INSTANCE);
        else object.add(key, value);
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
