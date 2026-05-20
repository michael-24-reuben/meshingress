# MCP WebSocket Demo

This sample shows the current `/mcp/ws` authentication flow end to end from Java.

The current implementation uses a configurable stub validator. That means token creation, deletion, and rotation are config operations for the MVP. There is no runtime token CRUD API yet. After changing or deleting credentials, restart the server so the new configuration is loaded.

## Java Sample

The Java sample is:

```txt
samples/mcp-websocket/java/McpWebSocketUseCase.java
```

It exposes methods for:

- `createTokens()`
- `modifyTokens()`
- `deleteTokens()`
- `ping(...)`
- `initialize(...)`
- `listTools(...)`
- `callTool(...)`
- `adminListTools()`

Compile it with the JDK:

```powershell
javac -d temp\mcp-ws-demo\classes samples\mcp-websocket\java\McpWebSocketUseCase.java
```

## Flow

```txt
1. Generate demo credentials into temp/mcp-ws-demo/application-mcp-ws-demo.properties.
2. Start the server with that file as an additional Spring config location.
3. Connect to ws://localhost:8080/mcp/ws with:
   Authorization: Bearer <access-token>
   X-Secret-Key: <secret-key>
   X-Auth-Token: <auth-token>
4. Send MCP JSON-RPC text frames.
5. Rotate credentials by generating a new file and restarting the server.
6. Delete credentials by removing the file and restarting the server without it.
```

## Create Demo Tokens

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase create
```

The sample prints a matching server command. It will look like:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am spring-boot:run "-Dspring-boot.run.arguments=--spring.config.additional-location=file:J:/.../temp/mcp-ws-demo/application-mcp-ws-demo.properties"
```

## Send Requests

In another terminal, after the server starts:

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase ping
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase initialize
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase tools-list
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase hello
```

## Send An Admin Request

Admin role behavior still belongs to the existing MCP dispatcher and role services. The WebSocket sample only passes the same role context that HTTP `/mcp` already uses.

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase admin-tools-list
```

The Java sample sends `X-Mcp-Role: admin` during the WebSocket handshake.

## Rotate Tokens

Generate a new credential file, restart the server with the same additional config location, then send again:

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase modify
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase ping
```

Old credentials should fail once the server is restarted with the rotated values.

## Delete Tokens

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase delete
```

Then restart the server without the deleted demo config. Because the MVP validator is config-backed, deletion is not visible to an already running server until restart.

## Failure Check

Use intentionally invalid credentials to confirm the handshake is rejected:

```powershell
java -cp temp\mcp-ws-demo\classes McpWebSocketUseCase invalid
```

## PowerShell Variant

The older PowerShell helper is still available at:

```txt
samples/mcp-websocket/mcp-ws-usecase.ps1
```
