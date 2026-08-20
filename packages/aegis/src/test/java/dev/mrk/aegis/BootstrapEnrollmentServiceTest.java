package dev.mrk.aegis;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapEnrollmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T20:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void enrollsExactlyOneFirstAdministratorAndConsumesTheIssuance() {
        UUID issuanceId = UUID.randomUUID();
        InMemoryBootstrapEnrollmentStore store = new InMemoryBootstrapEnrollmentStore();
        BootstrapEnrollmentService service = service(store, issuanceId, 3);
        service.issue(issuanceId);

        BootstrapEnrollmentResult first = service.enroll(new TestCredential("never persisted"), administratorProfile("first"));
        BootstrapEnrollmentResult second = service.enroll(new TestCredential("never persisted"), administratorProfile("second"));

        assertTrue(first.enrolled());
        assertEquals(DenialReason.BOOTSTRAP_CREDENTIAL_CONSUMED, second.denialReason());
        assertEquals(BootstrapIssuanceState.CONSUMED, store.findIssuance(issuanceId).orElseThrow().state());
        assertFalse(first.toString().contains("never persisted"));
    }

    @Test
    void deniesExpiredRevokedAndWrongActionWithoutCreatingAnAdministrator() {
        UUID issuanceId = UUID.randomUUID();
        InMemoryBootstrapEnrollmentStore store = new InMemoryBootstrapEnrollmentStore();
        BootstrapEnrollmentService service = service(store, issuanceId, 3);
        store.issue(new BootstrapIssuance(issuanceId, NOW.minusSeconds(60), NOW.minusSeconds(1), 3, 0,
                BootstrapIssuanceState.ACTIVE, NOW.minusSeconds(60)));

        assertEquals(DenialReason.BOOTSTRAP_CREDENTIAL_EXPIRED,
                service.enroll(new TestCredential("expired"), administratorProfile("expired")).denialReason());

        UUID revokedId = UUID.randomUUID();
        service.issue(revokedId);
        service.revoke(revokedId);
        BootstrapEnrollmentService revokedService = service(store, revokedId, 3);
        assertEquals(DenialReason.BOOTSTRAP_CREDENTIAL_REVOKED,
                revokedService.enroll(new TestCredential("revoked"), administratorProfile("revoked")).denialReason());
        assertEquals(DenialReason.BOOTSTRAP_INVALID_ACTION,
                revokedService.enroll("ordinary-tool-call", new TestCredential("wrong-action"), administratorProfile("wrong")).denialReason());
    }

    @Test
    void exhaustsOnlyAfterAcceptedEvidenceUsesAllAllowedAttempts() {
        UUID issuanceId = UUID.randomUUID();
        InMemoryBootstrapEnrollmentStore store = new InMemoryBootstrapEnrollmentStore();
        BootstrapEnrollmentService service = service(store, issuanceId, 2);
        service.issue(issuanceId);

        assertEquals(DenialReason.BOOTSTRAP_PROFILE_NOT_ADMINISTRATOR,
                service.enroll(new TestCredential("one"), nonAdministratorProfile("one")).denialReason());
        assertEquals(DenialReason.BOOTSTRAP_PROFILE_NOT_ADMINISTRATOR,
                service.enroll(new TestCredential("two"), nonAdministratorProfile("two")).denialReason());
        assertEquals(DenialReason.BOOTSTRAP_ATTEMPTS_EXHAUSTED,
                service.enroll(new TestCredential("three"), administratorProfile("three")).denialReason());
        assertEquals(BootstrapIssuanceState.EXHAUSTED, store.findIssuance(issuanceId).orElseThrow().state());
    }

    @Test
    void concurrentContendersHaveOneWinner() throws Exception {
        UUID issuanceId = UUID.randomUUID();
        BootstrapEnrollmentService service = service(new InMemoryBootstrapEnrollmentStore(), issuanceId, 10);
        service.issue(issuanceId);

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Callable<BootstrapEnrollmentResult>> calls = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(index -> (Callable<BootstrapEnrollmentResult>) () -> service.enroll(
                            new TestCredential("evidence-" + index), administratorProfile("candidate-" + index)))
                    .toList();
            List<BootstrapEnrollmentResult> results = executor.invokeAll(calls).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertEquals(1, results.stream().filter(BootstrapEnrollmentResult::enrolled).count());
        }
    }

    @Test
    void rotationIsExplicitAndInvalidatesThePrecedingIssuance() {
        UUID priorId = UUID.randomUUID();
        UUID replacementId = UUID.randomUUID();
        InMemoryBootstrapEnrollmentStore store = new InMemoryBootstrapEnrollmentStore();
        BootstrapEnrollmentService service = service(store, priorId, 3);
        service.issue(priorId);

        service.rotate(priorId, replacementId);

        assertEquals(BootstrapIssuanceState.REVOKED, store.findIssuance(priorId).orElseThrow().state());
        assertEquals(BootstrapIssuanceState.ACTIVE, store.findIssuance(replacementId).orElseThrow().state());
        assertThrows(IllegalStateException.class, () -> disabledService(store, replacementId).issue(UUID.randomUUID()));
    }

    private static BootstrapEnrollmentService service(InMemoryBootstrapEnrollmentStore store, UUID acceptedIssuanceId, int attempts) {
        BootstrapEnrollmentProperties properties = new BootstrapEnrollmentProperties(
                true, "bootstrap-enroll-admin", java.time.Duration.ofMinutes(15), attempts, "admin"
        );
        return new BootstrapEnrollmentService(properties, credential ->
                new BootstrapCredentialVerification.Accepted(acceptedIssuanceId), store,
                new RoleAdministratorPredicate("admin"), CLOCK);
    }

    private static BootstrapEnrollmentService disabledService(InMemoryBootstrapEnrollmentStore store, UUID acceptedIssuanceId) {
        BootstrapEnrollmentProperties properties = new BootstrapEnrollmentProperties(
                false, "bootstrap-enroll-admin", java.time.Duration.ofMinutes(15), 3, "admin"
        );
        return new BootstrapEnrollmentService(properties, credential ->
                new BootstrapCredentialVerification.Accepted(acceptedIssuanceId), store,
                new RoleAdministratorPredicate("admin"), CLOCK);
    }

    private static AuthProfile administratorProfile(String suffix) {
        return profile(suffix, Set.of("admin"));
    }

    private static AuthProfile nonAdministratorProfile(String suffix) {
        return profile(suffix, Set.of("user"));
    }

    private static AuthProfile profile(String suffix, Set<String> roles) {
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                "identity-" + suffix, URI.create("https://issuer.example"), "subject-" + suffix,
                AuthenticationMethod.LOCAL_DEVELOPMENT, Set.of("aegis"), Set.of(), roles, NOW
        );
        return new AuthProfile(1, UUID.randomUUID(), ProfileStatus.ACTIVE,
                new AuthProfileMetadata("Administrator " + suffix, NOW, NOW, Map.of("source", "bootstrap")),
                List.of(identity), List.of());
    }

    private record TestCredential(String secret) implements Credential {
    }
}
