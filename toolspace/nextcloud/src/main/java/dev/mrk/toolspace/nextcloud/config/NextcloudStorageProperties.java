package dev.mrk.toolspace.nextcloud.config;

import java.time.Duration;

/** Provider-owned configuration for delegated Nextcloud workspaces. */
public class NextcloudStorageProperties {
    private boolean enabled = true;
    private String endpoint = "";
    private String basePath = "";
    private String credentialRef = "";
    private String viewerBaseUri = "";
    private Duration requestTimeout = Duration.ofMinutes(30);
    private Duration defaultTtl = Duration.ofMinutes(30);
    private Duration maxTtl = Duration.ofHours(24);
    private int defaultMaxRequests = 1;
    private int maxRequests = 50;
    private String sqlSchema = "meshingress";
    private String sqlTable = "storage_entries";
    private boolean initializeSchema = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = normalized(endpoint); }
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = normalized(basePath); }
    public String getCredentialRef() { return credentialRef; }
    public void setCredentialRef(String credentialRef) { this.credentialRef = normalized(credentialRef); }
    public String getViewerBaseUri() { return viewerBaseUri; }
    public void setViewerBaseUri(String viewerBaseUri) { this.viewerBaseUri = normalized(viewerBaseUri); }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout == null ? Duration.ofMinutes(30) : requestTimeout; }
    public Duration getDefaultTtl() { return defaultTtl; }
    public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl == null ? Duration.ofMinutes(30) : defaultTtl; }
    public Duration getMaxTtl() { return maxTtl; }
    public void setMaxTtl(Duration maxTtl) { this.maxTtl = maxTtl == null ? Duration.ofHours(24) : maxTtl; }
    public int getDefaultMaxRequests() { return defaultMaxRequests; }
    public void setDefaultMaxRequests(int defaultMaxRequests) { this.defaultMaxRequests = defaultMaxRequests <= 0 ? 1 : defaultMaxRequests; }
    public int getMaxRequests() { return maxRequests; }
    public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests <= 0 ? 50 : maxRequests; }
    public String getSqlSchema() { return sqlSchema; }
    public void setSqlSchema(String sqlSchema) { this.sqlSchema = normalized(sqlSchema); }
    public String getSqlTable() { return sqlTable; }
    public void setSqlTable(String sqlTable) { this.sqlTable = normalized(sqlTable); }
    public boolean isInitializeSchema() { return initializeSchema; }
    public void setInitializeSchema(boolean initializeSchema) { this.initializeSchema = initializeSchema; }

    public void validate() {
        if (endpoint.isBlank()) throw new IllegalStateException("meshingress.toolspace.nextcloud.storage.endpoint is required.");
        if (basePath.isBlank()) throw new IllegalStateException("meshingress.toolspace.nextcloud.storage.base-path is required.");
        if (viewerBaseUri.isBlank()) throw new IllegalStateException("meshingress.toolspace.nextcloud.storage.viewer-base-uri is required.");
        if (requestTimeout.isNegative() || requestTimeout.isZero()) throw new IllegalStateException("Nextcloud request timeout must be positive.");
        if (defaultTtl.isNegative() || defaultTtl.isZero() || maxTtl.isNegative() || maxTtl.isZero()) throw new IllegalStateException("Nextcloud viewer TTL values must be positive.");
        if (defaultTtl.compareTo(maxTtl) > 0) throw new IllegalStateException("Nextcloud default viewer TTL cannot exceed its maximum.");
        if (defaultMaxRequests > maxRequests) throw new IllegalStateException("Nextcloud default viewer max requests cannot exceed its maximum.");
    }

    private static String normalized(String value) { return value == null ? "" : value.trim(); }
}
