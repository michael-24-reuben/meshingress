package dev.mrk.meshingress.api.storage;

import java.time.Duration;

public record ToolStorageWorkspaceRequest(Duration ttl, Integer maxRequests) { }
