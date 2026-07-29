package com.richetoku.richstuff.rikumimita.ai.transport;

import java.util.LinkedHashSet;
import java.util.Set;

/** Thread-safe sequence, acknowledgement, and replay tracking for the WebSocket protocol. */
public final class SeqAckManager {
    private static final int HISTORY_LIMIT = 4096;

    private int nextOutbound = -1;
    private int highestInbound = -1;
    private int ackedUpTo = -1;
    private final Set<Integer> seenInboundSequences = new LinkedHashSet<>();
    private final Set<String> idempotencyKeys = new LinkedHashSet<>();

    public enum SeqDecision {
        ACCEPT,
        DUPLICATE_DROP,
        GAP_DETECTED
    }

    public synchronized int nextSeq() {
        return ++nextOutbound;
    }

    public synchronized int ack() {
        return ackedUpTo;
    }

    public synchronized int highestReceived() {
        return highestInbound;
    }

    public synchronized SeqDecision recordInbound(int sequence) {
        if (sequence < 0) throw new IllegalArgumentException("Inbound seq must be non-negative: " + sequence);
        if (!seenInboundSequences.add(sequence)) return SeqDecision.DUPLICATE_DROP;
        trimOldest(seenInboundSequences);

        SeqDecision decision = highestInbound >= 0 && sequence > highestInbound + 1
                ? SeqDecision.GAP_DETECTED : SeqDecision.ACCEPT;
        highestInbound = Math.max(highestInbound, sequence);
        return decision;
    }

    public synchronized void advanceAck(int sequence) {
        ackedUpTo = Math.max(ackedUpTo, sequence);
    }

    public synchronized boolean seenIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return false;
        if (!idempotencyKeys.add(key)) return true;
        trimOldest(idempotencyKeys);
        return false;
    }

    private static <T> void trimOldest(Set<T> values) {
        if (values.size() <= HISTORY_LIMIT) return;
        var iterator = values.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }
}
