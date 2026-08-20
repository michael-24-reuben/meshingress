package dev.mrk.meshingress.documentation.dispatch;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Service
public class DispatchDocumentationService {

    private final DispatchDocumentationBuilder builder;

    public DispatchDocumentationService(DispatchDocumentationBuilder builder) {
        this.builder = builder;
    }

    public Map<String, Object> jsonDocument() {
        return builder.build();
    }

    public String yamlDocument() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setSplitLines(false);
        Yaml yaml = new Yaml(options);
        return yaml.dump(builder.build());
    }
}
