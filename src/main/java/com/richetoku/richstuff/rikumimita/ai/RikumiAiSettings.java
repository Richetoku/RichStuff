package com.richetoku.richstuff.rikumimita.ai;

import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.RichStuffConfig;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/** Resolved RichStuff configuration for Rikumi's externally controlled fake-player actor. */
public record RikumiAiSettings(
        boolean enabled,
        boolean autoSpawn,
        boolean mirrorAvatar,
        boolean allowUnsafeTeleport,
        UUID accountUuid,
        String accountName,
        String apiUrl,
        String apiToken) {

    public static final String ENV_ACCOUNT_UUID = "RIKUMI_ACCOUNT_UUID";
    public static final String ENV_ACCOUNT_NAME = "RIKUMI_ACCOUNT_NAME";
    public static final String ENV_API_URL = "COMPANION_API_URL";
    public static final String ENV_API_TOKEN = "COMPANION_API_TOKEN";

    private static final UUID DEFAULT_ACCOUNT_UUID = UUID.nameUUIDFromBytes(
            "RichStuff:RikumiMita".getBytes(StandardCharsets.UTF_8));

    public static RikumiAiSettings load() {
        String accountName = envOrConfig(ENV_ACCOUNT_NAME, RichStuffConfig.RIKUMI_AI_ACCOUNT_NAME.get(), "RikumiMita");
        String rawUuid = envOrConfig(ENV_ACCOUNT_UUID, RichStuffConfig.RIKUMI_AI_ACCOUNT_UUID.get(), "");
        UUID accountUuid = rawUuid.isBlank() ? DEFAULT_ACCOUNT_UUID : parseUuid(rawUuid);
        String apiUrl = envOrConfig(ENV_API_URL, RichStuffConfig.RIKUMI_AI_API_URL.get(), "");
        String apiToken = envOrConfig(ENV_API_TOKEN, RichStuffConfig.RIKUMI_AI_API_TOKEN.get(), "");
        return new RikumiAiSettings(
                RichStuffConfig.RIKUMI_AI_ENABLED.get(),
                RichStuffConfig.RIKUMI_AI_AUTO_SPAWN.get(),
                RichStuffConfig.RIKUMI_AI_MIRROR_AVATAR.get(),
                RichStuffConfig.RIKUMI_AI_ALLOW_UNSAFE_TELEPORT.get(),
                accountUuid,
                accountName,
                apiUrl,
                apiToken);
    }

    public boolean hasTransport() {
        return !apiUrl.isBlank() && !apiToken.isBlank();
    }

    private static String envOrConfig(String envName, String configured, String fallback) {
        String environment = System.getenv(envName);
        if (environment != null && !environment.isBlank()) return environment.trim();
        if (configured != null && !configured.isBlank()) return configured.trim();
        return fallback;
    }

    private static UUID parseUuid(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("[0-9a-f]{32}")) {
            normalized = normalized.substring(0, 8) + "-" + normalized.substring(8, 12) + "-"
                    + normalized.substring(12, 16) + "-" + normalized.substring(16, 20) + "-"
                    + normalized.substring(20);
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            RichStuff.LOGGER.error("Invalid Rikumi AI account UUID '{}'; using the deterministic RichStuff UUID.", raw);
            return DEFAULT_ACCOUNT_UUID;
        }
    }
}
