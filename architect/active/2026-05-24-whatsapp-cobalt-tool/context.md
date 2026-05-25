# Context

- Cobalt is an unofficial WhatsApp client library with Web and Mobile modes.
- The stable Maven Central artifact inspected for implementation is `com.github.auties00:cobalt:0.0.10`.
- Cobalt can sync/store contacts, chats, and messages after a session is connected. This implementation should avoid exposing that capability through public Meshingress functions.
- Web pairing can be started with either a QR payload or a phone-number pairing code.
- The module should use `WebHistorySetting.discard(false)` by default to avoid pulling historical message data into the session store.
- The implementation is for personal/local notification use, not bulk or business messaging.
