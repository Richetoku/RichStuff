package com.richetoku.richstuff.rikumimita.ai.transport;

import java.util.concurrent.ThreadLocalRandom;

/** Exponential reconnect backoff with ±25% jitter, capped at 30 seconds. */
public final class ReconnectStrategy {
    private static final long[] BASE_BACKOFF_MS = {1000L, 2000L, 4000L, 8000L, 16000L, 30000L};
    private int attemptIndex;

    public synchronized long nextDelayMs() {
        long base = BASE_BACKOFF_MS[Math.min(attemptIndex, BASE_BACKOFF_MS.length - 1)];
        long jitter = (long) (base * 0.25D * ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D));
        attemptIndex++;
        return Math.max(0L, base + jitter);
    }

    public synchronized void reset() {
        attemptIndex = 0;
    }
}
