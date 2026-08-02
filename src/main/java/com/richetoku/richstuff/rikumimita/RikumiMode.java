package com.richetoku.richstuff.rikumimita;

import java.util.Locale;

/** Owner-selectable local behavior state for Rikumi Mita. */
public enum RikumiMode {
    STAY("Stay"),
    FOLLOW("Follow"),
    ASSIST("Assist"),
    AUTO("Auto"),
    PATROL("Patrol");

    private final String displayName;

    RikumiMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public RikumiMode next() {
        RikumiMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static RikumiMode byOrdinal(int ordinal) {
        RikumiMode[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static RikumiMode parse(String raw) {
        if (raw == null || raw.isBlank()) return FOLLOW;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (RikumiMode mode : values()) if (mode.name().equals(normalized)) return mode;
        return FOLLOW;
    }
}
