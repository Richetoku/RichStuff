package com.richetoku.richstuff.rikumimita;

import java.util.Locale;

/** Client-synchronized animation/action state. */
public enum RikumiAction {
    IDLE,
    WALK,
    SIT,
    MINE,
    ATTACK,
    USE_ITEM,
    FISH,
    BUILD,
    CRAFT;

    public static RikumiAction byOrdinal(int ordinal) {
        RikumiAction[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static RikumiAction parse(String raw) {
        if (raw == null || raw.isBlank()) return IDLE;
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (RikumiAction action : values()) if (action.name().equals(normalized)) return action;
        return IDLE;
    }
}
