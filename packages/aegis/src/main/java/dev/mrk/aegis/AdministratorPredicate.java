package dev.mrk.aegis;

/** Determines whether an active profile is an administrator in the host's role model. */
@FunctionalInterface
public interface AdministratorPredicate {
    boolean isAdministrator(AuthProfile profile);
}
