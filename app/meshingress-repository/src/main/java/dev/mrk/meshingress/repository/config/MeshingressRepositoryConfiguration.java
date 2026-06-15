package dev.mrk.meshingress.repository.config;

import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.scope.BytecodeScopeScanner;
import dev.mrk.meshingress.artifact.scope.CycloneDxSbomGenerator;
import dev.mrk.meshingress.artifact.scope.EmbeddedCycloneDxJarSbomGenerator;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalog;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalogLoader;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataStore;
import dev.mrk.meshingress.repository.artifact.store.SqlArtifactMetadataStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class MeshingressRepositoryConfiguration {

    @Bean
    FileSystemArtifactStorage artifactStorage(MeshingressRepositoryProperties properties) {
        return new FileSystemArtifactStorage(properties.root());
    }

    @Bean
    PublicationRecordSigner publicationRecordSigner(MeshingressRepositoryProperties properties) {
        return new HmacPublicationRecordSigner(properties.signingSecret());
    }

    @Bean
    BytecodeScopeScanner bytecodeScopeScanner() {
        return new BytecodeScopeScanner();
    }

    @Bean
    CycloneDxSbomGenerator cycloneDxSbomGenerator() {
        return new EmbeddedCycloneDxJarSbomGenerator();
    }

    @Bean
    ScopeInferenceCatalog scopeInferenceCatalog(
            MeshingressRepositoryProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        Resource resource = resourceLoader.getResource(properties.scopeCatalogLocation());
        try (var inputStream = resource.getInputStream()) {
            return new ScopeInferenceCatalogLoader(objectMapper).load(inputStream);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load scope inference catalog from "
                    + properties.scopeCatalogLocation(), exception);
        }
    }

    @Bean
    ArtifactMetadataStore artifactMetadataStore(
            JdbcTemplate jdbcTemplate,
            MeshingressRepositoryProperties properties,
            ObjectMapper objectMapper
    ) {
        if (!"sql".equalsIgnoreCase(properties.metadataStore())) {
            throw new IllegalStateException("Unsupported repository metadata store: " + properties.metadataStore());
        }
        return new SqlArtifactMetadataStore(jdbcTemplate, properties.sql(), objectMapper);
    }
}
