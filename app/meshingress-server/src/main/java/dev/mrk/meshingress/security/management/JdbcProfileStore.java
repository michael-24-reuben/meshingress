package dev.mrk.meshingress.security.management;

import dev.mrk.aegis.AuthProfile;
import dev.mrk.aegis.ProfileNotFoundException;
import dev.mrk.aegis.ProfileQuery;
import dev.mrk.aegis.ProfileRevisionConflictException;
import dev.mrk.aegis.ProfileSnapshot;
import dev.mrk.aegis.ProfileStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable JDBC implementation of Aegis's atomic profile-store SPI. */
@Component
public final class JdbcProfileStore implements ProfileStore {
    private static final String TABLE = "meshingress.security_profiles";
    private static final String IDENTITY_TABLE = "meshingress.security_profile_identities";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcProfileStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        jdbc.execute("create schema if not exists meshingress");
        jdbc.execute("create table if not exists " + TABLE + " (profile_id varchar(36) primary key, tenant_id varchar(255) not null, status varchar(32) not null, revision bigint not null, payload clob not null)");
        jdbc.execute("create index if not exists security_profiles_tenant_status_idx on " + TABLE + " (tenant_id, status)");
        jdbc.execute("create table if not exists " + IDENTITY_TABLE + " (issuer varchar(1024) not null, subject varchar(1024) not null, profile_id varchar(36) not null, primary key (issuer, subject), unique (profile_id, issuer, subject))");
        backfillIdentityIndex();
    }

    @Override
    public synchronized ProfileSnapshot create(AuthProfile profile) {
        Objects.requireNonNull(profile, "profile");
        try {
            jdbc.update("insert into " + TABLE + " (profile_id, tenant_id, status, revision, payload) values (?, ?, ?, ?, ?)",
                    profile.profileId().toString(), profile.tenantId(), profile.status().name(), 0L,
                    objectMapper.writeValueAsString(profile));
            try {
                syncIdentityIndex(profile);
            } catch (RuntimeException exception) {
                jdbc.update("delete from " + TABLE + " where profile_id = ?", profile.profileId().toString());
                throw exception;
            }
            return new ProfileSnapshot(profile, 0L);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist profile", exception);
        }
    }

    @Override
    public Optional<ProfileSnapshot> find(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId");
        return jdbc.query("select revision, payload from " + TABLE + " where profile_id = ?", (row, number) -> snapshot(row.getLong(1), row.getString(2)), profileId.toString())
                .stream().findFirst();
    }

    @Override
    public List<ProfileSnapshot> list(ProfileQuery query) {
        Objects.requireNonNull(query, "query");
        String sql = "select revision, payload from " + TABLE;
        Object[] args = new Object[0];
        if (!query.statuses().isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(query.statuses().size(), "?"));
            sql += " where status in (" + placeholders + ")";
            args = query.statuses().stream().map(Enum::name).toArray();
        }
        sql += " order by profile_id limit " + query.limit();
        return List.copyOf(jdbc.query(sql, (row, number) -> snapshot(row.getLong(1), row.getString(2)), args));
    }

    /** Finds the one profile bound to a verified external issuer and subject. */
    public Optional<ProfileSnapshot> findByIdentity(String issuer, String subject) {
        if (issuer == null || issuer.isBlank() || subject == null || subject.isBlank()) return Optional.empty();
        return jdbc.query("select p.revision, p.payload from " + TABLE + " p join " + IDENTITY_TABLE
                        + " i on p.profile_id = i.profile_id where i.issuer = ? and i.subject = ?",
                (row, number) -> snapshot(row.getLong(1), row.getString(2)), issuer, subject).stream().findFirst();
    }

    @Override
    public synchronized ProfileSnapshot replace(UUID profileId, long expectedRevision, AuthProfile replacement) {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(replacement, "replacement");
        if (!profileId.equals(replacement.profileId())) throw new IllegalArgumentException("replacement profile id must match profileId");
        try {
            ensureIdentityClaimsAvailable(replacement);
            int changed = jdbc.update("update " + TABLE + " set tenant_id = ?, status = ?, revision = revision + 1, payload = ? where profile_id = ? and revision = ?",
                    replacement.tenantId(), replacement.status().name(), objectMapper.writeValueAsString(replacement), profileId.toString(), expectedRevision);
            if (changed == 1) {
                try {
                    syncIdentityIndex(replacement);
                } catch (RuntimeException exception) {
                    // Identity changes are normally made by privileged routes. A failed unique
                    // binding is rejected instead of leaving a second identity owner.
                    throw exception;
                }
                return new ProfileSnapshot(replacement, Math.addExact(expectedRevision, 1));
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to replace profile", exception);
        }
        ProfileSnapshot actual = find(profileId).orElseThrow(() -> new ProfileNotFoundException(profileId));
        throw new ProfileRevisionConflictException(profileId, expectedRevision, actual.revision());
    }

    private ProfileSnapshot snapshot(long revision, String payload) {
        try {
            return new ProfileSnapshot(objectMapper.readValue(payload, AuthProfile.class), revision);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored profile payload is invalid", exception);
        }
    }

    private void backfillIdentityIndex() {
        for (ProfileSnapshot snapshot : jdbc.query("select revision, payload from " + TABLE, (row, number) -> snapshot(row.getLong(1), row.getString(2)))) {
            syncIdentityIndex(snapshot.profile());
        }
    }

    private void syncIdentityIndex(AuthProfile profile) {
        jdbc.update("delete from " + IDENTITY_TABLE + " where profile_id = ?", profile.profileId().toString());
        for (var identity : profile.identities()) {
            try {
                jdbc.update("insert into " + IDENTITY_TABLE + " (issuer, subject, profile_id) values (?, ?, ?)",
                        identity.issuer().toString(), identity.subject(), profile.profileId().toString());
            } catch (org.springframework.dao.DataIntegrityViolationException exception) {
                throw new IllegalArgumentException("external identity is already bound to another profile", exception);
            }
        }
    }

    private void ensureIdentityClaimsAvailable(AuthProfile profile) {
        for (var identity : profile.identities()) {
            List<String> owners = jdbc.query("select profile_id from " + IDENTITY_TABLE + " where issuer = ? and subject = ?",
                    (row, number) -> row.getString(1), identity.issuer().toString(), identity.subject());
            if (!owners.isEmpty() && !owners.getFirst().equals(profile.profileId().toString())) {
                throw new IllegalArgumentException("external identity is already bound to another profile");
            }
        }
    }
}
