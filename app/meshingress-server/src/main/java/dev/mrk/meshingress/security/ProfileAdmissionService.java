package dev.mrk.meshingress.security;

import dev.mrk.aegis.ProfileDraft;
import dev.mrk.aegis.ProfileService;
import dev.mrk.aegis.ProfileSnapshot;
import dev.mrk.aegis.ProfileStatus;
import dev.mrk.aegis.VerifiedIdentity;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.security.management.JdbcProfileStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Server-owned admission boundary shared by every verified identity provider.
 * It accepts a {@link VerifiedIdentity}, never browser profile fields or raw credentials.
 */
@Service
public final class ProfileAdmissionService {
    private final JdbcProfileStore store;
    private final ProfileService profiles;
    private final MeshingressProperties properties;

    public ProfileAdmissionService(JdbcProfileStore store, MeshingressProperties properties) {
        this.store = store;
        this.profiles = new ProfileService(store);
        this.properties = properties;
    }

    public AdmissionResult admit(VerifiedIdentity identity) {
        Optional<ProfileSnapshot> existing = store.findByIdentity(identity.issuer().toString(), identity.subject());
        if (existing.isPresent()) return new AdmissionResult(existing.get(), false, "EXISTING_BINDING");

        MeshingressProperties.Security.Admission admission = properties.security().admission();
        if (admission.mode() != MeshingressProperties.Security.AdmissionMode.SELF_SERVICE_UNPRIVILEGED) {
            return new AdmissionResult(null, false, "ADMISSION_CLOSED");
        }
        if (admission.defaultTenantId().isBlank()) {
            return new AdmissionResult(null, false, "ADMISSION_TENANT_NOT_CONFIGURED");
        }
        ProfileStatus status = admission.initialProfileStatus() == MeshingressProperties.Security.InitialProfileStatus.ACTIVE
                ? ProfileStatus.ACTIVE : ProfileStatus.PENDING_REVIEW;
        LinkedHashMap<String, String> labels = new LinkedHashMap<>(identity.displayAttributes());
        labels.put("identityProvider", identity.issuer().getHost() == null ? identity.issuer().toString() : identity.issuer().getHost());
        try {
            ProfileSnapshot created = profiles.create(new ProfileDraft(admission.defaultTenantId(), identity.displayName(), labels,
                    List.of(identity.asProfileIdentity()), List.of()), status);
            return new AdmissionResult(created, true, "SELF_SERVICE_UNPRIVILEGED");
        } catch (IllegalArgumentException duplicate) {
            return store.findByIdentity(identity.issuer().toString(), identity.subject())
                    .map(value -> new AdmissionResult(value, false, "EXISTING_BINDING"))
                    .orElseThrow(() -> duplicate);
        }
    }

    public record AdmissionResult(ProfileSnapshot profile, boolean created, String reason) { }
}
