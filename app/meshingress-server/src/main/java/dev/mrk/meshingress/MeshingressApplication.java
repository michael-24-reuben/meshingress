package dev.mrk.meshingress;

import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.documentation.MeshingressDocumentationProperties;
import dev.mrk.meshingress.documentation.dispatch.DispatchDocumentationService;
import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@SpringBootApplication
@EnableConfigurationProperties({MeshingressProperties.class, MeshingressRepositoryProperties.class})
public class MeshingressApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeshingressApplication.class);

    static void main(String[] args) {
        SpringApplication.run(MeshingressApplication.class, args);
    }

    @Bean
    ApplicationRunner serverPropertiesPrinter(ServerProperties serverProperties, MeshingressProperties properties) {
        return args -> {
            String host = serverProperties.getAddress() != null
                    ? serverProperties.getAddress().getHostAddress()
                    : "localhost";
            int port = Objects.requireNonNullElse(serverProperties.getPort(), -1);
            String httpUrl = String.format("http://%s:%d", host, port);
            String wsUrl = properties.mcp().websocket().enabled()
                    ? String.format("ws://%s:%d%s", host, port, properties.mcp().websocket().path())
                    : "disabled";

            System.out.println("\n" +
                    "╔═══════════════════════════════════════════════════════════════════════╗\n" +
                    "║                     Meshingress Server Started                        ║\n" +
                    "╠═══════════════════════════════════════════════════════════════════════╣\n" +
                    "║ Runtime:    " + String.format("%-58s", properties.identity().name()) + "║\n" +
                    "║ Instance:   " + String.format("%-58s", properties.identity().instanceId()) + "║\n" +
                    "║ Environment:" + String.format(" %-57s", properties.identity().environment()) + "║\n" +
                    "║ Server Host: " + String.format("%-57s", host) +                     "║\n" +
                    "║ Server Port: " + String.format("%-57d", port) +                     "║\n" +
                    "║ HTTP URL:    " + String.format("%-57s", httpUrl) +                  "║\n" +
                    "║ WebSocket URL: " + String.format("%-55s", wsUrl) +                  "║\n" +
                    "║ Swagger UI URL: " + String.format("%-54s", httpUrl + "/swagger-ui/index.html") + "║\n" +
                    "╚═══════════════════════════════════════════════════════════════════════╝\n");
        };
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ApplicationRunner documentationSnapshotRefresher(
            ServerProperties serverProperties,
            MeshingressDocumentationProperties documentationProperties,
            DispatchDocumentationService dispatchDocumentationService,
            ObjectProvider<ServletWebServerApplicationContext> webServerContextProvider
    ) {
        return args -> {
            if (!documentationProperties.enabled() || !documentationProperties.refresh().enabled()) {
                return;
            }

            Path dispatchOutput = resolveOutputPath(documentationProperties.refresh().dispatchOutput());
            Path openApiOutput = resolveOutputPath(documentationProperties.refresh().openapiOutput());
            writeFile(dispatchOutput, dispatchDocumentationService.yamlDocument());

            ServletWebServerApplicationContext webServerContext = webServerContextProvider.getIfAvailable();
            if (webServerContext == null || webServerContext.getWebServer() == null) {
                LOGGER.info("Skipping OpenAPI YAML refresh because no servlet web server is active in this context.");
                return;
            }

            String host = serverProperties.getAddress() != null
                    ? serverProperties.getAddress().getHostAddress()
                    : "localhost";
            int port = webServerContext.getWebServer().getPort();
            String openApiYaml = RestClient.create()
                    .get()
                    .uri("http://%s:%d/v3/api-docs.yaml".formatted(host, port))
                    .retrieve()
                    .body(String.class);
            if (openApiYaml == null || openApiYaml.isBlank()) {
                throw new IllegalStateException("OpenAPI YAML endpoint returned an empty document.");
            }
            writeFile(openApiOutput, openApiYaml);
            LOGGER.info(
                    "Documentation refreshed: dispatch={} openapi={}",
                    dispatchOutput.toAbsolutePath(),
                    openApiOutput.toAbsolutePath()
            );
        };
    }

    private static Path resolveOutputPath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        return path.isAbsolute() ? path : Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    private static void writeFile(Path outputPath, String content) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, content);
    }

}
// ws://100.121.15.11:8080/mcp/ws
