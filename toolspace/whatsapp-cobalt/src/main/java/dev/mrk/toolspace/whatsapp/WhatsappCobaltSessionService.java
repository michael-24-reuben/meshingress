package dev.mrk.toolspace.whatsapp;

import it.auties.whatsapp.api.PairingCodeHandler;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.WebHistorySetting;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WhatsappCobaltSessionService {

    static final String DEFAULT_ALIAS = "meshingress";
    static final String DEFAULT_DEVICE_NAME = "Meshingress";
    static final int DEFAULT_PAIRING_TIMEOUT_MS = 30_000;
    static final int DEFAULT_CONNECT_TIMEOUT_MS = 45_000;
    static final int DEFAULT_SEND_TIMEOUT_MS = 30_000;

    private final ObjectMapper objectMapper;

    private volatile Whatsapp whatsapp;
    private volatile CompletableFuture<Whatsapp> loginFuture;
    private volatile String alias = DEFAULT_ALIAS;
    private volatile String lastEvent = "idle";
    private volatile String lastDisconnectReason;
    private volatile Instant startedAt;
    private volatile Instant loggedInAt;

    public WhatsappCobaltSessionService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ObjectNode status() {
        Whatsapp client = whatsapp;
        ObjectNode status = objectMapper.createObjectNode();
        status.put("alias", alias);
        status.put("hasActiveClient", client != null);
        status.put("connected", client != null && client.isConnected());
        status.put("lastEvent", lastEvent);
        putNullable(status, "lastDisconnectReason", lastDisconnectReason);
        putNullable(status, "startedAt", startedAt == null ? null : startedAt.toString());
        putNullable(status, "loggedInAt", loggedInAt == null ? null : loggedInAt.toString());
        putNullable(status, "localJid", localJid(client).orElse(null));
        status.put("historySetting", "discard");
        status.put("conversationReadSurfaceExposed", false);
        status.put("note", "This tool exposes pairing, status, send_text, and disconnect only. It does not expose chat or message readers.");
        return status;
    }

    public ObjectNode startPairing(WhatsappStartPairingArgs args) throws Exception {
        String mode = normalizeMode(args == null ? null : args.mode());
        String requestedAlias = normalizeAlias(args == null ? null : args.alias());
        String deviceName = normalizeDeviceName(args == null ? null : args.deviceName());
        int timeoutMs = normalizeTimeout(args == null ? null : args.timeoutMs(), DEFAULT_PAIRING_TIMEOUT_MS);

        CompletableFuture<String> challenge = new CompletableFuture<>();
        Whatsapp client;

        synchronized (this) {
            disconnectQuietly();
            alias = requestedAlias;
            startedAt = Instant.now();
            loggedInAt = null;
            lastDisconnectReason = null;
            lastEvent = "pairing_started";

            var builder = Whatsapp.webBuilder()
                    .newConnection(requestedAlias)
                    .name(deviceName)
                    .historySetting(WebHistorySetting.discard(false))
                    .automaticMessageReceipts(false);

            if ("qr".equals(mode)) {
                client = builder.unregistered(QrHandler.toPlainString(qr -> completeChallenge(challenge, "qr_received", qr)));
            } else {
                long phoneNumber = parsePhoneNumber(args == null ? null : args.phoneNumber());
                PairingCodeHandler handler = code -> completeChallenge(challenge, "pairing_code_received", code);
                client = builder.unregistered(phoneNumber, handler);
            }

            this.whatsapp = attachListeners(client);
            this.loginFuture = client.connect();
            this.loginFuture.whenComplete((ignored, error) -> {
                if (error != null) {
                    lastEvent = "connect_failed";
                }
            });
        }

        String value = challenge.get(timeoutMs, TimeUnit.MILLISECONDS);

        ObjectNode result = status();
        result.put("mode", mode);
        result.put("verificationValue", value);
        result.put("verificationValueType", "qr".equals(mode) ? "raw_qr_payload" : "pairing_code");
        result.put("message", "Pairing started. Complete linking in WhatsApp; status will move to connected after login succeeds.");
        return result;
    }

    public ObjectNode connectRegistered(WhatsappConnectArgs args) throws Exception {
        String requestedAlias = normalizeAlias(args == null ? null : args.alias());
        String deviceName = normalizeDeviceName(args == null ? null : args.deviceName());
        int timeoutMs = normalizeTimeout(args == null ? null : args.timeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);

        Whatsapp client;
        synchronized (this) {
            disconnectQuietly();
            alias = requestedAlias;
            startedAt = Instant.now();
            loggedInAt = null;
            lastDisconnectReason = null;
            lastEvent = "registered_connect_started";

            Optional<Whatsapp> existing = Whatsapp.webBuilder()
                    .newConnection(requestedAlias)
                    .name(deviceName)
                    .historySetting(WebHistorySetting.discard(false))
                    .automaticMessageReceipts(false)
                    .registered();

            if (existing.isEmpty()) {
                lastEvent = "registered_session_missing";
                throw new IllegalStateException("No registered WhatsApp session exists for alias: " + requestedAlias);
            }

            client = attachListeners(existing.get());
            this.whatsapp = client;
            this.loginFuture = client.connect();
        }

        clientFromFuture(loginFuture, timeoutMs);
        ObjectNode result = status();
        result.put("message", "Registered WhatsApp session connected.");
        return result;
    }

    public ObjectNode sendText(WhatsappSendTextArgs args) throws Exception {
        if (args == null) {
            throw new IllegalArgumentException("arguments are required");
        }
        if (args.recipient() == null || args.recipient().isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (args.text() == null || args.text().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }

        Whatsapp client = requireConnectedClient();
        Jid recipient = parseRecipient(args.recipient());
        int timeoutMs = normalizeTimeout(args.timeoutMs(), DEFAULT_SEND_TIMEOUT_MS);

        MessageInfo<?> info = client.sendMessage(recipient, args.text())
                .get(timeoutMs, TimeUnit.MILLISECONDS);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "sent");
        result.put("messageId", info.id());
        result.put("recipient", recipient.toString());
        result.put("conversationReadSurfaceExposed", false);
        putNullable(result, "localJid", localJid(client).orElse(null));
        return result;
    }

    public ObjectNode disconnect(WhatsappDisconnectArgs args) throws Exception {
        Whatsapp client = whatsapp;
        if (client == null) {
            lastEvent = "idle";
            return status().put("message", "No active WhatsApp client was present.");
        }

        boolean logout = args != null && Boolean.TRUE.equals(args.logout());
        int timeoutMs = normalizeTimeout(args == null ? null : args.timeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
        if (logout) {
            client.logout().get(timeoutMs, TimeUnit.MILLISECONDS);
            lastEvent = "logged_out";
        } else {
            client.disconnect().get(timeoutMs, TimeUnit.MILLISECONDS);
            lastEvent = "disconnected";
        }

        ObjectNode result = status();
        result.put("message", logout ? "WhatsApp session logged out." : "WhatsApp session disconnected.");
        return result;
    }

    private Whatsapp attachListeners(Whatsapp client) {
        return client
                .addLoggedInListener(api -> {
                    loggedInAt = Instant.now();
                    lastEvent = "logged_in";
                })
                .addDisconnectedListener(reason -> {
                    lastDisconnectReason = reason == null ? null : reason.name();
                    lastEvent = "disconnected";
                });
    }

    private void completeChallenge(CompletableFuture<String> challenge, String event, String value) {
        lastEvent = event;
        challenge.complete(value);
    }

    private Whatsapp requireConnectedClient() {
        Whatsapp client = whatsapp;
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("WhatsApp is not connected. Run connect_registered or start_pairing first.");
        }
        return client;
    }

    private void disconnectQuietly() {
        Whatsapp client = whatsapp;
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect().get(5, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
            // Starting a new session should not fail because cleanup of a stale session failed.
        }
    }

    private static Whatsapp clientFromFuture(CompletableFuture<Whatsapp> future, int timeoutMs) throws Exception {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            throw new TimeoutException("Timed out waiting for WhatsApp login after " + timeoutMs + "ms");
        }
    }

    static String normalizeMode(String value) {
        if (value == null || value.isBlank()) {
            return "pairing_code";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!"pairing_code".equals(normalized) && !"qr".equals(normalized)) {
            throw new IllegalArgumentException("mode must be pairing_code or qr");
        }
        return normalized;
    }

    static String normalizeAlias(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ALIAS;
        }
        return value.trim();
    }

    static String normalizeDeviceName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DEVICE_NAME;
        }
        return value.trim();
    }

    static int normalizeTimeout(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value < 1_000 || value > 180_000) {
            throw new IllegalArgumentException("timeoutMs must be between 1000 and 180000");
        }
        return value;
    }

    static long parsePhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("phoneNumber is required for pairing_code mode");
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("phoneNumber must contain digits");
        }
        return Long.parseUnsignedLong(normalized);
    }

    static Jid parseRecipient(String value) {
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (normalized.contains("@")) {
            return Jid.of(normalized);
        }
        return Jid.of(normalized.replaceAll("[^0-9]", ""));
    }

    private static Optional<String> localJid(Whatsapp client) {
        if (client == null) {
            return Optional.empty();
        }
        return client.store().jid().map(Jid::toString);
    }

    private static void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }
}
