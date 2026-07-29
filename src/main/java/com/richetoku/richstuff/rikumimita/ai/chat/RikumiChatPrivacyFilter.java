package com.richetoku.richstuff.rikumimita.ai.chat;

import java.util.regex.Pattern;

/** Defense-in-depth filter that prevents operational data from appearing as Rikumi dialogue. */
public final class RikumiChatPrivacyFilter {
    private static final Pattern TELEMETRY = Pattern.compile(
            "\\b(token[_\\s-]?usage|cost|tokens?\\s*[=:]\\s*\\d+|seq\\s*=\\s*\\d+|corr_?id)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON = Pattern.compile("\\{[\"']schema[\"']\\s*:");
    private static final Pattern STACK_TRACE = Pattern.compile(
            "at\\s+[a-z_$][a-z0-9_$.]*\\.[a-z_$][a-z0-9_$]*\\([^)]*\\.java:\\d+\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_HEX = Pattern.compile("\\b[0-9a-fA-F]{32,}\\b");
    private static final Pattern UUID = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern COORDINATES = Pattern.compile(
            "\\(?-?\\d{1,6}\\s*,\\s*-?\\d{1,4}\\s*,\\s*-?\\d{1,6}\\)?");
    private static final int MAX_LENGTH = 1000;

    private RikumiChatPrivacyFilter() {}

    public record Decision(boolean accepted, String text, String reason) {}

    public static Decision filter(String candidate) {
        if (candidate == null || candidate.isBlank()) return new Decision(false, "", "chat.empty");
        String text = candidate.trim();
        if (TELEMETRY.matcher(text).find()) return reject("chat.telemetry_leak");
        if (JSON.matcher(text).find()) return reject("chat.json_envelope_leak");
        if (STACK_TRACE.matcher(text).find()) return reject("chat.stack_trace_leak");
        if (LONG_HEX.matcher(text).find()) return reject("chat.long_hex_leak");
        if (UUID.matcher(text).find()) return reject("chat.uuid_leak");
        if (COORDINATES.matcher(text).find()) return reject("chat.coordinates_leak");
        if (text.length() > MAX_LENGTH) text = text.substring(0, MAX_LENGTH - 1) + "…";
        return new Decision(true, text, "");
    }

    private static Decision reject(String reason) {
        return new Decision(false, "", reason);
    }
}
