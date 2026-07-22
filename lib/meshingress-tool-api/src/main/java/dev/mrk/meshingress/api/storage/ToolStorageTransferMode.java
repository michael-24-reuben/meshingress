package dev.mrk.meshingress.api.storage;

/** The byte-transfer responsibility exposed to tool implementations. */
public enum ToolStorageTransferMode {
    /** Tools upload every file through {@link ToolStorageService#writeFile}. */
    LOCAL_BYTES,
    /** Tools upload native files but register HTTPS media for the provider to fetch. */
    DELEGATED_SOURCE_URLS
}
