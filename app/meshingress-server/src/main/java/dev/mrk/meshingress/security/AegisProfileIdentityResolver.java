package dev.mrk.meshingress.security;

import dev.mrk.aegis.AuthenticatedIdentity;
import dev.mrk.aegis.VerifiedIdentity;
import dev.mrk.aegis.ProfileQuery;
import dev.mrk.aegis.ProfileSnapshot;
import dev.mrk.aegis.ProfileStatus;
import dev.mrk.meshingress.security.management.JdbcProfileStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Looks up local authorization data only after an external issuer has verified a JWT. */
@Component
public final class AegisProfileIdentityResolver {
    private final JdbcProfileStore profiles;

    public AegisProfileIdentityResolver(JdbcProfileStore profiles) {
        this.profiles = profiles;
    }

    public Optional<ResolvedIdentity> resolve(String issuer, String subject) {
        if (issuer == null || issuer.isBlank() || subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        return profiles.findByIdentity(issuer, subject)
                .filter(snapshot -> snapshot.profile().status() == ProfileStatus.ACTIVE)
                .flatMap(snapshot -> snapshot.profile().identities().stream()
                        .filter(identity -> identity.issuer().toString().equals(issuer) && identity.subject().equals(subject))
                        .findFirst()
                        .map(identity -> new ResolvedIdentity(snapshot.profile().profileId().toString(), snapshot.profile().tenantId(), identity.roles(), identity.scopes())));
    }

    public Optional<ResolvedIdentity> resolve(VerifiedIdentity identity) {
        return resolve(identity.issuer().toString(), identity.subject());
    }

    public record ResolvedIdentity(String profileId, String tenantId, Set<String> roles, Set<String> grants) {
        public ResolvedIdentity {
            roles = Set.copyOf(roles);
            grants = Set.copyOf(grants);
        }
    }

}
