package dev.mrk.meshingress.documentation.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@ConditionalOnProperty(prefix = "meshingress.documentation.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiAliasController {

    @GetMapping("${meshingress.documentation.openapi.json-path:/v3/openapi-docs}")
    public ResponseEntity<Void> openApiJson() {
        return redirect("/v3/api-docs");
    }

    @GetMapping("${meshingress.documentation.openapi.yaml-path:/v3/openapi-docs.yaml}")
    public ResponseEntity<Void> openApiYaml() {
        return redirect("/v3/api-docs.yaml");
    }

    private ResponseEntity<Void> redirect(String path) {
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .header(HttpHeaders.LOCATION, URI.create(path).toString())
                .build();
    }
}
