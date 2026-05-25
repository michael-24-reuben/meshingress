package dev.mrk.toolspace.whatsapp;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WhatsappConnectArgs(
        @McpInputField(value = "alias", description = "Local Cobalt session alias.", required = false)
        String alias,

        @McpInputField(value = "deviceName", description = "Device name shown in WhatsApp linked devices.", required = false)
        String deviceName,

        @McpInputField(value = "timeoutMs", description = "Milliseconds to wait for the registered session to connect.", required = false)
        Integer timeoutMs
) {
}
