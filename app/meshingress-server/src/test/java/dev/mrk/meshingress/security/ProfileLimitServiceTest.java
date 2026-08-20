package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.McpPrincipal;
import dev.mrk.meshingress.api.McpAuthenticationMethod;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.security.management.JdbcProfileStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileLimitServiceTest {
    @Test
    void enabledRequestLimitRejectsTheNextRequestWithStableReason() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:limits-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        MeshingressProperties properties = properties(true, 1);
        ProfileLimitService limits = new ProfileLimitService(properties, new JdbcProfileStore(jdbc, new ObjectMapper()), jdbc, new ObjectMapper());

        limits.reserveRequest(context()).close();
        JsonRpcException failure = assertThrows(JsonRpcException.class, () -> limits.reserveRequest(context()));

        assertEquals(JsonRpcErrorCodes.RATE_LIMITED, failure.code());
        assertEquals("PROFILE_LIMIT_EXCEEDED", failure.data().path("reason").asString());
    }

    @Test
    void disabledDeploymentSwitchDoesNotDenyTheSameConfiguredLimit() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:limits-off-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        ProfileLimitService limits = new ProfileLimitService(properties(false, 1), new JdbcProfileStore(jdbc, new ObjectMapper()), jdbc, new ObjectMapper());
        limits.reserveRequest(context()).close();
        limits.reserveRequest(context()).close();
    }

    private MeshingressProperties properties(boolean enabled, int requests) {
        MeshingressProperties.Security security = new MeshingressProperties.Security(true, "dev", false, false, "", false, true, true, true, true,
                null, null, new MeshingressProperties.Security.Limits(enabled, true, requests, Duration.ofMinutes(1), 0, Duration.ofMinutes(1), 0));
        return new MeshingressProperties(null, null, null, null, null, security, null, null, null, null, null);
    }

    private McpCallContext context() {
        return new McpCallContext(null, new McpPrincipal("subject", "issuer", McpAuthenticationMethod.OIDC_JWT, null, Set.of(), Set.of(), "", ""), null, null, null);
    }
}
