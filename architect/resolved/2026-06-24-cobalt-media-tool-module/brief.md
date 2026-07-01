# Cobalt Media Tool Module

Install `https://github.com/imputnet/cobalt` as a Meshingress tool module.

The upstream Cobalt repo must live inside the Meshingress checkout, specifically inside the tool module, and not as a sibling checkout or external repo. The module should expose a practical MCP surface for using a configured Cobalt API instance.

Scope boundaries:

- Keep the upstream source under `toolspace/cobalt/`.
- Do not place the cloned repo outside Meshingress.
- Do not conflate this with the existing `toolspace/whatsapp-cobalt` module, which wraps Auties00 Cobalt for WhatsApp.
- Do not default to the public hosted Cobalt API; upstream documents that hosted instances are not intended for third-party project use without permission.
