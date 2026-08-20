package dev.mrk.aegis;

import java.util.Objects;

/** Administrator predicate based on a configured normalized identity role. */
public final class RoleAdministratorPredicate implements AdministratorPredicate {
    private final String administratorRole;

    public RoleAdministratorPredicate(String administratorRole) {
        if (administratorRole == null || administratorRole.isBlank()) {
            throw new IllegalArgumentException("administratorRole must not be blank");
        }
        this.administratorRole = administratorRole;
    }

    @Override
    public boolean isAdministrator(AuthProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return profile.status() == ProfileStatus.ACTIVE
                && profile.identities().stream().anyMatch(identity -> identity.roles().contains(administratorRole));
    }
}
