package dev.mrk.toolspace.whatsapp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WhatsappSendTextArgs(
        @McpInputField(value = "recipient", description = "WhatsApp recipient as a phone number with country code or full JID.")
        String recipient,

        @McpInputField(value = "text", description = "Text message to send.")
        String text,

        @McpInputField(value = "timeoutMs", description = "Milliseconds to wait for Cobalt to send the message.", required = false)
        Integer timeoutMs
) {
}
