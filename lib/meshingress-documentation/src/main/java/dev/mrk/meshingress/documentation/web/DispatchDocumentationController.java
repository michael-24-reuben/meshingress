package dev.mrk.meshingress.documentation.web;

import dev.mrk.meshingress.documentation.dispatch.DispatchDocumentationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "meshingress.documentation.dispatch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DispatchDocumentationController {

    private final DispatchDocumentationService service;

    public DispatchDocumentationController(DispatchDocumentationService service) {
        this.service = service;
    }

    @GetMapping("${meshingress.documentation.dispatch.json-path:/v3/dispatch-docs}")
    public Map<String, Object> dispatchJson() {
        return service.jsonDocument();
    }

    @GetMapping(value = "${meshingress.documentation.dispatch.yaml-path:/v3/dispatch-docs.yaml}", produces = {
            "application/yaml", "text/yaml", MediaType.TEXT_PLAIN_VALUE
    })
    public ResponseEntity<String> dispatchYaml() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(service.yamlDocument());
    }
}
