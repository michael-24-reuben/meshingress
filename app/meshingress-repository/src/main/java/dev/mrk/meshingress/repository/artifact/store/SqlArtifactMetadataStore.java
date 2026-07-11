package dev.mrk.meshingress.repository.artifact.store;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactFileEntry;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactRecord;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.security.ScannerResult;
import dev.mrk.meshingress.repository.artifact.ArtifactReviewQueueItem;
import dev.mrk.meshingress.repository.artifact.RepositoryException;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class SqlArtifactMetadataStore implements ArtifactMetadataStore {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String schema;
    private final String artifactsTable;
    private final String artifactFilesTable;
    private final String assessmentsTable;
    private final String publicationsTable;
    private final String lifecycleEventsTable;

    public SqlArtifactMetadataStore(
            JdbcTemplate jdbcTemplate,
            MeshingressRepositoryProperties.Sql properties,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.schema = validateIdentifier(properties.schema(), "schema");
        this.artifactsTable = qualified(properties.table().artifacts(), "artifacts table");
        this.artifactFilesTable = qualified(properties.table().artifactFiles(), "artifact files table");
        this.assessmentsTable = qualified(properties.table().assessments(), "assessments table");
        this.publicationsTable = qualified(properties.table().publications(), "publications table");
        this.lifecycleEventsTable = qualified(properties.table().lifecycleEvents(), "lifecycle events table");
        if (properties.initializeSchema()) {
            initializeSchema();
        }
    }

    @Override
    public void saveArtifact(ArtifactRecord record, Path artifactPath) {
        String coordinateKey = key(record.coordinate());
        String payload = toJson(record);
        int updated = jdbcTemplate.update("""
                        update %s
                        set type = ?,
                            trust_status = ?,
                            artifact_uri = ?,
                            artifact_path = ?,
                            checksum_algorithm = ?,
                            checksum_value = ?,
                            payload_json = ?,
                            created_at = ?,
                            updated_at = ?
                        where coordinate_key = ?
                        """.formatted(artifactsTable),
                record.type().name(),
                record.trustStatus().name(),
                record.artifactUri(),
                artifactPath.toAbsolutePath().normalize().toString(),
                record.artifactChecksum() == null ? null : record.artifactChecksum().algorithm(),
                record.artifactChecksum() == null ? null : record.artifactChecksum().value(),
                payload,
                record.createdAt().toString(),
                record.updatedAt().toString(),
                coordinateKey
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into %s (
                                coordinate_key,
                                group_id,
                                artifact_id,
                                version,
                                classifier,
                                packaging,
                                type,
                                trust_status,
                                artifact_uri,
                                artifact_path,
                                checksum_algorithm,
                                checksum_value,
                                payload_json,
                                created_at,
                                updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """.formatted(artifactsTable),
                    coordinateKey,
                    record.coordinate().groupId(),
                    record.coordinate().artifactId(),
                    record.coordinate().version(),
                    record.coordinate().classifier(),
                    record.coordinate().packaging(),
                    record.type().name(),
                    record.trustStatus().name(),
                    record.artifactUri(),
                    artifactPath.toAbsolutePath().normalize().toString(),
                    record.artifactChecksum() == null ? null : record.artifactChecksum().algorithm(),
                    record.artifactChecksum() == null ? null : record.artifactChecksum().value(),
                    payload,
                    record.createdAt().toString(),
                    record.updatedAt().toString()
            );
        }

        jdbcTemplate.update("delete from %s where coordinate_key = ?".formatted(artifactFilesTable), coordinateKey);
        for (ArtifactFileEntry file : record.files()) {
            jdbcTemplate.update("""
                            insert into %s (
                                coordinate_key,
                                path,
                                size_bytes,
                                checksum_algorithm,
                                checksum_value,
                                executable
                            )
                            values (?, ?, ?, ?, ?, ?)
                            """.formatted(artifactFilesTable),
                    coordinateKey,
                    file.path(),
                    file.size(),
                    file.checksum().algorithm(),
                    file.checksum().value(),
                    file.executable()
            );
        }
    }

    @Override
    public Optional<ArtifactMetadataEntry> findArtifact(ArtifactCoordinate coordinate) {
        List<ArtifactMetadataEntry> entries = jdbcTemplate.query(
                "select payload_json, artifact_path from %s where coordinate_key = ?".formatted(artifactsTable),
                artifactMapper(),
                key(coordinate)
        );
        return entries.stream().findFirst();
    }

    @Override
    public List<ArtifactMetadataEntry> findArtifactsByPackaging(String packaging) {
        return jdbcTemplate.query("""
                        select payload_json, artifact_path
                        from %s
                        where lower(packaging) = lower(?)
                        order by updated_at desc, group_id asc, artifact_id asc, version asc
                        """.formatted(artifactsTable),
                artifactMapper(),
                clean(packaging)
        );
    }

    @Override
    public void saveAssessment(ArtifactCoordinate coordinate, List<ScannerResult> results) {
        List<ScannerResult> safeResults = results == null ? List.of() : List.copyOf(results);
        String coordinateKey = key(coordinate);
        String payload = toJson(safeResults);
        int findingCount = safeResults.stream().mapToInt(result -> result.findings().size()).sum();
        String status = safeResults.stream()
                .map(result -> result.status().name())
                .distinct()
                .sorted()
                .toList()
                .toString();
        String updatedAt = OffsetDateTime.now().toString();
        int updated = jdbcTemplate.update("""
                        update %s
                        set status = ?,
                            scanner_count = ?,
                            finding_count = ?,
                            payload_json = ?,
                            updated_at = ?
                        where coordinate_key = ?
                        """.formatted(assessmentsTable),
                status,
                safeResults.size(),
                findingCount,
                payload,
                updatedAt,
                coordinateKey
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into %s (
                                coordinate_key,
                                status,
                                scanner_count,
                                finding_count,
                                payload_json,
                                updated_at
                            )
                            values (?, ?, ?, ?, ?, ?)
                            """.formatted(assessmentsTable),
                    coordinateKey,
                    status,
                    safeResults.size(),
                    findingCount,
                    payload,
                    updatedAt
            );
        }
    }

    @Override
    public List<ScannerResult> findAssessment(ArtifactCoordinate coordinate) {
        List<String> payloads = jdbcTemplate.query(
                "select payload_json from %s where coordinate_key = ?".formatted(assessmentsTable),
                (rs, rowNum) -> rs.getString("payload_json"),
                key(coordinate)
        );
        return payloads.stream()
                .findFirst()
                .map(payload -> List.of(fromJson(payload, ScannerResult[].class)))
                .orElseGet(List::of);
    }

    @Override
    public void savePublication(ArtifactPublicationRecord publication) {
        String coordinateKey = key(publication.coordinate());
        String payload = toJson(publication);
        String updatedAt = OffsetDateTime.now().toString();
        int updated = jdbcTemplate.update("""
                        update %s
                        set trust_status = ?,
                            artifact_uri = ?,
                            signature_key_id = ?,
                            signature_algorithm = ?,
                            signature = ?,
                            revoked = ?,
                            payload_json = ?,
                            published_at = ?,
                            updated_at = ?
                        where coordinate_key = ?
                        """.formatted(publicationsTable),
                publication.trustStatus().name(),
                publication.artifactUri(),
                publication.signatureKeyId(),
                publication.signatureAlgorithm(),
                publication.signature(),
                publication.revoked(),
                payload,
                publication.publishedAt().toString(),
                updatedAt,
                coordinateKey
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into %s (
                                coordinate_key,
                                trust_status,
                                artifact_uri,
                                signature_key_id,
                                signature_algorithm,
                                signature,
                                revoked,
                                payload_json,
                                published_at,
                                updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """.formatted(publicationsTable),
                    coordinateKey,
                    publication.trustStatus().name(),
                    publication.artifactUri(),
                    publication.signatureKeyId(),
                    publication.signatureAlgorithm(),
                    publication.signature(),
                    publication.revoked(),
                    payload,
                    publication.publishedAt().toString(),
                    updatedAt
            );
        }
    }

    @Override
    public Optional<ArtifactPublicationRecord> findPublication(ArtifactCoordinate coordinate) {
        List<ArtifactPublicationRecord> publications = jdbcTemplate.query(
                "select payload_json from %s where coordinate_key = ?".formatted(publicationsTable),
                (rs, rowNum) -> fromJson(rs.getString("payload_json"), ArtifactPublicationRecord.class),
                key(coordinate)
        );
        return publications.stream().findFirst();
    }

    @Override
    public List<ArtifactReviewQueueItem> findPendingReviewArtifacts() {
        return jdbcTemplate.query("""
                        select artifacts.payload_json as artifact_payload,
                               assessments.payload_json as assessment_payload
                        from %s artifacts
                        left join %s assessments on assessments.coordinate_key = artifacts.coordinate_key
                        where artifacts.trust_status = ?
                        order by artifacts.updated_at asc
                        """.formatted(artifactsTable, assessmentsTable),
                reviewQueueMapper(),
                ArtifactTrustStatus.REVIEW_PENDING.name()
        );
    }

    @Override
    public boolean hasLifecycleEvent(ArtifactCoordinate coordinate, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from %s where coordinate_key = ? and event_type = ?".formatted(lifecycleEventsTable),
                Integer.class,
                key(coordinate),
                clean(eventType)
        );
        return count != null && count > 0;
    }

    @Override
    public int countLifecycleEvents(ArtifactCoordinate coordinate, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from %s where coordinate_key = ? and event_type = ?".formatted(lifecycleEventsTable),
                Integer.class,
                key(coordinate),
                clean(eventType)
        );
        return count == null ? 0 : count;
    }

    @Override
    public Optional<ArtifactLifecycleEvent> findLatestLifecycleEvent(ArtifactCoordinate coordinate, String eventType) {
        List<ArtifactLifecycleEvent> events = jdbcTemplate.query("""
                        select event_type,
                               from_state,
                               to_state,
                               actor,
                               request_id,
                               reason
                        from %s
                        where coordinate_key = ? and event_type = ?
                        order by event_id desc
                        limit 1
                        """.formatted(lifecycleEventsTable),
                lifecycleEventMapper(coordinate),
                key(coordinate),
                clean(eventType)
        );
        return events.stream().findFirst();
    }

    @Override
    public void appendLifecycleEvent(ArtifactLifecycleEvent event) {
        jdbcTemplate.update("""
                        insert into %s (
                            coordinate_key,
                            event_type,
                            from_state,
                            to_state,
                            actor,
                            request_id,
                            reason,
                            created_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """.formatted(lifecycleEventsTable),
                key(event.coordinate()),
                clean(event.eventType()),
                clean(event.fromState()),
                clean(event.toState()),
                clean(event.actor()),
                clean(event.requestId()),
                clean(event.reason()),
                OffsetDateTime.now().toString()
        );
    }

    private void initializeSchema() {
        jdbcTemplate.execute("create schema if not exists " + schema);
        jdbcTemplate.execute("""
                create table if not exists %s (
                    coordinate_key varchar(512) primary key,
                    group_id varchar(255) not null,
                    artifact_id varchar(255) not null,
                    version varchar(128) not null,
                    classifier varchar(128),
                    packaging varchar(64) not null,
                    type varchar(128) not null,
                    trust_status varchar(128) not null,
                    artifact_uri varchar(2048) not null,
                    artifact_path varchar(2048) not null,
                    checksum_algorithm varchar(64),
                    checksum_value varchar(256),
                    payload_json clob not null,
                    created_at varchar(64) not null,
                    updated_at varchar(64) not null
                )
                """.formatted(artifactsTable));
        jdbcTemplate.execute("""
                create table if not exists %s (
                    coordinate_key varchar(512) not null,
                    path varchar(2048) not null,
                    size_bytes bigint not null,
                    checksum_algorithm varchar(64) not null,
                    checksum_value varchar(256) not null,
                    executable boolean not null,
                    primary key (coordinate_key, path)
                )
                """.formatted(artifactFilesTable));
        jdbcTemplate.execute("""
                create table if not exists %s (
                    coordinate_key varchar(512) primary key,
                    status varchar(256) not null,
                    scanner_count integer not null,
                    finding_count integer not null,
                    payload_json clob not null,
                    updated_at varchar(64) not null
                )
                """.formatted(assessmentsTable));
        jdbcTemplate.execute("""
                create table if not exists %s (
                    coordinate_key varchar(512) primary key,
                    trust_status varchar(128) not null,
                    artifact_uri varchar(2048) not null,
                    signature_key_id varchar(255) not null default '',
                    signature_algorithm varchar(128) not null,
                    signature varchar(1024) not null,
                    revoked boolean not null,
                    payload_json clob not null,
                    published_at varchar(64) not null,
                    updated_at varchar(64) not null
                )
                """.formatted(publicationsTable));
        jdbcTemplate.execute("alter table %s add column if not exists signature_key_id varchar(255) not null default ''".formatted(publicationsTable));
        jdbcTemplate.execute("""
                create table if not exists %s (
                    event_id bigint generated by default as identity primary key,
                    coordinate_key varchar(512) not null,
                    event_type varchar(128) not null,
                    from_state varchar(128),
                    to_state varchar(128),
                    actor varchar(255) not null,
                    request_id varchar(255),
                    reason varchar(2048),
                    created_at varchar(64) not null
                )
                """.formatted(lifecycleEventsTable));
        jdbcTemplate.execute("alter table %s add column if not exists actor varchar(255) not null default 'unknown'".formatted(lifecycleEventsTable));
        jdbcTemplate.execute("alter table %s add column if not exists request_id varchar(255)".formatted(lifecycleEventsTable));
    }

    private RowMapper<ArtifactMetadataEntry> artifactMapper() {
        return (rs, rowNum) -> new ArtifactMetadataEntry(
                fromJson(rs.getString("payload_json"), ArtifactRecord.class),
                Path.of(rs.getString("artifact_path"))
        );
    }

    private RowMapper<ArtifactReviewQueueItem> reviewQueueMapper() {
        return (rs, rowNum) -> {
            String assessmentPayload = rs.getString("assessment_payload");
            List<ScannerResult> assessment = assessmentPayload == null || assessmentPayload.isBlank()
                    ? List.of()
                    : List.of(fromJson(assessmentPayload, ScannerResult[].class));
            return new ArtifactReviewQueueItem(
                    fromJson(rs.getString("artifact_payload"), ArtifactRecord.class),
                    assessment
            );
        };
    }

    private RowMapper<ArtifactLifecycleEvent> lifecycleEventMapper(ArtifactCoordinate coordinate) {
        return (rs, rowNum) -> new ArtifactLifecycleEvent(
                coordinate,
                rs.getString("event_type"),
                rs.getString("from_state"),
                rs.getString("to_state"),
                rs.getString("actor"),
                rs.getString("request_id"),
                rs.getString("reason")
        );
    }

    private String qualified(String table, String name) {
        return schema + "." + validateIdentifier(table, name);
    }

    private String validateIdentifier(String value, String name) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL " + name + ": " + value);
        }
        return value;
    }

    private String key(ArtifactCoordinate coordinate) {
        return coordinate.display();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new RepositoryException("unable to serialize repository metadata: " + exception.getMessage(), exception);
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new RepositoryException("unable to read repository metadata: " + exception.getMessage(), exception);
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
