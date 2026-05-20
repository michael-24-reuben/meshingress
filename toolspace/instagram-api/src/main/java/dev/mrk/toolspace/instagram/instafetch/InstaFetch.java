package dev.mrk.toolspace.instagram.instafetch;

import dev.mrk.toolspace.instagram.instafetch.dto.InstaGraphQLResponseRoot;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class InstaFetch {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final List<FetchPath> media = new ArrayList<>();
    private final InstaFetchBridge bridge;
    private final InstaFetchOptions options;

    public InstaFetch() {
        this(List.of(), InstaFetchOptions.defaults(), defaultBridge());
    }

    public InstaFetch(String input) {
        this(List.of(FetchPath.from(input)), InstaFetchOptions.defaults(), defaultBridge());
    }

    public InstaFetch(FetchPath input) {
        this(List.of(input), InstaFetchOptions.defaults(), defaultBridge());
    }

    public InstaFetch(Collection<?> inputs) {
        this(inputs, InstaFetchOptions.defaults(), defaultBridge());
    }

    public InstaFetch(String input, InstaFetchOptions options) {
        this(List.of(FetchPath.from(input)), options, defaultBridge());
    }

    public InstaFetch(FetchPath input, InstaFetchOptions options) {
        this(List.of(input), options, defaultBridge());
    }

    public InstaFetch(Collection<?> inputs, InstaFetchOptions options) {
        this(inputs, options, defaultBridge());
    }

    public InstaFetch(InstaFetchBridge bridge) {
        this(List.of(), InstaFetchOptions.defaults(), bridge);
    }

    public InstaFetch(String input, InstaFetchBridge bridge) {
        this(List.of(FetchPath.from(input)), InstaFetchOptions.defaults(), bridge);
    }

    public InstaFetch(FetchPath input, InstaFetchBridge bridge) {
        this(List.of(input), InstaFetchOptions.defaults(), bridge);
    }

    public InstaFetch(Collection<?> inputs, InstaFetchBridge bridge) {
        this(inputs, InstaFetchOptions.defaults(), bridge);
    }

    public InstaFetch(String input, InstaFetchOptions options, InstaFetchBridge bridge) {
        this(List.of(FetchPath.from(input)), options, bridge);
    }

    public InstaFetch(FetchPath input, InstaFetchOptions options, InstaFetchBridge bridge) {
        this(List.of(input), options, bridge);
    }

    public InstaFetch(Collection<?> inputs, InstaFetchOptions options, InstaFetchBridge bridge) {
        this.options = options == null ? InstaFetchOptions.defaults() : options;
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        addAllMedia(inputs);
    }

    public InstaFetch addMedia(String input) {
        return addMedia(FetchPath.from(input));
    }

    public InstaFetch addMedia(FetchPath input) {
        media.add(FetchPath.from(input));
        return this;
    }

    public InstaFetch addAllMedia(Collection<?> inputs) {
        if (inputs == null) {
            return this;
        }
        for (Object input : inputs) {
            if (input instanceof FetchPath path) {
                addMedia(path);
            } else if (input instanceof String path) {
                addMedia(path);
            } else {
                throw new IllegalArgumentException("Media input must be a String or FetchPath");
            }
        }
        return this;
    }

    public InstaFetch clearMedia() {
        media.clear();
        return this;
    }

    public List<FetchPath> getMedia() {
        return List.copyOf(media);
    }

    public List<String> getShortcodes() {
        return media.stream().map(FetchPath::shortcode).toList();
    }

    public JsonNode submitRequest() {
        if (media.size() != 1) {
            throw new InstaFetchException("submitRequest requires exactly one media item; use submitAllRequests for batches");
        }
        return bridge.submitRequest(media.getFirst(), options);
    }

    public InstaGraphQLResponseRoot submitRequestToDto() {
        JsonNode response = submitRequest();

        try {
            return mapper.treeToValue(response, InstaGraphQLResponseRoot.class);
        } catch (Exception e) {
//        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize Instagram GraphQL response", e);
        }
    }

    public InstaGraphDataHandler submitHandledRequest() {
        return InstaGraphDataHandler.from(submitRequest());
    }

    public InstaFetchResult fetchOne() {
        return InstaFetchResult.from(submitRequest());
    }

    public List<InstaGraphDataHandler> submitAllHandledRequests() {
        return submitAllRequests().stream()
                .map(InstaGraphDataHandler::from)
                .toList();
    }

    public List<InstaFetchResult> fetchAll() {
        return submitAllRequests().stream()
                .map(InstaFetchResult::from)
                .toList();
    }

    public List<JsonNode> submitAllRequests() {
        requireMedia();
        return bridge.submitAllRequests(getMedia(), options);
    }

    public List<InstaFetchSettledResult> settleAllRequests() {
        requireMedia();
        return bridge.settleAllRequests(getMedia(), options);
    }

    private void requireMedia() {
        if (media.isEmpty()) {
            throw new InstaFetchException("No Instagram media has been added");
        }
    }

    private static InstaFetchBridge defaultBridge() {
        return NodeInstaFetchBridge.createDefault(new ObjectMapper());
    }
}
