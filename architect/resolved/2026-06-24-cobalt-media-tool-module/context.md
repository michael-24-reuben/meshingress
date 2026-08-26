# Context

Upstream repo: `https://github.com/imputnet/cobalt`

Vendored commit checked before integration:

```txt
a636575b09de1fc55d9b8cd98cac88f5f2f16b42
```

Upstream shape:

- Cobalt is a Node/Express API service and web app, not a Java library.
- The API package is `@imput/cobalt-api`.
- Local development requirements are Node.js, Git, and pnpm.
- Upstream docs state that hosted API instances such as `api.cobalt.tools` use bot protection and are not intended for use in other projects without explicit permission.

Existing Meshingress context:

- `toolspace/whatsapp-cobalt` already exists but is unrelated to `imputnet/cobalt`; it wraps the Auties00 Java WhatsApp library named Cobalt.
- The new module should be a separate `toolspace/cobalt` module with public tool id `cobalt`.
