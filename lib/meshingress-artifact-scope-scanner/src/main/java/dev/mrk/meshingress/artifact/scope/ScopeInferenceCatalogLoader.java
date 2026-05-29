package dev.mrk.meshingress.artifact.scope;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScopeInferenceCatalogLoader {
    private final ObjectMapper objectMapper;

    public ScopeInferenceCatalogLoader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ScopeInferenceCatalog load(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return load(inputStream);
        }
    }

    public ScopeInferenceCatalog load(InputStream inputStream) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);
        ScopeInferenceCatalog catalog = new ScopeInferenceCatalog(
                root.path("version").asString(""),
                parseRules(root.path("rules"))
        );
        catalog.validate();
        return catalog;
    }

    private List<ScopeInferenceRule> parseRules(JsonNode rulesNode) {
        List<ScopeInferenceRule> rules = new ArrayList<>();
        for (JsonNode ruleNode : rulesNode) {
            rules.add(new ScopeInferenceRule(
                    ruleNode.path("id").asString(""),
                    ruleNode.path("scope").asString(""),
                    parseMatchers(ruleNode.path("matchers"))
            ));
        }
        return List.copyOf(rules);
    }

    private List<ScopeMatcherDefinition> parseMatchers(JsonNode matchersNode) {
        List<ScopeMatcherDefinition> matchers = new ArrayList<>();
        for (JsonNode matcherNode : matchersNode) {
            matchers.add(new ScopeMatcherDefinition(
                    ScopeMatcherType.fromWireName(matcherNode.path("type").asString("")),
                    blankToNull(matcherNode.path("owner").asString("")),
                    blankToNull(matcherNode.path("namePattern").asString("")),
                    blankToNull(matcherNode.path("descriptorPattern").asString("")),
                    matcherNode.path("confidence").asString("unknown"),
                    matcherNode.path("reviewOnly").asBoolean(false)
            ));
        }
        return List.copyOf(matchers);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
