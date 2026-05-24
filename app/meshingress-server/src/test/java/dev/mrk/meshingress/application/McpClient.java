package dev.mrk.meshingress.application;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Simple standalone MCP HTTP client. This is not a test; run it against a running server
 * (default http://localhost:8080) and it will print the HTTP status and response body for
 * a set of representative MCP requests.
 */
public class McpClient {

    private final HttpClient client;
    private final String baseUrl;

    public McpClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://100.121.15.11:4737";
        McpClient c = new McpClient(url);

        // Mirror the original test requests but as live client calls
        c.printResponse("initializeReturnsToolCapabilities",
                c.post("/mcp", initializePayload()));

        c.printResponse("toolsListIncludesStaticArchitectTool",
                c.post("/mcp", toolsListPayload(2)));

        c.printResponse("toolsListIncludesAttachedHelloWorldModule",
                c.post("/mcp", toolsListPayload(20)));

        c.printResponse("toolsListIncludesAttachedInstaFetchModule",
                c.post("/mcp", toolsListPayload(30)));

        c.printResponse("toolsCallInvokesAttachedHelloWorldModule",
                c.post("/mcp", toolsCallHelloPayload()));

        c.printResponse("toolsCallInvokesAttachedInstaFetchModule",
                c.post("/mcp", toolsCallInstaFetchPayload()));

        c.printResponse("toolsCallReturnsArchitectEntries",
                c.post("/mcp", toolsCallArchitectPayload()));

        c.printResponse("invalidJsonRpcEnvelopeReturnsProtocolError",
                c.post("/mcp", invalidEnvelopePayload()));

        c.printResponse("roleMethodsRequireAdminAuthorization",
                c.post("/mcp", adminListPayload()));

        // Role-gated operations with Authorization header
        c.printResponse("roles/tools/check",
                c.post("/mcp", toolRequest(6, "roles/tools/check"), Map.of("Authorization", "Bearer dev-admin")));

        c.printResponse("roles/tools/register",
                c.post("/mcp", toolRequest(7, "roles/tools/register"), Map.of("Authorization", "Bearer dev-admin")));

        c.printResponse("roles/tools/update",
                c.post("/mcp", "{\n  \"jsonrpc\": \"2.0\",\n  \"id\": 8,\n  \"method\": \"roles/tools/update\",\n  \"params\": {\n    \"name\": \"architect.entries.copy\",\n    \"patch\": {\n      \"description\": \"List architect entries through a dynamic alias.\",\n      \"enabled\": true\n    }\n  }\n}\n", Map.of("Authorization", "Bearer dev-admin")));

        c.printResponse("roles/tools/delete",
                c.post("/mcp", "{\n  \"jsonrpc\": \"2.0\",\n  \"id\": 9,\n  \"method\": \"roles/tools/delete\",\n  \"params\": {\n    \"name\": \"architect.entries.copy\",\n    \"mode\": \"disable\"\n  }\n}\n", Map.of("Authorization", "Bearer dev-admin")));

        c.printResponse("GET /mcp",
                c.get("/mcp"));

        c.printResponse("DELETE /mcp",
                c.delete("/mcp"));
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return post(path, body, Map.of());
    }

    private HttpResponse<String> post(String path, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        headers.forEach(rb::header);

        HttpRequest req = rb.build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private void printResponse(String label, HttpResponse<String> response) {
        System.out.println();
        System.out.println("=== " + label + " ===");
        System.out.println("HTTP " + response.statusCode());
        System.out.println(response.body());
        System.out.println("====================");
    }

    // Payloads ported from the original test class
    private static String initializePayload() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "test-client",
                      "version": "0.1.0"
                    }
                  }
                }""";
    }

    private static String toolsListPayload(int id) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "tools/list",
                  "params": {}
                }""".formatted(id);
    }

    private static String toolsCallHelloPayload() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": 21,
                  "method": "tools/call",
                  "params": {
                    "name": "helloworld.greet",
                    "arguments": {
                      "name": "Meshingress"
                    }
                  }
                }""";
    }

    private static String toolsCallInstaFetchPayload() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": 30,
                  "method": "tools/call",
                  "params": {
                    "name": "instagram.fetch",
                    "arguments": {
                      "url": "https://www.instagram.com/reel/DWCd2FtkfTj/"
                    }
                  }
                }""";
    }

    private static String toolsCallArchitectPayload() {
        return """
                {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                  "name": "architect.entries.list",
                  "arguments": {
                    "status": "resolved"
                  }
                }
                }""";
    }

    private static String invalidEnvelopePayload() {
        return """
                {
                "jsonrpc": "1.0",
                "id": 4,
                "method": "ping"
                }""";
    }

    private static String adminListPayload() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": 5,
                  "method": "roles/tools/list",
                  "params": {
                    "includeDisabled": true,
                    "includePrivate": true
                  }
                }""";
    }

    private static String toolRequest(int id, String method) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "%s",
                  "params": {
                    "tool": {
                      "name": "architect.entries.copy",
                      "title": "List Architect Entries Alias",
                      "description": "List architect entries through a dynamic alias.",
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
                  }
                }""".formatted(id, method);
    }
}

