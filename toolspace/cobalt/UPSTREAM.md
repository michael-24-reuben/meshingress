# Vendored Cobalt Upstream

Source repository: `https://github.com/imputnet/cobalt`

Vendored commit:

```txt
a636575b09de1fc55d9b8cd98cac88f5f2f16b42
```

Vendored path:

```txt
toolspace/cobalt/upstream/cobalt
```

The nested `.git` directory was intentionally removed after cloning so this tool module can be tracked as ordinary Meshingress source rather than as a Git submodule.

The Java MCP wrapper in this module calls a configured Cobalt API instance. It does not call the public hosted Cobalt API by default; upstream documents that hosted instances are not intended for third-party project use without explicit permission.
