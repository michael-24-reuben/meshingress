package dev.mrk.meshingress.mcp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class McpWebSocketUseCase {
    @Deprecated(forRemoval = false) // The credential generation should not take place on the client side. The server should generate and respond with creds
    public record DemoTokens(
            String accessToken,
            String secretKey,
            String authToken,
            String clientId,
            String subject
    ) {
        static DemoTokens create() {
            return new DemoTokens(
                    "access-" + UUID.randomUUID(),
                    "secret-" + UUID.randomUUID(),
                    "auth-" + UUID.randomUUID(),
                    "java-demo-client",
                    "java-demo-subject"
            );
        }
    }

    private static final Path DEFAULT_CONFIG_PATH = Path.of(
            "temp",
            "mcp-ws-demo",
            "application-mcp-ws-demo.properties"
    );
    private static final URI DEFAULT_WS_URI = URI.create("ws://100.121.15.11:8080/mcp/ws");

    public static void main(String[] args) throws Exception {
        String action = args.length == 0 ? "show" : args[0];
        McpWebSocketUseCase sample = new McpWebSocketUseCase(DEFAULT_CONFIG_PATH, DEFAULT_WS_URI);

        switch (action) {
            case "create" -> {
                DemoTokens tokens = sample.createTokens();
                sample.printTokens(tokens);
                sample.printServerCommand();
            }
            case "modify", "rotate" -> {
                DemoTokens tokens = sample.modifyTokens();
                sample.printTokens(tokens);
                System.out.println("Restart the server for the modified credentials to take effect.");
                sample.printServerCommand();
            }
            case "delete" -> {
                sample.deleteTokens();
                System.out.println("Restart the server without the deleted config to remove the credentials from runtime.");
            }
            case "ping" -> System.out.println(sample.ping(false));
            case "initialize" -> System.out.println(sample.initialize(false));
            case "tools-list" -> System.out.println(sample.listTools(false));
            case "hello" -> System.out.println(sample.callTool(
                    "helloworld.greeting.greet",
                    "{\"name\":\"Meshingress\"}",
                    false
            ));
            case "admin-tools-list" -> System.out.println(sample.adminListTools());
            case "invalid" -> System.out.println(sample.pingWithInvalidCredentials());
            case "show" -> {
                System.out.println("Config path: " + DEFAULT_CONFIG_PATH.toAbsolutePath());
                if (Files.exists(DEFAULT_CONFIG_PATH)) {
                    sample.printTokens(sample.readTokens());
                    sample.printServerCommand();
                } else {
                    System.out.println("No demo credentials found. Run: java McpWebSocketUseCase create");
                }
            }
            default -> printUsage();
        }
    }

    private final Path configPath;
    private final URI webSocketUri;
    private final HttpClient httpClient;

    public McpWebSocketUseCase(Path configPath, URI webSocketUri) {
        this.configPath = configPath;
        this.webSocketUri = webSocketUri;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public DemoTokens createTokens() throws IOException {
        DemoTokens tokens = DemoTokens.create();
        writeTokens(tokens);
        return tokens;
    }

    public DemoTokens modifyTokens() throws IOException {
        DemoTokens tokens = DemoTokens.create();
        writeTokens(tokens);
        return tokens;
    }

    public void deleteTokens() throws IOException {
        Files.deleteIfExists(configPath);
    }

    public String ping(boolean admin) throws Exception {
        return sendJsonRpc(readTokens(), "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":{}}", admin);
    }

    public String pingWithInvalidCredentials() throws Exception {
        return sendJsonRpc(
                new DemoTokens("invalid-access", "invalid-secret", "invalid-auth", "invalid-client", "invalid-subject"),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":{}}",
                false
        );
    }

    public String initialize(boolean admin) throws Exception {
        return sendJsonRpc(readTokens(), """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"java-mcp-ws-usecase","version":"0.1.0"}}}
                """, admin);
    }

    public String listTools(boolean admin) throws Exception {
        return sendJsonRpc(readTokens(), "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}", admin);
    }

    public String callTool(String name, String argumentsJson, boolean admin) throws Exception {
        String payload = """
                {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"%s","arguments":%s}}
                """.formatted(jsonString(name), argumentsJson);
        return sendJsonRpc(readTokens(), payload, admin);
    }

    public String adminListTools() throws Exception {
        return sendJsonRpc(readTokens(), """
                {"jsonrpc":"2.0","id":1,"method":"roles/tools/list","params":{"includeDisabled":true,"includePrivate":true}}
                """, true);
    }

    public String sendJsonRpc(DemoTokens tokens, String payload, boolean admin) throws Exception {
        MessageCollector listener = new MessageCollector();
        WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .header("X-Secret-Key", tokens.secretKey())
                .header("X-Auth-Token", tokens.authToken())
                .header("Mcp-Session-Id", "java-sample-session")
                .header("X-Request-Id", "java-sample-request");

        if (admin) {
            builder.header("X-Mcp-Role", "admin");
        }

        WebSocket webSocket = builder
                .buildAsync(webSocketUri, listener)
                .get(10, TimeUnit.SECONDS);

        webSocket.sendText(payload.strip(), true).get(10, TimeUnit.SECONDS);
        String response = listener.firstMessage().get(10, TimeUnit.SECONDS);
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "sample complete").get(10, TimeUnit.SECONDS);
        return response;
    }

    public DemoTokens readTokens() throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Demo token file does not exist: " + configPath.toAbsolutePath());
        }

        Properties properties = new Properties();
        try (var input = Files.newInputStream(configPath)) {
            properties.load(input);
        }

        return new DemoTokens(
                required(properties, "meshingress.mcp.auth.stub.access-token"),
                required(properties, "meshingress.mcp.auth.stub.secret-key"),
                required(properties, "meshingress.mcp.auth.stub.auth-token"),
                required(properties, "meshingress.mcp.auth.stub.client-id"),
                required(properties, "meshingress.mcp.auth.stub.subject")
        );
    }

    private void writeTokens(DemoTokens tokens) throws IOException {
        Files.createDirectories(configPath.getParent());
        Properties properties = new Properties();
        properties.setProperty("meshingress.mcp.auth.stub.enabled", "true");
        properties.setProperty("meshingress.mcp.auth.stub.access-token", tokens.accessToken());
        properties.setProperty("meshingress.mcp.auth.stub.secret-key", tokens.secretKey());
        properties.setProperty("meshingress.mcp.auth.stub.auth-token", tokens.authToken());
        properties.setProperty("meshingress.mcp.auth.stub.client-id", tokens.clientId());
        properties.setProperty("meshingress.mcp.auth.stub.subject", tokens.subject());

        try (var output = Files.newOutputStream(configPath)) {
            properties.store(output, "Meshingress MCP WebSocket demo credentials");
        }
    }

    private void printTokens(DemoTokens tokens) {
        System.out.println("Access token: " + tokens.accessToken());
        System.out.println("Secret key: " + tokens.secretKey());
        System.out.println("Auth token: " + tokens.authToken());
        System.out.println("Client id: " + tokens.clientId());
        System.out.println("Subject: " + tokens.subject());
    }

    private void printServerCommand() {
        System.out.println();
        System.out.println("Start the server with:");
        System.out.println(".\\mvnw.cmd -pl app/meshingress-server -am spring-boot:run \"-Dspring-boot.run.arguments=--spring.config.additional-location=%s\""
                .formatted(configPath.toAbsolutePath().toUri()));
        System.out.println();
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value;
    }

    private static String jsonString(String value) {
        return Objects.requireNonNull(value, "value").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  java McpWebSocketUseCase create
                  java McpWebSocketUseCase modify
                  java McpWebSocketUseCase delete
                  java McpWebSocketUseCase ping
                  java McpWebSocketUseCase initialize
                  java McpWebSocketUseCase tools-list
                  java McpWebSocketUseCase hello
                  java McpWebSocketUseCase admin-tools-list
                  java McpWebSocketUseCase invalid
                """);
    }


    private static final class MessageCollector implements WebSocket.Listener {
        private final CompletableFuture<String> firstMessage = new CompletableFuture<>();
        private final StringBuilder currentMessage = new StringBuilder();

        CompletableFuture<String> firstMessage() {
            return firstMessage;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            currentMessage.append(data);
            if (last) {
                firstMessage.complete(currentMessage.toString());
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            firstMessage.completeExceptionally(error);
        }
    }
}
