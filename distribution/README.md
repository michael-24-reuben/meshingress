# Meshingress Tool Distribution

This directory keeps three one-way Maven roles separate:

```text
tool -> tool BOM / tool starter
tool distribution -> tool
server -> tool distribution
```

- `meshingress-tool-bom` manages shared tool dependency versions.
- `meshingress-tool-starter` provides the minimum common authoring classpath.
- `meshingress-tool-distribution` is the authoritative list of tools bundled with the server. It is an otherwise-empty JAR so Spring Boot packages its transitive tool dependencies.

Individual tools must never depend on `meshingress-tool-distribution`.
