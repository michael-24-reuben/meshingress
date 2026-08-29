# Brief: Tool Distribution Modules

Create a Maven module structure under `distribution/` that makes tool dependencies easy to track without creating a circular dependency.

The required direction is:

```text
tool -> tool BOM / tool starter
tool distribution -> tool
server -> tool distribution
```
