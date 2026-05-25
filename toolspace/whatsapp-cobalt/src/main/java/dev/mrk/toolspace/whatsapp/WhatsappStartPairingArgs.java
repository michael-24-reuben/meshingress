package dev.mrk.toolspace.whatsapp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WhatsappStartPairingArgs(
        @McpInputField(value = "mode", description = "Pairing mode: pairing_code or qr.", required = false)
        String mode,

        @McpInputField(value = "phoneNumber", description = "Phone number with country code and digits only. Required for pairing_code.", required = false)
        String phoneNumber,

        @McpInputField(value = "alias", description = "Local Cobalt session alias.", required = false)
        String alias,

        @McpInputField(value = "deviceName", description = "Device name shown in WhatsApp linked devices.", required = false)
        String deviceName,

        @McpInputField(value = "timeoutMs", description = "Milliseconds to wait for a QR payload or pairing code.", required = false)
        Integer timeoutMs
) {
}
