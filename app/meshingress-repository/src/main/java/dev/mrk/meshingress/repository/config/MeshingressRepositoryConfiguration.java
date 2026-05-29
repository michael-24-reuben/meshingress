package dev.mrk.meshingress.repository.config;

import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.scope.BytecodeScopeScanner;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalog;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalogLoader;
import dev.mrk.meshingress.artifact.security.FakeScanner;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class MeshingressRepositoryConfiguration {

    @Bean
    FileSystemArtifactStorage artifactStorage(MeshingressRepositoryProperties properties) {
        return new FileSystemArtifactStorage(properties.root());
    }

    @Bean
    List<ScannerAdapter> scannerAdapters(MeshingressRepositoryProperties properties) {
        if (!properties.fakeScannerEnabled()) {
            return List.of();
        }
        return List.of(new FakeScanner());
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
}
