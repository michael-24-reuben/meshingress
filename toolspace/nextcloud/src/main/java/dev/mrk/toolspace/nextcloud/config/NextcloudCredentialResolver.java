package dev.mrk.toolspace.nextcloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves a provider-owned authorization reference without exposing its value. */
final class NextcloudCredentialResolver {
    private static final Logger logger = LoggerFactory.getLogger(NextcloudCredentialResolver.class);

    private NextcloudCredentialResolver() { }

    static String resolveAuthorization(String credentialRef) {
        if (credentialRef == null || credentialRef.isBlank()) return "";
        if (!credentialRef.startsWith("env:")) {
            logger.warn("Inline Nextcloud authorization is configured; use env:<VARIABLE_NAME> instead.");
            return credentialRef;
        }
        String variableName = credentialRef.substring("env:".length());
        if (variableName.isBlank()) throw new IllegalStateException("Nextcloud credential reference must name an environment variable.");
        String authorization = System.getenv(variableName);
        if (authorization == null || authorization.isBlank()) {
            throw new IllegalStateException("Nextcloud credential environment variable is missing or blank: " + variableName);
        }
        return authorization;
    }
}
