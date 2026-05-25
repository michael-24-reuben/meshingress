package dev.mrk.toolspace.whatsapp;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhatsappCobaltSessionServiceTests {

    private final WhatsappCobaltSessionService service = new WhatsappCobaltSessionService(new ObjectMapper());

    @Test
    void statusDoesNotExposeConversationReadSurface() {
        var status = service.status();

        assertFalse(status.get("connected").booleanValue());
        assertFalse(status.get("conversationReadSurfaceExposed").booleanValue());
        assertEquals("discard", status.get("historySetting").asText());
    }

    @Test
    void pairingCodeRequiresPhoneNumber() {
        var args = new WhatsappStartPairingArgs("pairing_code", null, null, null, 1000);

        assertThrows(IllegalArgumentException.class, () -> service.startPairing(args));
    }

    @Test
    void sendTextRequiresConnectedClient() {
        var args = new WhatsappSendTextArgs("15555550123", "hello", 1000);

        assertThrows(IllegalStateException.class, () -> service.sendText(args));
    }

    @Test
    void parsesPhoneRecipientAsUserJid() {
        var jid = WhatsappCobaltSessionService.parseRecipient("+1 (555) 555-0123");

        assertEquals("15555550123@s.whatsapp.net", jid.toString());
    }
}
