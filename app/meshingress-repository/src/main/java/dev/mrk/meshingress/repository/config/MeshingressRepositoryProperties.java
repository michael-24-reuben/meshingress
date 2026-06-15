package dev.mrk.meshingress.repository.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "meshingress.repository")
public record MeshingressRepositoryProperties(
        @NotNull Path root,
        @NotBlank String signingSecret,
        boolean scopeScannerEnabled,
        String scopeCatalogLocation,
        String metadataStore,
        Sql sql
) {
    public MeshingressRepositoryProperties {
        root = root == null ? Path.of("repository") : root;
        signingSecret = signingSecret == null || signingSecret.isBlank() ? "dev-repository-signing-key" : signingSecret;
        scopeCatalogLocation = scopeCatalogLocation == null || scopeCatalogLocation.isBlank()
                ? "classpath:/scope-rules/default-bytecode-scope-catalog.json"
                : scopeCatalogLocation;
        metadataStore = metadataStore == null || metadataStore.isBlank() ? "sql" : metadataStore.trim();
        sql = sql == null ? Sql.defaults() : sql;
    }

    public record Sql(
            String schema,
            String tablePrefix,
            Tables table,
            boolean initializeSchema
    ) {
        public Sql {
            schema = schema == null || schema.isBlank() ? "meshingress" : schema.trim();
            tablePrefix = tablePrefix == null ? "repository_" : tablePrefix.trim();
            table = table == null ? Tables.defaults(tablePrefix) : table.withDefaults(tablePrefix);
        }

        static Sql defaults() {
            return new Sql("meshingress", "repository_", Tables.defaults("repository_"), true);
        }
    }

    public record Tables(
            String artifacts,
            String artifactFiles,
            String assessments,
            String publications,
            String lifecycleEvents
    ) {
        public Tables withDefaults(String prefix) {
            return new Tables(
                    valueOrDefault(artifacts, prefix + "artifacts"),
                    valueOrDefault(artifactFiles, prefix + "artifact_files"),
                    valueOrDefault(assessments, prefix + "assessments"),
                    valueOrDefault(publications, prefix + "publications"),
                    valueOrDefault(lifecycleEvents, prefix + "lifecycle_events")
            );
        }

        static Tables defaults(String prefix) {
            return new Tables(
                    prefix + "artifacts",
                    prefix + "artifact_files",
                    prefix + "assessments",
                    prefix + "publications",
                    prefix + "lifecycle_events"
            );
        }

        private static String valueOrDefault(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }
    }
}
