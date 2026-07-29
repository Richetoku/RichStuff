package com.richetoku.richstuff.rikumimita.ai.transport;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Small bounded FIFO that keeps transport work off the Minecraft server thread.
 * Non-heartbeat traffic replaces the oldest pending message when full; a heartbeat
 * is simply skipped because a newer heartbeat will follow.
 */
public final class OutboundQueue {
    public static final int DEFAULT_CAPACITY = 1024;

    private final int capacity;
    private final Deque<ProtocolEnvelope> deque;

    public OutboundQueue() {
        this(DEFAULT_CAPACITY);
    }

    public OutboundQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0: " + capacity);
        this.capacity = capacity;
        this.deque = new ArrayDeque<>(capacity);
    }

    /** @return {@code false} only when a heartbeat was skipped because the queue was full. */
    public synchronized boolean offer(ProtocolEnvelope message, boolean heartbeat) {
        if (message == null) throw new IllegalArgumentException("message must not be null");
        if (deque.size() >= capacity) {
            if (heartbeat) return false;
            deque.pollFirst();
        }
        deque.addLast(message);
        return true;
    }

    public synchronized ProtocolEnvelope poll() {
        return deque.pollFirst();
    }

    public synchronized boolean isFull() {
        return deque.size() >= capacity;
    }
}
