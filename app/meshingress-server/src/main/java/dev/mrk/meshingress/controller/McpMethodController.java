package dev.mrk.meshingress.controller;

import dev.mrk.meshingress.api.McpCallContext;
import tools.jackson.databind.JsonNode;

import java.util.Set;

public interface McpMethodController {

    Set<String> supportedMethods();

    boolean supports(String method);

    JsonNode dispatch(String method, JsonNode params, McpCallContext context);
}
