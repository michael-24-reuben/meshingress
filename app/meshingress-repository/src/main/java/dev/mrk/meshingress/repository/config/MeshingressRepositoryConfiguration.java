package dev.mrk.meshingress.repository.config;

import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.security.FakeScanner;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
