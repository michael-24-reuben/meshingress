package dev.mrk.meshingress.api.storage;

/**
 * Meshingress-owned publication execution for {@link ToolStorageTransferMode#LOCAL_BYTES}.
 * This is intentionally absent from delegated-source workspaces.
 */
public enum ToolStorageLocalPublicationMode {
    /** Publish to the configured destination before the tool call returns. */
    INLINE,
    /** Persist a Meshingress-owned external handoff job and return after it is queued. */
    QUEUED
}
