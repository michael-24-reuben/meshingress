package dev.mrk.meshingress.storage.workspace;

import java.io.InputStream;

/** Opens a file from the sole Nextcloud workspace after consuming one viewer capability use. */
public final class DelegatedViewerService {
    public record OpenFile(InputStream input, String mimeType, long byteSize, String filename) { }

    private final DelegatedViewerCapabilityStore capabilities;
    private final NextcloudDelegatedWorkspaceClient nextcloud;

    public DelegatedViewerService(DelegatedViewerCapabilityStore capabilities, NextcloudDelegatedWorkspaceClient nextcloud) {
        this.capabilities = capabilities;
        this.nextcloud = nextcloud;
    }

    public OpenFile open(String token, String relativePath) {
        DelegatedViewerCapabilityStore.Capability capability = capabilities.claim(token);
        NextcloudDelegatedWorkspaceClient.RemoteFile file = nextcloud.download(capability.requestId(), capability.toolId(), relativePath);
        return new OpenFile(file.input(), mimeType(relativePath, file.mimeType()), file.byteSize(), filename(relativePath));
    }

    private static String mimeType(String path, String upstream) {
        return path != null && path.toLowerCase(java.util.Locale.ROOT).endsWith(".json") ? "application/json" : upstream;
    }
    private static String filename(String path) {
        int slash = path == null ? -1 : path.lastIndexOf('/');
        return path == null ? "download" : path.substring(slash + 1).replace("\"", "_");
    }
}
