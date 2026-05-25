package dev.mrk.toolspace.whatsapp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WhatsappDisconnectArgs(
        @McpInputField(value = "logout", description = "When true, log out and invalidate the saved WhatsApp session. Otherwise only disconnect.", required = false)
        Boolean logout,

        @McpInputField(value = "timeoutMs", description = "Milliseconds to wait for disconnect or logout.", required = false)
        Integer timeoutMs
) {
}
