package dev.mrk.meshingress.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "meshingress")
public record MeshingressProperties(
        @Valid @NotNull Identity identity,
        @Valid @NotNull Mcp mcp,
        @Valid @NotNull Tools tools,
        @Valid @NotNull Dispatch dispatch,
        @Valid @NotNull Security security,
        @Valid @NotNull Scopes scopes,
        @Valid @NotNull Audit audit,
        @Valid @NotNull Secrets secrets
) {
    public record Identity(
            @NotBlank String name,
            @NotBlank String instanceId,
            @NotBlank String environment,
            @NotNull URI publicBaseUrl,
            @NotBlank String nodeRole
    ) {}

    public record Mcp(
            @Valid @NotNull WebSocket websocket
    ) {
        public record WebSocket(
                boolean enabled,
                @NotBlank String path,
                @NotNull List<String> allowedOrigins,
                @NotNull DataSize maxMessageSize,
                @NotNull Duration sendTimeout,
                @NotNull Duration idleTimeout,
                boolean requireAuth
        ) {}
    }

    public record Tools(
            @Valid @NotNull Registry registry,
            List<String> allowList,
            List<String> denyList,
            @NotNull Duration defaultTimeout,
            boolean defaultAudit,
            boolean defaultDebugTrace
    ) {
        public record Registry(
                boolean enabled,
                boolean failOnDuplicateToolId,
                boolean failOnInvalidToolId,
                boolean includeDisabled,
                boolean scanOnStartup,
                boolean exposePrivateTools
        ) {}
    }

    public record Dispatch(
            @NotNull Duration defaultTimeout,
            @Min(1) int maxConcurrentCalls,
            @Min(0) int queueCapacity,
            boolean rejectWhenSaturated,
            boolean includeStacktrace,
            boolean includeGeneratedAt,
            boolean redactErrors
    ) {}

    public record Security(
            boolean enabled,
            @NotBlank String mode,
            boolean requireAuthentication,
            boolean requireToolApproval,
            boolean requireApprovalForPrivileged,
            boolean requireApprovalForCritical,
            boolean denyUnknownScopes,
            boolean defaultDeny
    ) {}

    public record Scopes(
            boolean auditRequiredForHighRisk,
            boolean explicitApprovalForCritical,
            boolean allowShellExecute,
            boolean allowFilesDelete,
            boolean allowNetworkInbound
    ) {}

    public record Audit(
            boolean enabled,
            boolean logToolCalls,
            boolean logToolResults,
            boolean logArguments,
            boolean redactSecrets,
            @NotBlank String storage,
            @Min(0) int maxArgumentLength
    ) {}

    public record Secrets(
            boolean enabled,
            boolean allowEnv,
            boolean allowFile,
            boolean allowInline,
            @NotBlank String redactionPlaceholder,
            boolean failOnMissing
    ) {}
}