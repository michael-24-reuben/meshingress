# Fixes

## Files Changed

- `toolspace/whatsapp-cobalt/pom.xml`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappCobaltTool.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappCobaltSessionService.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappCobaltToolAutoConfiguration.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappStartPairingArgs.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappConnectArgs.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappSendTextArgs.java`
- `toolspace/whatsapp-cobalt/src/main/java/dev/mrk/toolspace/whatsapp/WhatsappDisconnectArgs.java`
- `toolspace/whatsapp-cobalt/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `toolspace/whatsapp-cobalt/src/test/java/dev/mrk/toolspace/whatsapp/WhatsappCobaltSessionServiceTests.java`
- `app/meshingress-server/pom.xml`

## Behavioral Changes

- Added a WhatsApp Cobalt toolspace module with a single-session service and auto-configuration.
- Exposed only the minimal MCP tools needed for status, pairing, registered reconnect, send text, and disconnect.
- Kept the privacy boundary by not exposing chats, contacts, or inbound message access through the MCP surface.

