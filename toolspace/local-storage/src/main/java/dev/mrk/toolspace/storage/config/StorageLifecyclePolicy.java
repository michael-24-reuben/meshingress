package dev.mrk.toolspace.storage.config;

import dev.mrk.toolspace.localstorage.config.LocalStorageProperties;
import dev.mrk.toolspace.storage.workspace.ExternalHandoffPublisher;
import dev.mrk.toolspace.storage.workspace.WebDavHandoffPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Validates the lifecycle before accepting writes and selects only supported handoff implementations.
 */
final class StorageLifecyclePolicy {
    private static final Logger logger = LoggerFactory.getLogger(StorageLifecyclePolicy.class);
    private final ExternalHandoffPublisher publisher;

    StorageLifecyclePolicy(LocalStorageProperties storage) {
        if (storage.lifecycle() == LocalStorageProperties.Lifecycle.LOCAL_LOCAL) {
            publisher = null;
        } else {
            logger.info("Initializing external local-byte storage for lifecycle: {}", storage.lifecycle());
            publisher = localPublisher(storage);
        }
    }

    private static ExternalHandoffPublisher localPublisher(LocalStorageProperties storage) {
        if (storage.external().defaultTarget().isBlank())
            throw new IllegalStateException("meshingress.toolspace.local-storage.external.default-target is required for lifecycle=local-external.");
        LocalStorageProperties.Target target = storage.external().targets().get(storage.external().defaultTarget());
        if (target == null || !target.enabled()) throw new IllegalStateException("The selected local-byte storage target must exist and be enabled.");
        if (target.provider() != LocalStorageProperties.Provider.WEBDAV || target.stagingMode() != LocalStorageProperties.StagingMode.DIRECT_FINAL_ONLY) {
            throw new IllegalStateException("local-external currently requires an enabled WebDAV direct-final-only target.");
        }
        return new WebDavHandoffPublisher(storage.external().defaultTarget(), target, storage.external().uploadTimeout(), authorization(target.credentialRef()));
    }

    Optional<ExternalHandoffPublisher> publisher() {
        return Optional.ofNullable(publisher);
    }

    /**
     * Resolves an external authorization credential.
     *
     * @param credentialRef An environment reference such as env:STORAGE_AUTH,
     *                      or an inline credential.
     * @return The resolved authorization value.
     */
    static String authorization(String credentialRef) {
        if (credentialRef == null || credentialRef.isBlank()) return "";

        if (!credentialRef.startsWith("env:")) {
            logger.warn(
                    "Inline external credential detected. Credentials should be referenced using " +
                    "'env:<VARIABLE_NAME>' to prevent exposure through configuration files, source control, " +
                    "logs, diagnostics, or process metadata."
            );

            return credentialRef;
        }

        String variableName = credentialRef.substring("env:".length());

        if (variableName.isBlank())
            throw new IllegalStateException("External credential reference must specify an environment variable: env:<VARIABLE_NAME>.");

        String authorization = System.getenv(variableName);

        if (authorization == null || authorization.isBlank())
            throw new IllegalStateException("External credential environment variable is missing or blank: " + variableName);

        return authorization;
    }
}
