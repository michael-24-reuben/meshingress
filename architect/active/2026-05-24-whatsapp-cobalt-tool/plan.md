# Plan

1. Add `toolspace/whatsapp-cobalt` as a Maven module.
2. Wrap Cobalt behind a Spring bean service that manages one active Web session.
3. Expose only status, pairing, registered reconnect, send text, and disconnect functions.
4. Attach the module to `app/meshingress-server`.
5. Add focused tests for the privacy boundary and argument validation.
6. Run focused Maven verification.
