# Meshingress

Spring Boot MCP HTTP server prototype with attachable MCP tool modules.

## Module Layout

```txt
lib/meshingress-tool-api/     # shared SPI for tool authors
toolspace/helloworld/         # sample attachable tool module
app/meshingress-server/       # Spring Boot MCP server
```

Tool modules should depend on `meshingress-tool-api`, not the Spring Boot server. The server then attaches a tool module by adding it as a dependency.

## Creating a Tool Module

Add the API dependency:

```xml
<dependency>
    <groupId>dev.mrk.meshingress</groupId>
    <artifactId>meshingress-tool-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

Implement:

```java
import dev.mrk.meshingress.api.tools.McpToolHandler;

McpToolHandler
```

The handler returns a `McpToolDescriptor` from `descriptor()` and executes in `call(ObjectNode arguments, McpCallContext context)`.

See:

```txt
toolspace/helloworld/src/main/java/dev/mrk/toolspace/helloworld/HelloWorldTool.java
```

Expose the handler as a Spring bean from the tool module. The sample uses Boot auto-configuration:

```txt
toolspace/helloworld/src/main/java/dev/mrk/toolspace/helloworld/HelloWorldToolAutoConfiguration.java
toolspace/helloworld/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Attach a module to the server by adding it to:

```txt
app/meshingress-server/pom.xml
```

## MCP Transport

The canonical MCP surface is the single JSON-RPC endpoint:

```http
POST /mcp
```

The server also exposes MCP over authenticated WebSocket for server-to-server clients:

```txt
ws://localhost:8080/mcp/ws
```

See the runnable token/use-case walkthrough:

```txt
samples/mcp-websocket/README.md
samples/mcp-websocket/mcp-ws-usecase.ps1
```

Reserved MVP paths:

```http
GET    /mcp    # 405 Method Not Allowed until SSE is implemented
DELETE /mcp    # 202 Accepted session-termination placeholder
```

Implemented JSON-RPC methods:

```txt
initialize
notifications/initialized
ping
tools/list
tools/call
admin/tools/check
admin/tools/register
admin/tools/update
admin/tools/delete
admin/tools/list
admin/tools/reload
```

Admin methods require either:

```http
Authorization: Bearer dev-admin
```

or:

```http
X-Mcp-Admin: true
```

Current built-in/attached tools:

```txt
architect.entries.list
helloworld.greet
```

## Development

```powershell
.\mvnw.cmd test
```

Run only the server and required upstream modules:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test
```
