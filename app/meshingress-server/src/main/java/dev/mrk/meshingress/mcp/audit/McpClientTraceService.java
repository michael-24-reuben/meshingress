package dev.mrk.meshingress.mcp.audit;

import dev.mrk.meshingress.api.McpCallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Retains a small, bounded session-to-client-profile index and writes a
 * redacted request trace. It deliberately never reads or logs credentials.
 */
@Service
public class McpClientTraceService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("dev.mrk.meshingress.audit.mcp");
    private static final int MAX_PROFILES = 1_024;
    private static final int MAX_VALUE_LENGTH = 256;

    private final Map<String, McpClientProfile> profiles = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, McpClientProfile> eldest) {
            return size() > MAX_PROFILES;
        }
    };

    public void record(JsonNode request, McpCallContext context) {
        if (request == null || !request.isObject()) {
            return;
        }

        String method = clean(request.path("method").asString(""));
        String sessionId = clean(context == null ? null : context.sessionId());
        McpClientProfile profile = "initialize".equals(method)
                ? recordInitialize(request.path("params"), sessionId)
                : profile(sessionId).orElse(null);

        AUDIT_LOG.info(
                "event=mcp_request method={} requestId={} sessionId={} clientProfile={} clientName={} clientVersion={} clientProtocolVersion={}",
                method,
                clean(context == null ? null : context.requestId()),
                sessionId,
                profile == null ? "absent" : "reported",
                profile == null ? "" : profile.name(),
                profile == null ? "" : profile.version(),
                profile == null ? "" : profile.protocolVersion()
        );
    }

    public Optional<McpClientProfile> profile(String sessionId) {
        String key = clean(sessionId);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        synchronized (profiles) {
            return Optional.ofNullable(profiles.get(key));
        }
    }

    private McpClientProfile recordInitialize(JsonNode params, String sessionId) {
        JsonNode clientInfo = params.path("clientInfo");
        McpClientProfile profile = new McpClientProfile(
                clean(clientInfo.path("name").asString("")),
                clean(clientInfo.path("version").asString("")),
                clean(params.path("protocolVersion").asString("")),
                OffsetDateTime.now()
        );
        if (!sessionId.isEmpty()) {
            synchronized (profiles) {
                profiles.put(sessionId, profile);
            }
        }
        return profile;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").strip();
        return normalized.length() <= MAX_VALUE_LENGTH
                ? normalized
                : normalized.substring(0, MAX_VALUE_LENGTH);
    }
}
