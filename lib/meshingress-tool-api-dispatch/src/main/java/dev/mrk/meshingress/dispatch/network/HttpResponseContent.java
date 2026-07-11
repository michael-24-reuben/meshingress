package dev.mrk.meshingress.dispatch.network;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class HttpResponseContent extends StructuredContent {
    @Setter
    private String url;
    @Setter
    private String method;
    @Setter
    private Integer status;
    private Map<String, String> headers = new LinkedHashMap<>();
    @Setter
    private JsonNode body;
    @Setter
    private Long durationMs;

    public HttpResponseContent() {
        super(StructuredContentKind.Network.NETWORK_HTTP_RESPONSE);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

}
