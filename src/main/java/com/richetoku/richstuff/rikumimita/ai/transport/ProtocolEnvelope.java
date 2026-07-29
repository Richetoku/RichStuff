/*
 * RichStuff — Rikumi AI Integration — transport/ProtocolEnvelope
 * Authored in M2; integrated into the RichStuff Java 21 build.
 */
package com.richetoku.richstuff.rikumimita.ai.transport;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Java record mirroring the {@code protocol-envelope.v1} JSON Schema.
 *
 * <p>Every WebSocket message between the mod and companion-api is wrapped in
 * this envelope. The envelope carries:
 * <ul>
 *   <li>identity ({@code msg_id}, {@code corr_id}) — UUIDv4</li>
 *   <li>ordering ({@code seq}, {@code ack}) — monotonic per direction</li>
 *   <li>reliability ({@code ack_required}, {@code idempotency_key},
 *       {@code deadline_ms})</li>
 *   <li>capability negotiation ({@code capabilities})</li>
 *   <li>payload ({@code payload}) — the concrete message body, dispatched by
 *       {@code type} on the receiver</li>
 *   <li>timing ({@code ts}) — ISO-8601 UTC</li>
 * </ul>
 *
 * <p>The shared {@link com.richetoku.richstuff.rikumimita.ai.util.JsonCodec}
 * applies lower-case-with-underscores field naming, so record components use
 * the protocol's snake-case wire names in both directions.
 *
 * @see <a href="schemas/protocol-envelope.v1.json">…/schemas/protocol-envelope.v1.json</a>
 */
public record ProtocolEnvelope(
        String schema,
        String type,
        UUID msgId,
        UUID corrId,
        int seq,
        Integer ack,
        boolean ackRequired,
        String idempotencyKey,
        JsonObject payload,
        int deadlineMs,
        List<String> capabilities,
        Instant ts
) {

    /** Wire value of the {@code schema} field — matches the JSON Schema const. */
    public static final String SCHEMA_CONST = "rikumi.protocol_envelope.v1";

    /** Default deadline (ms) for non-ack messages. */
    public static final int DEFAULT_DEADLINE_MS = 5000;

    /** Canonical message types used by the mod → companion-api direction. */
    public static final class Type {
        public static final String ACTOR_STATUS = "actor_status.v1";
        public static final String CHAT_MESSAGE = "chat_message.v1";
        public static final String ACTOR_CONTROL = "actor_control.v1";
        public static final String ACTION_RESULT = "action_result.v1";
        public static final String HEARTBEAT = "heartbeat.v1";
        public static final String ACK = "ack.v1";

        private Type() {
            throw new IllegalStateException("Constant class — not instantiable");
        }
    }

    /**
     * Canonical factory for outbound messages. Caller supplies the type,
     * payload, seq, ack, and corrId; everything else is filled in with
     * schema-compliant defaults.
     */
    public static ProtocolEnvelope outbound(String type, JsonObject payload,
                                            int seq, Integer ack, UUID corrId) {
        return new ProtocolEnvelope(
                SCHEMA_CONST,
                type,
                UUID.randomUUID(),
                corrId == null ? UUID.randomUUID() : corrId,
                seq,
                ack,
                false,
                null,
                payload == null ? new JsonObject() : payload,
                DEFAULT_DEADLINE_MS,
                List.of(),
                Instant.now());
    }

    /**
     * Ack-only message — {@code type=ack.v1}, payload empty, {@code ack}
     * field set to the seq being acknowledged.
     */
    public static ProtocolEnvelope ack(int ackSeq, int ourSeq, UUID corrId) {
        return new ProtocolEnvelope(
                SCHEMA_CONST,
                Type.ACK,
                UUID.randomUUID(),
                corrId == null ? UUID.randomUUID() : corrId,
                ourSeq,
                ackSeq,
                false,
                null,
                new JsonObject(),
                DEFAULT_DEADLINE_MS,
                List.of(),
                Instant.now());
    }

    /**
     * Heartbeat — empty payload, current seq, no ack. Used by the
     * {@link CompanionApiClient} every 15s to keep the WS alive.
     */
    public static ProtocolEnvelope heartbeat(int ourSeq) {
        return new ProtocolEnvelope(
                SCHEMA_CONST,
                Type.HEARTBEAT,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ourSeq,
                null,
                false,
                null,
                new JsonObject(),
                1000,
                List.of(),
                Instant.now());
    }
}
