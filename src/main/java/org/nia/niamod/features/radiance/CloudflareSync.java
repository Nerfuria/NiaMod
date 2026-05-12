package org.nia.niamod.features.radiance;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.radiance.RadianceSyncEntry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Slf4j(topic = "niamod")
public class CloudflareSync {
    private static final Gson GSON = new Gson();
    private static final long RECONNECT_INTERVAL_MS = 5000L;
    private static final long PING_INTERVAL_MS = 5000L;
    private static final long MAX_REASONABLE_RTT_MS = 10000L;
    private static final String DEFAULT_WORKER_URL = "https://radiancesync.wavelink.workers.dev";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AtomicReference<RadianceSyncEntry> pendingEntry = new AtomicReference<>(null);
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile String connectedKey;
    private volatile String connectedPlayerName;
    private volatile String connectedPlayerUuid;
    private volatile long lastReconnectAttemptMs;
    private volatile long lastPingSentMs;
    private volatile long estimatedRttMs;

    private static String getString(JsonObject message, String key) {
        if (!message.has(key) || message.get(key).isJsonNull()) {
            return "";
        }
        try {
            return message.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject message, String key) {
        if (!message.has(key) || message.get(key).isJsonNull()) {
            return -1;
        }
        try {
            return message.get(key).getAsInt();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static long getLong(JsonObject message, String key) {
        if (!message.has(key) || message.get(key).isJsonNull()) {
            return -1L;
        }
        try {
            return message.get(key).getAsLong();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static boolean isValidKey(String key) {
        return key != null && !key.isBlank() && key.length() <= 64;
    }

    public void tick(String key, String playerName, String playerUuid) {
        if (!isValidKey(key) || playerName == null || playerName.isEmpty() || playerUuid == null || playerUuid.isEmpty()) {
            disconnect();
            return;
        }

        boolean targetChanged = !key.equals(connectedKey)
                || !playerName.equals(connectedPlayerName)
                || !playerUuid.equals(connectedPlayerUuid);
        if (targetChanged && webSocket != null) {
            disconnect();
        }

        long now = System.currentTimeMillis();
        if (webSocket == null && !connecting.get()) {
            if (now - lastReconnectAttemptMs >= RECONNECT_INTERVAL_MS) {
                lastReconnectAttemptMs = now;
                connect(key, playerName, playerUuid);
            }
            return;
        }

        maybeSendPing(now);
    }

    private void connect(String key, String playerName, String playerUuid) {
        if (!connecting.compareAndSet(false, true)) {
            return;
        }

        URI wsUri = workerWebSocketUri();
        if (wsUri == null) {
            connecting.set(false);
            return;
        }

        try {
            httpClient.newWebSocketBuilder()
                    .buildAsync(wsUri, new RadianceWebSocketListener(
                            ws -> handleOpen(ws, key, playerName, playerUuid),
                            (ws, payload) -> handleIncomingMessage(payload),
                            this::handleClose,
                            this::handleError
                    ))
                    .exceptionally(ex -> {
                        connecting.set(false);
                        log.warn("[RadianceSync] WebSocket connect failed: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            connecting.set(false);
            log.warn("[RadianceSync] Failed to start WS connect: {}", e.getMessage());
        }
    }

    private void handleOpen(WebSocket ws, String key, String playerName, String playerUuid) {
        webSocket = ws;
        connectedKey = key;
        connectedPlayerName = playerName;
        connectedPlayerUuid = playerUuid;
        lastPingSentMs = 0L;
        connecting.set(false);
        log.info("[RadianceSync] WebSocket connected");
        sendHello(ws, key, playerName, playerUuid);
    }

    private void handleClose(WebSocket ws, int statusCode, String reason) {
        log.info("[RadianceSync] WebSocket closed: {} {}", statusCode, reason);
        clearSocketState(ws);
        connecting.set(false);
    }

    private void handleError(WebSocket ws, Throwable error) {
        log.warn("[RadianceSync] WebSocket error: {}", error.getMessage());
        clearSocketState(ws);
        connecting.set(false);
    }

    private URI workerWebSocketUri() {
        String workerUrl = NyahConfig.getData().getRadianceSyncWorkerUrl();
        if (workerUrl == null || workerUrl.isBlank()) {
            workerUrl = DEFAULT_WORKER_URL;
        }

        try {
            URI uri = URI.create(workerUrl.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                log.warn("[RadianceSync] Invalid worker URL host");
                return defaultWorkerWebSocketUri();
            }
            String scheme = uri.getScheme();
            if ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
                String path = uri.getPath() == null ? "" : uri.getPath();
                return new URI("wss", null, host, uri.getPort(), path, null, null);
            }
            log.warn("[RadianceSync] Refusing unsupported worker URL scheme: {}", scheme);
            return defaultWorkerWebSocketUri();
        } catch (Exception exception) {
            log.warn("[RadianceSync] Invalid worker URL, using default: {}", exception.getMessage());
            return defaultWorkerWebSocketUri();
        }
    }

    private URI defaultWorkerWebSocketUri() {
        return URI.create(DEFAULT_WORKER_URL.replaceFirst("^https://", "wss://"));
    }

    private void sendHello(WebSocket ws, String key, String playerName, String playerUuid) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "hello");
        payload.addProperty("key", key);
        payload.addProperty("ign", playerName);
        payload.addProperty("uuid", playerUuid);
        sendText(ws, GSON.toJson(payload));
    }

    public void send(int tier, long remainingMs) {
        if (tier < 0 || tier > 3 || remainingMs <= 0L) {
            return;
        }
        WebSocket ws = webSocket;
        if (ws == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long senderOneWayMs = getEstimatedOneWayLatencyMs();

        JsonObject payload = new JsonObject();
        payload.addProperty("type", "cast");
        payload.addProperty("tier", tier);
        payload.addProperty("timestamp", now);
        payload.addProperty("remainingMs", remainingMs);
        payload.addProperty("senderOneWayMs", senderOneWayMs);
        sendText(ws, GSON.toJson(payload));
    }

    public RadianceSyncEntry consumePending() {
        return pendingEntry.getAndSet(null);
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public long getEstimatedOneWayLatencyMs() {
        long rttMs = estimatedRttMs;
        if (rttMs <= 0L) {
            return 0L;
        }
        return rttMs / 2L;
    }

    private void disconnect() {
        WebSocket ws = webSocket;
        webSocket = null;
        connectedKey = null;
        connectedPlayerName = null;
        connectedPlayerUuid = null;
        lastPingSentMs = 0L;
        connecting.set(false);
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect");
            } catch (Exception ignored) {
            }
        }
    }

    public void reset() {
        disconnect();
        pendingEntry.set(null);
        estimatedRttMs = 0L;
        lastReconnectAttemptMs = 0L;
    }

    private void maybeSendPing(long now) {
        WebSocket ws = webSocket;
        if (ws == null) {
            return;
        }
        if (lastPingSentMs != 0L && now - lastPingSentMs < PING_INTERVAL_MS) {
            return;
        }
        lastPingSentMs = now;

        JsonObject payload = new JsonObject();
        payload.addProperty("type", "ping");
        payload.addProperty("clientSentAt", now);
        sendText(ws, GSON.toJson(payload));
    }

    private void handleIncomingMessage(String payload) {
        try {
            JsonObject message = GSON.fromJson(payload, JsonObject.class);
            if (message == null) {
                return;
            }
            String type = getString(message, "type");
            if ("pong".equals(type)) {
                handlePong(message);
                return;
            }
            RadianceSyncEntry entry = parseEntry(message);
            if (entry != null) {
                pendingEntry.set(entry);
            }
        } catch (Exception e) {
            log.warn("[RadianceSync] Failed to parse WS message: {}", e.getMessage());
        }
    }

    private void handlePong(JsonObject message) {
        long clientSentAt = getLong(message, "clientSentAt");
        if (clientSentAt <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long rttMs = now - clientSentAt;
        if (rttMs <= 0L || rttMs > MAX_REASONABLE_RTT_MS) {
            return;
        }
        if (estimatedRttMs <= 0L) {
            estimatedRttMs = rttMs;
            return;
        }
        estimatedRttMs = Math.round((estimatedRttMs * 0.75) + (rttMs * 0.25));
    }

    private RadianceSyncEntry parseEntry(JsonObject message) {
        int tier = getInt(message, "tier");
        if (tier < 0 || tier > 3) {
            return null;
        }

        long remainingMs = getLong(message, "remainingMs");
        if (remainingMs <= 0L) {
            return null;
        }

        return new RadianceSyncEntry(tier, remainingMs, System.currentTimeMillis());
    }

    private void sendText(WebSocket ws, String payload) {
        ws.sendText(payload, true).exceptionally(ex -> {
            log.warn("[RadianceSync] WS send failed: {}", ex.getMessage());
            clearSocketState(ws);
            connecting.set(false);
            return null;
        });
    }

    private void clearSocketState(WebSocket ws) {
        if (webSocket == ws) {
            webSocket = null;
            connectedKey = null;
            connectedPlayerName = null;
            connectedPlayerUuid = null;
            lastPingSentMs = 0L;
        }
    }

}
