package dev.mrk.toolspace.instagram.instafetch;

import tools.jackson.databind.JsonNode;

public final class InstaFetchSettledResult {
    private final FetchPath path;
    private final boolean ok;
    private final JsonNode data;
    private final InstaFetchResult result;
    private final JsonNode error;

    private InstaFetchSettledResult(FetchPath path, boolean ok, JsonNode data, InstaFetchResult result, JsonNode error) {
        this.path = path;
        this.ok = ok;
        this.data = data;
        this.result = result;
        this.error = error;
    }

    public static InstaFetchSettledResult success(FetchPath path, JsonNode data) {
        return new InstaFetchSettledResult(path, true, data, InstaFetchResult.from(data), null);
    }

    public static InstaFetchSettledResult failure(FetchPath path, JsonNode error) {
        return new InstaFetchSettledResult(path, false, null, null, error);
    }

    public FetchPath path() {
        return path;
    }

    public boolean ok() {
        return ok;
    }

    public JsonNode data() {
        return data;
    }

    public InstaFetchResult result() {
        return result;
    }

    public JsonNode error() {
        return error;
    }
}
