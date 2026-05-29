package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationRecord;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public record ToolPublicationInstallResult(ToolRegistrationRecord record, long registryVersion) {

    public ObjectNode toJson(ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("installed", true);
        result.put("registrationId", record.registrationId());
        result.put("toolId", record.toolId());
        result.put("phase", record.phase().wireName());
        result.put("sourceKind", record.sourceKind().name());
        result.put("status", record.status());
        result.put("registryVersion", registryVersion);
        if (record.runtimeModuleId() != null) {
            result.put("runtimeModuleId", record.runtimeModuleId());
        }

        ObjectNode source = objectMapper.createObjectNode();
        record.source().forEach(source::put);
        result.set("source", source);

        ObjectNode provenance = objectMapper.createObjectNode();
        provenance.put("actor", record.actor());
        provenance.put("registeredAt", record.registeredAt().toString());
        if (record.requestId() != null) {
            provenance.put("requestId", record.requestId());
        }
        result.set("provenance", provenance);

        ArrayNode registeredFunctions = objectMapper.createArrayNode();
        record.registeredFunctions().forEach(registeredFunctions::add);
        result.set("registeredFunctions", registeredFunctions);
        return result;
    }
}
