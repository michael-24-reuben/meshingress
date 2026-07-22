package dev.mrk.meshingress.storage.config;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.storage.workspace.ExternalHandoffPublisher;
import dev.mrk.meshingress.storage.workspace.WebDavHandoffPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Validates the lifecycle before accepting writes and selects only supported handoff implementations.
 */
final class StorageLifecyclePolicy {
    private static final Logger logger = LoggerFactory.getLogger(StorageLifecyclePolicy.class);
    private final ExternalHandoffPublisher publisher;

    StorageLifecyclePolicy(MeshingressProperties.Storage storage) {
        if (storage.lifecycle() == MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL) {
            publisher = null;
            return;
        }
        logger.info("Initializing external storage for lifecycle: {}", storage.lifecycle());

        if (storage.external().defaultTarget().isBlank()) throw new IllegalStateException("meshingress.storage.external.default-target is required for an external lifecycle.");
        MeshingressProperties.Storage.Target target = storage.external().targets().get(storage.external().defaultTarget());
        if (target == null || !target.enabled()) throw new IllegalStateException("The selected external storage target must exist and be enabled.");
        if (storage.lifecycle() == MeshingressProperties.Storage.Lifecycle.EXTERNAL_EXTERNAL) {
            if (target.stagingMode() != MeshingressProperties.Storage.StagingMode.PROVIDER_SESSION)
                throw new IllegalStateException("external-external requires a target with provider-session staging.");
            throw new IllegalStateException("No provider-session storage adapter is installed for external-external.");
        }
        if (storage.lifecycle() == MeshingressProperties.Storage.Lifecycle.DELEGATED_EXTERNAL) {
            if (target.provider() != MeshingressProperties.Storage.Provider.NEXTCLOUD)
                throw new IllegalStateException("delegated-external requires an enabled Nextcloud target.");
            if (target.endpoint().isBlank())
                throw new IllegalStateException("delegated-external requires a Nextcloud target endpoint.");
            publisher = null;
            return;
        }
        if (target.provider() != MeshingressProperties.Storage.Provider.WEBDAV || target.stagingMode() != MeshingressProperties.Storage.StagingMode.DIRECT_FINAL_ONLY) {
            throw new IllegalStateException("local-external and local-async-external currently require an enabled WebDAV direct-final-only target.");
        }
        publisher = new WebDavHandoffPublisher(storage.external().defaultTarget(), target, storage.external().uploadTimeout(), authorization(target.credentialRef()));
    }

    Optional<ExternalHandoffPublisher> publisher() {
        return Optional.ofNullable(publisher);
    }

    /**
     * Resolves an external authorization credential.
     *
     * @param credentialRef An environment reference such as env:NEXTCLOUD_AUTH,
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
