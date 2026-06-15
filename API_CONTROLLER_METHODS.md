# API Controller Methods and Paths

Scanned modules:
- `app/meshingress-server`
- `app/meshingress-repository`

## `app/meshingress-server`

### Controller: `dev.mrk.meshingress.mcp.McpController`
- Source: `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`
- Class-level base path: `/mcp` (from `@RequestMapping("/mcp")`)

| Method   | Full Path | Java Method | Notes                                                                                                                                           |
|----------|-----------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST`   | `/mcp`    | `post(...)` | `consumes=application/json`, `produces=application/json`; also annotated with `@McpRoute(id="mcp.transport.post.v1", method=POST, path="/mcp")` |
| `GET`    | `/mcp`    | `get()`     | Returns `405 METHOD_NOT_ALLOWED`; also `@McpRoute(id="mcp.transport.get.v1", method=GET, path="/mcp")`                                          |
| `DELETE` | `/mcp`    | `delete()`  | Returns `202 ACCEPTED`; also `@McpRoute(id="mcp.transport.delete.v1", method=DELETE, path="/mcp")`                                              |

## `app/meshingress-repository`

### Controller: `dev.mrk.meshingress.repository.artifact.ArtifactController`
- Source: `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java`
- Class-level base path: _none_ (`@RestController` only; each method declares full path)

| Method | Full Path                                                | Java Method        | Notes                                      |
|--------|----------------------------------------------------------|--------------------|--------------------------------------------|
| `POST` | `/artifact/{groupId}/{artifactId}/{version}`             | `upload(...)`      | `consumes=multipart/form-data`             |
| `GET`  | `/artifact/{groupId}/{artifactId}/{version}/metadata`    | `metadata(...)`    |                                            |
| `POST` | `/artifact/{groupId}/{artifactId}/{version}/assess`      | `assess(...)`      |                                            |
| `GET`  | `/artifact/{groupId}/{artifactId}/{version}/assessment`  | `assessment(...)`  |                                            |
| `POST` | `/artifact/{groupId}/{artifactId}/{version}/approve`     | `approve(...)`     | Optional JSON body `ArtifactReviewRequest` |
| `POST` | `/artifact/{groupId}/{artifactId}/{version}/publish`     | `publish(...)`     |                                            |
| `GET`  | `/artifact/{groupId}/{artifactId}/{version}/publication` | `publication(...)` |                                            |

## Notes
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/web/RepositoryExceptionHandler.java` is `@RestControllerAdvice` with `@ExceptionHandler`, not a request-mapped API endpoint.
- No `server.servlet.context-path` or `spring.mvc.servlet.path` was found under either module's `src/main/resources`, so paths above are listed as declared in controller annotations.

