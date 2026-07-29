package com.richetoku.richstuff.rikumimita.ai.transport;

import com.google.gson.JsonObject;
import com.richetoku.richstuff.rikumimita.ai.util.AuditLogger;
import com.richetoku.richstuff.rikumimita.ai.util.JsonCodec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Authenticated WebSocket transport for Rikumi's external AI controller. */
public final class CompanionApiClient {
    private static final Logger LOG = LoggerFactory.getLogger(CompanionApiClient.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;
    private static final long DRAIN_INTERVAL_MILLIS = 50L;

    private final URI connectUri;
    private final String endpointLabel;
    private final String serverRegistrationJson;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10L))
            .build();
    private final SeqAckManager outboundSeq = new SeqAckManager();
    private final SeqAckManager inboundSeq = new SeqAckManager();
    private final OutboundQueue outboundQueue = new OutboundQueue();
    private final ReconnectStrategy reconnect = new ReconnectStrategy();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rikumi-companion-transport");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean reconnectPending = new AtomicBoolean(false);
    private final AtomicReference<WebSocket> wsRef = new AtomicReference<>();
    private final ConcurrentMap<String, InboundListener> listeners = new ConcurrentHashMap<>();

    public CompanionApiClient(String url, String token, JsonObject serverRegistrationPayload) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("COMPANION_API_URL must be non-blank");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("COMPANION_API_TOKEN must be non-blank");

        String normalizedUrl = url.trim();
        if (!normalizedUrl.startsWith("ws://") && !normalizedUrl.startsWith("wss://")) {
            normalizedUrl = normalizedUrl.replaceFirst("^http", "ws");
        }
        String tokenQuery = (normalizedUrl.contains("?") ? "&" : "?") + "token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        this.connectUri = URI.create(normalizedUrl + tokenQuery);
        this.endpointLabel = safeEndpoint(connectUri);

        JsonObject registration = new JsonObject();
        registration.addProperty("schema", "rikumi.server_registration.v1");
        if (serverRegistrationPayload != null) registration.add("payload", serverRegistrationPayload);
        this.serverRegistrationJson = JsonCodec.toJson(registration);
    }

    @FunctionalInterface
    public interface InboundListener {
        void onInbound(ProtocolEnvelope envelope);
    }

    public void addListener(String key, InboundListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduler.scheduleWithFixedDelay(this::drainOutbound, 0L, DRAIN_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::sendHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        scheduler.execute(this::connect);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        reconnectPending.set(false);
        WebSocket socket = wsRef.getAndSet(null);
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "rikumi-shutdown");
            } catch (RuntimeException exception) {
                LOG.debug("Error sending WebSocket close: {}", exception.toString());
                socket.abort();
            }
        }
        scheduler.shutdownNow();
    }

    /** Queues an outbound protocol message without blocking the Minecraft server thread. */
    public void send(String type, JsonObject payload, UUID correlationId) {
        int seq = outboundSeq.nextSeq();
        int receivedAck = inboundSeq.ack();
        ProtocolEnvelope envelope = ProtocolEnvelope.outbound(
                type, payload, seq, receivedAck >= 0 ? receivedAck : null, correlationId);
        boolean heartbeat = ProtocolEnvelope.Type.HEARTBEAT.equals(type);
        boolean wasFull = outboundQueue.isFull();
        boolean queued = outboundQueue.offer(envelope, heartbeat);
        if (wasFull && queued && !heartbeat) {
            audit("transport.queue_full", "Outbound queue was full; replaced the oldest pending message.",
                    AuditLogger.Severity.WARNING, correlationId);
        }
    }

    private void connect() {
        if (!running.get()) return;
        WebSocket socket = null;
        try {
            socket = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10L))
                    .buildAsync(connectUri, new WsListener())
                    .join();
            if (!running.get()) {
                socket.abort();
                return;
            }
            wsRef.set(socket);
            reconnect.reset();
            socket.sendText(serverRegistrationJson, true).join();
            audit("transport.connect", "WebSocket connected; sent server registration.",
                    AuditLogger.Severity.INFO, null);
        } catch (RuntimeException exception) {
            if (socket != null) {
                wsRef.compareAndSet(socket, null);
                socket.abort();
            }
            LOG.warn("Failed to connect to {}: {}", endpointLabel, rootMessage(exception));
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!running.get() || !reconnectPending.compareAndSet(false, true)) return;
        long delay = reconnect.nextDelayMs();
        try {
            scheduler.schedule(() -> {
                reconnectPending.set(false);
                connect();
            }, delay, TimeUnit.MILLISECONDS);
        } catch (RuntimeException exception) {
            reconnectPending.set(false);
            if (running.get()) LOG.warn("Could not schedule Rikumi transport reconnect: {}", rootMessage(exception));
        }
    }

    private void drainOutbound() {
        WebSocket socket = wsRef.get();
        if (socket == null) return;
        try {
            ProtocolEnvelope envelope;
            while ((envelope = outboundQueue.poll()) != null) {
                try {
                    socket.sendText(JsonCodec.toJson(envelope), true).join();
                    audit("transport.send", "Sent " + envelope.type() + " seq=" + envelope.seq(),
                            AuditLogger.Severity.INFO, envelope.corrId());
                } catch (RuntimeException exception) {
                    outboundQueue.offer(envelope, ProtocolEnvelope.Type.HEARTBEAT.equals(envelope.type()));
                    wsRef.compareAndSet(socket, null);
                    socket.abort();
                    LOG.warn("WebSocket send failed for seq={} type={}: {}",
                            envelope.seq(), envelope.type(), rootMessage(exception));
                    scheduleReconnect();
                    break;
                }
            }
        } catch (RuntimeException exception) {
            LOG.error("Rikumi outbound transport loop failed: {}", rootMessage(exception), exception);
        }
    }

    private void sendHeartbeat() {
        outboundQueue.offer(ProtocolEnvelope.heartbeat(outboundSeq.nextSeq()), true);
    }

    private void onInboundText(String message) {
        ProtocolEnvelope envelope;
        try {
            envelope = JsonCodec.fromJson(message, ProtocolEnvelope.class);
        } catch (RuntimeException exception) {
            LOG.warn("Dropped malformed Rikumi transport message: {}", rootMessage(exception));
            audit("transport.inbound_malformed", "Dropped malformed inbound message.",
                    AuditLogger.Severity.WARNING, null);
            return;
        }
        if (envelope == null || !ProtocolEnvelope.SCHEMA_CONST.equals(envelope.schema())) {
            LOG.warn("Dropped Rikumi transport message with wrong or missing schema.");
            return;
        }

        if (envelope.idempotencyKey() != null
                && inboundSeq.seenIdempotencyKey(envelope.idempotencyKey())) return;

        int previousHigh = inboundSeq.highestReceived();
        SeqAckManager.SeqDecision decision = inboundSeq.recordInbound(envelope.seq());
        if (decision == SeqAckManager.SeqDecision.DUPLICATE_DROP) return;
        if (decision == SeqAckManager.SeqDecision.GAP_DETECTED) {
            audit("transport.seq_gap",
                    "Sequence gap detected: expected " + (previousHigh + 1) + " but received " + envelope.seq() + ".",
                    AuditLogger.Severity.NOTICE, envelope.corrId());
        }

        if (envelope.ackRequired()) {
            inboundSeq.advanceAck(envelope.seq());
            outboundQueue.offer(ProtocolEnvelope.ack(
                    envelope.seq(), outboundSeq.nextSeq(), envelope.corrId()), false);
        }

        for (InboundListener listener : listeners.values()) {
            try {
                listener.onInbound(envelope);
            } catch (RuntimeException exception) {
                LOG.warn("Rikumi inbound listener failed for {}: {}",
                        envelope.type(), rootMessage(exception));
            }
        }
    }

    private void onWsClose(WebSocket socket, int statusCode, String reason) {
        if (!wsRef.compareAndSet(socket, null)) return;
        audit("transport.disconnect", "WebSocket closed: status=" + statusCode + " reason=" + reason,
                AuditLogger.Severity.WARNING, null);
        scheduleReconnect();
    }

    private void onWsError(WebSocket socket, Throwable error) {
        if (!wsRef.compareAndSet(socket, null)) return;
        socket.abort();
        LOG.warn("WebSocket error for {}: {}", endpointLabel, rootMessage(error));
        audit("transport.error", "WebSocket connection failed.", AuditLogger.Severity.ERROR, null);
        scheduleReconnect();
    }

    private void audit(String action, String reason, AuditLogger.Severity severity, UUID correlationId) {
        AuditLogger.log("richstuff-rikumi", AuditLogger.ActorRole.SYSTEM, action,
                "companion-api:" + endpointLabel, reason, severity, correlationId,
                null, null, null, null);
    }

    private static String safeEndpoint(URI uri) {
        StringBuilder value = new StringBuilder();
        if (uri.getScheme() != null) value.append(uri.getScheme()).append("://");
        value.append(uri.getHost() == null ? "configured-endpoint" : uri.getHost());
        if (uri.getPort() >= 0) value.append(':').append(uri.getPort());
        if (uri.getRawPath() != null && !uri.getRawPath().isBlank()) value.append(uri.getRawPath());
        return value.toString();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private final class WsListener implements WebSocket.Listener {
        private final StringBuilder pendingText = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1L);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            pendingText.append(data);
            if (last) {
                String fullMessage = pendingText.toString();
                pendingText.setLength(0);
                onInboundText(fullMessage);
            }
            webSocket.request(1L);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            onWsClose(webSocket, statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            onWsError(webSocket, error);
        }
    }
}
