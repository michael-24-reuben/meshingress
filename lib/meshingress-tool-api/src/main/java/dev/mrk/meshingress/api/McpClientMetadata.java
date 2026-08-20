package dev.mrk.meshingress.api;

/** Caller-declared client details. These values are intentionally not identity claims. */
public record McpClientMetadata(String name, String version, String protocolVersion) {
    public McpClientMetadata {
        name = text(name);
        version = text(version);
        protocolVersion = text(protocolVersion);
    }
    public static McpClientMetadata unknown() { return new McpClientMetadata("", "", ""); }
    private static String text(String value) { return value == null || value.isBlank() ? "" : value.trim(); }
}
