package dev.mrk.meshingress.security.management;

import dev.mrk.meshingress.api.McpAuthenticationMethod;
import dev.mrk.meshingress.api.McpPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPolicyServiceTest {
    @Test
    void appliesTenantPoliciesByPriorityWithDenyOverrides() {
        ToolPolicyService policies = service();
        McpPrincipal allowed = principal("tenant-a", Set.of("catalog.read"));

        policies.create("tenant-a", "catalog.search", StoredToolPolicy.Effect.ALLOW, Set.of("catalog.read"), 10, true);
        assertTrue(policies.evaluate("catalog.search", allowed).allowed());

        policies.create("tenant-b", "catalog.search", StoredToolPolicy.Effect.DENY, Set.of(), 100, true);
        assertTrue(policies.evaluate("catalog.search", allowed).allowed(), "other tenants cannot affect this decision");

        policies.create("tenant-a", "catalog.search", StoredToolPolicy.Effect.DENY, Set.of(), 10, true);
        assertFalse(policies.evaluate("catalog.search", allowed).allowed(), "DENY wins at the selected priority");
    }

    @Test
    void deniesWhenNoHighestPriorityAllowPolicyHasEveryGrant() {
        ToolPolicyService policies = service();
        policies.create("tenant-a", "catalog.search", StoredToolPolicy.Effect.ALLOW, Set.of("catalog.write"), 1, true);
        assertFalse(policies.evaluate("catalog.search", principal("tenant-a", Set.of("catalog.read"))).allowed());
    }

    private static ToolPolicyService service() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:tool-policy-" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        return new ToolPolicyService(new JdbcTemplate(dataSource), new ObjectMapper(), java.time.Clock.systemUTC());
    }

    private static McpPrincipal principal(String tenant, Set<String> grants) {
        return new McpPrincipal("user", "https://issuer.example", McpAuthenticationMethod.OIDC_JWT,
                Instant.now().plusSeconds(300), Set.of(), grants, "", tenant);
    }
}
