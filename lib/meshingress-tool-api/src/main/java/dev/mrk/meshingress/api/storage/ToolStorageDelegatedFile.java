package dev.mrk.meshingress.api.storage;

import java.net.URI;

/** A provider-owned source reference; byte and checksum metadata is unavailable until it completes. */
public record ToolStorageDelegatedFile(String relativePath, URI sourceUrl) { }
