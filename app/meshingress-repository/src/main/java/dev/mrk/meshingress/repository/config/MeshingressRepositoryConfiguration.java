package dev.mrk.meshingress.repository.config;

import dev.mrk.meshingress.artifact.publication.HmacPublicationRecordSigner;
import dev.mrk.meshingress.artifact.publication.PublicationRecordSigner;
import dev.mrk.meshingress.artifact.scope.BytecodeScopeScanner;
import dev.mrk.meshingress.artifact.scope.CycloneDxSbomGenerator;
import dev.mrk.meshingress.artifact.scope.EmbeddedCycloneDxJarSbomGenerator;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalog;
import dev.mrk.meshingress.artifact.scope.ScopeInferenceCatalogLoader;
import dev.mrk.meshingress.artifact.security.GrypeScannerAdapter;
import dev.mrk.meshingress.artifact.security.GrypeScannerOptions;
import dev.mrk.meshingress.artifact.security.GrypeSeverity;
import dev.mrk.meshingress.artifact.security.ScannerAdapter;
import dev.mrk.meshingress.artifact.security.ScannerFailurePolicy;
import dev.mrk.meshingress.artifact.security.ScannerPipelinePlan;
import dev.mrk.meshingress.artifact.security.ScannerPipelineStage;
import dev.mrk.meshingress.artifact.security.ScannerProcessRunner;
import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.repository.artifact.store.ArtifactMetadataStore;
import dev.mrk.meshingress.repository.artifact.store.SqlArtifactMetadataStore;
import dev.mrk.meshingress.toolmetadata.McpToolMetadata;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadataExporter;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadataExtractor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MeshingressRepositoryConfiguration {

    @Bean
    FileSystemArtifactStorage artifactStorage(MeshingressRepositoryProperties properties) {
        return new FileSystemArtifactStorage(properties.root());
    }

    @Bean
    PublicationRecordSigner publicationRecordSigner(MeshingressRepositoryProperties properties) {
        return new HmacPublicationRecordSigner(properties.signingKeyId(), properties.signingSecret());
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
    McpToolNativeMetadataExtractor mcpToolNativeMetadataExtractor() {
        return new McpToolNativeMetadataExtractor();
    }

    @Bean
    McpToolNativeMetadataExporter mcpToolNativeMetadataExporter() {
        return new McpToolNativeMetadataExporter();
    }

    @Bean
    McpToolMetadata mcpToolMetadata() {
        return new McpToolMetadata();
    }

    @Bean
    ScannerProcessRunner scannerProcessRunner() {
        return new ScannerProcessRunner();
    }

    @Bean
    ScannerPipelinePlan scannerPipelinePlan(MeshingressRepositoryProperties properties) {
        MeshingressRepositoryProperties.ScannerPipeline pipeline = properties.scannerPipeline();
        Map<String, ScannerPipelineStage> stages = new LinkedHashMap<>();
        addStages(stages, pipeline.requiredScanners(), true, pipeline);
        addStages(stages, pipeline.optionalScanners(), false, pipeline);
        return new ScannerPipelinePlan(new ArrayList<>(stages.values()));
    }

    @Bean
    ScannerAdapter grypeScannerAdapter(
            ScannerProcessRunner scannerProcessRunner,
            ScannerPipelinePlan scannerPipelinePlan,
            MeshingressRepositoryProperties properties,
            ObjectMapper objectMapper
    ) {
        MeshingressRepositoryProperties.Grype grype = properties.grype();
        ScannerPipelineStage stage = scannerPipelinePlan.stageOrDefault(GrypeScannerAdapter.SCANNER_NAME);
        return new GrypeScannerAdapter(
                scannerProcessRunner,
                objectMapper,
                new GrypeScannerOptions(
                        grype.executable(),
                        stage.expectedVersion(),
                        stage.timeout(),
                        stage.failurePolicy(),
                        GrypeSeverity.fromWire(grype.blockSeverityThreshold(), GrypeSeverity.CRITICAL),
                        GrypeSeverity.fromWire(grype.reviewSeverityThreshold(), GrypeSeverity.LOW),
                        grype.maxOutputChars()
                )
        );
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

    private void addStages(
            Map<String, ScannerPipelineStage> stages,
            List<String> scanners,
            boolean required,
            MeshingressRepositoryProperties.ScannerPipeline pipeline
    ) {
        for (String scanner : scanners) {
            if (scanner == null || scanner.isBlank()) {
                continue;
            }
            String name = scanner.trim();
            Duration timeout = pipeline.scannerTimeouts().getOrDefault(name, pipeline.defaultTimeout());
            String policy = pipeline.scannerFailurePolicies().getOrDefault(name, pipeline.failurePolicy());
            stages.put(name, new ScannerPipelineStage(
                    name,
                    pipeline.scannerVersions().getOrDefault(name, ""),
                    required,
                    timeout,
                    ScannerFailurePolicy.fromWire(policy, ScannerFailurePolicy.BLOCK)
            ));
        }
    }
}
