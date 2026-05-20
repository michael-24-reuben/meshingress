package dev.mrk.toolspace.instagram.instafetch;

import tools.jackson.databind.JsonNode;

import java.util.List;

public interface InstaFetchBridge {
    JsonNode submitRequest(FetchPath path, InstaFetchOptions options);

    List<JsonNode> submitAllRequests(List<FetchPath> paths, InstaFetchOptions options);

    List<InstaFetchSettledResult> settleAllRequests(List<FetchPath> paths, InstaFetchOptions options);
}
