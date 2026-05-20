package dev.mrk.meshingress.application;

import dev.mrk.meshingress.auth.AuthStoreRegistry;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = McpWebSocketApplicationUseCaseTestsV1.StorePathInitializer.class)
class McpWebSocketApplicationUseCaseTestsV1 {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private AuthStoreRegistry authStoreRegistry;

    @Contract(" -> new")
    private @NonNull URI baseHttpUri() {
        return URI.create("http://100.121.15.11:" + port);
    }

    @Contract(" -> new")
    private @NonNull URI baseWebSocketUri() {
        return URI.create("ws://100.121.15.11:" + port + "/mcp/ws");
    }

    static class StorePathInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "meshingress.auth.store.path=target/test-auth-store/ws-application-usecase-"
                            + UUID.randomUUID()
                            + ".json"
            ).applyTo(applicationContext);
        }
    }

    @Test
    void firstAdminCanRegisterThenUseWebSocketAsAdminAndClient() throws Exception {
        MeshingressUseCase useCase = new MeshingressUseCase(httpClient, objectMapper, baseHttpUri(), baseWebSocketUri());

        printSection("Server Targets");
        System.out.println("HTTP base: " + baseHttpUri());
        System.out.println("WebSocket: " + baseWebSocketUri());
        System.out.println("Auth store bootstrap user: " + authStoreRegistry.bootstrapAdmin().username());

        McpCredentials adminCredentials = useCase.registerFirstAdmin(new FirstAdminRequest(
                "root",
                authStoreRegistry.bootstrapAdmin().password(),
                "new-root-password",
                "new-root-password",
                "root@example.test",
                "Root Admin"
        ));
        printSection("Generated MCP Credentials");
        System.out.println("Access token: " + adminCredentials.accessToken());
        System.out.println("Secret key: " + adminCredentials.secretKey());
        System.out.println("Auth token: " + adminCredentials.authToken());
        System.out.println("Client id: " + adminCredentials.clientId());
        System.out.println("Subject: " + adminCredentials.subject());

        try (McpWebSocketSession admin = useCase.openWebSocket(adminCredentials, true)) {
            admin.initialize();
            admin.listTools();
            admin.callTool("helloworld.greet", "{\"name\":\"Meshingress\"}");
            admin.adminListTools(true, true);
            admin.adminCheckToolAlias("architect.entries.copy");
            admin.adminRegisterToolAlias("architect.entries.copy");
            admin.adminUpdateToolAlias("architect.entries.copy");
            admin.adminDisableToolAlias("architect.entries.copy");
            admin.adminReloadTools();
        }

        try (McpWebSocketSession client = useCase.openWebSocket(adminCredentials, false)) {
            client.ping();
            client.listTools();
            client.callTool("helloworld.greet", "{\"name\":\"Client\"}");
        }

        useCase.createClientCredentials("future-client");
        useCase.rotateClientCredentials("future-client");
    }


    record FirstAdminRequest(
            String username,
            String bootstrapPassword,
            String newPassword,
            String confirmationPassword,
            String email,
            String displayName
    ) {
    }

    record McpCredentials(
            String accessToken,
            String secretKey,
            String authToken,
            String clientId,
            String subject
    ) {
    }

    static class MeshingressUseCase {

        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final URI httpBaseUri;
        private final URI webSocketUri;

        MeshingressUseCase(
                HttpClient httpClient,
                ObjectMapper objectMapper,
                URI httpBaseUri,
                URI webSocketUri
        ) {
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
            this.httpBaseUri = httpBaseUri;
            this.webSocketUri = webSocketUri;
        }

        McpCredentials registerFirstAdmin(FirstAdminRequest request) throws Exception {
            printSection("HTTP Register First Admin");
            String body = """
                    {
                      "username": "%s",
                      "bootstrapPassword": "%s",
                      "newPassword": "%s",
                      "confirmationPassword": "%s",
                      "email": "%s",
                      "displayName": "%s"
                    }
                    """.formatted(
                    jsonString(request.username()),
                    jsonString(request.bootstrapPassword()),
                    jsonString(request.newPassword()),
                    jsonString(request.confirmationPassword()),
                    jsonString(request.email()),
                    jsonString(request.displayName())
            );
            System.out.println("POST " + httpBaseUri.resolve("/api/v1/auth/bootstrap/admin"));
            System.out.println(body.strip());
            HttpRequest httpRequest = HttpRequest.newBuilder(httpBaseUri.resolve("/api/v1/auth/bootstrap/admin"))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("HTTP " + response.statusCode());
            System.out.println(prettyOrRaw(objectMapper, response.body()));

            JsonNode generatedAuth = objectMapper.readTree(response.body()).path("generatedAuth");
            return new McpCredentials(
                    generatedAuth.path("accessToken").asString(),
                    generatedAuth.path("secretKey").asString(),
                    generatedAuth.path("authToken").asString(),
                    generatedAuth.path("clientId").asString(),
                    generatedAuth.path("subject").asString()
            );
        }

        McpWebSocketSession openWebSocket(McpCredentials credentials, boolean admin) throws Exception {
            printSection(admin ? "Open Admin WebSocket" : "Open Client WebSocket");
            System.out.println("WS " + webSocketUri);
            System.out.println("Authorization: Bearer " + credentials.accessToken());
            System.out.println("X-Secret-Key: " + credentials.secretKey());
            System.out.println("X-Auth-Token: " + credentials.authToken());
            System.out.println("X-Mcp-Role: " + (admin ? "admin" : "<not sent>"));
            MessageCollector listener = new MessageCollector();
            WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.accessToken())
                    .header("X-Secret-Key", credentials.secretKey())
                    .header("X-Auth-Token", credentials.authToken())
                    .header("Mcp-Session-Id", "java-usecase-session")
                    .header("X-Request-Id", "java-usecase-request");
            if (admin) {
                builder.header("X-Mcp-Role", "admin");
            }

            WebSocket webSocket = builder
                    .buildAsync(webSocketUri, listener)
                    .get(10, TimeUnit.SECONDS);
            return new McpWebSocketSession(objectMapper, webSocket, listener);
        }

        void createClientCredentials(String clientName) {
            printSection("Create Client Credentials");
            System.out.println("Requested client: " + clientName);
            System.out.println("Not implemented yet: no Meshingress HTTP or MCP method currently creates additional MCP clients.");
        }

        void rotateClientCredentials(String clientId) {
            printSection("Rotate Client Credentials");
            System.out.println("Requested client: " + clientId);
            System.out.println("Not implemented yet: no Meshingress HTTP or MCP method currently rotates MCP client credentials.");
        }
    }

    static class McpWebSocketSession implements AutoCloseable {

        private final ObjectMapper objectMapper;
        private final WebSocket webSocket;
        private final MessageCollector listener;
        private int id = 1;

        McpWebSocketSession(ObjectMapper objectMapper, WebSocket webSocket, MessageCollector listener) {
            this.objectMapper = objectMapper;
            this.webSocket = webSocket;
            this.listener = listener;
        }

        JsonNode initialize() throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"java-websocket-usecase","version":"0.1.0"}}}
                    """.formatted(nextId()));
        }

        JsonNode ping() throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"ping","params":{}}
                    """.formatted(nextId()));
        }

        JsonNode listTools() throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"tools/list","params":{}}
                    """.formatted(nextId()));
        }

        JsonNode callTool(String name, String argumentsJson) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"tools/call","params":{"name":"%s","arguments":%s}}
                    """.formatted(nextId(), jsonString(name), argumentsJson));
        }

        JsonNode adminListTools(boolean includeDisabled, boolean includePrivate) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/list","params":{"includeDisabled":%s,"includePrivate":%s}}
                    """.formatted(nextId(), includeDisabled, includePrivate));
        }

        JsonNode adminCheckToolAlias(String name) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/check","params":{"tool":%s}}
                    """.formatted(nextId(), toolAliasJson(name)));
        }

        JsonNode adminRegisterToolAlias(String name) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/register","params":{"tool":%s}}
                    """.formatted(nextId(), toolAliasJson(name)));
        }

        JsonNode adminUpdateToolAlias(String name) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/update","params":{"name":"%s","patch":{"description":"List architect entries through a WebSocket use-case alias.","enabled":true}}}
                    """.formatted(nextId(), jsonString(name)));
        }

        JsonNode adminDisableToolAlias(String name) throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/delete","params":{"name":"%s","mode":"disable"}}
                    """.formatted(nextId(), jsonString(name)));
        }

        JsonNode adminReloadTools() throws Exception {
            return send("""
                    {"jsonrpc":"2.0","id":%d,"method":"roles/tools/reload","params":{}}
                    """.formatted(nextId()));
        }

        private JsonNode send(String payload) throws Exception {
            printSection("WebSocket JSON-RPC Request");
            System.out.println(payload.strip());

            CompletableFuture<String> response = listener.nextMessage();
            webSocket.sendText(payload.strip(), true).get(10, TimeUnit.SECONDS);

            String responseBody = response.get(10, TimeUnit.SECONDS);
            printSection("WebSocket JSON-RPC Response");
            System.out.println(prettyOrRaw(objectMapper, responseBody));

            return objectMapper.readTree(responseBody);
        }

        private int nextId() {
            return id++;
        }

        @Override
        public void close() throws Exception {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "usecase complete").get(10, TimeUnit.SECONDS);
        }
    }

    static class MessageCollector implements WebSocket.Listener {

        private CompletableFuture<String> nextMessage = new CompletableFuture<>();
        private final StringBuilder currentMessage = new StringBuilder();

        synchronized CompletableFuture<String> nextMessage() {
            if (nextMessage.isDone()) {
                nextMessage = new CompletableFuture<>();
            }
            currentMessage.setLength(0);
            return nextMessage;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            currentMessage.append(data);
            if (last) {
                nextMessage.complete(currentMessage.toString());
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
            nextMessage.completeExceptionally(error);
        }
    }


    private static @NonNull String toolAliasJson(String name) {
        return """
                {
                  "name": "%s",
                  "title": "List Architect Entries Alias",
                  "description": "List architect entries through a WebSocket use-case alias.",
                  "enabled": true,
                  "visibility": "public",
                  "handlerKey": "architect.entries.list",
                  "inputSchema": {
                    "type": "object",
                    "properties": {
                      "status": {
                        "type": "string"
                      }
                    },
                    "additionalProperties": false
                  }
                }
                """.formatted(jsonString(name));
    }

    private static @NonNull String jsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void printSection(String label) {
        System.out.println();
        System.out.println("=== " + label + " ===");
    }

    private static String prettyOrRaw(ObjectMapper objectMapper, String body) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(body));
        } catch (Exception exception) {
            return body;
        }
    }
}
