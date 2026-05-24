package dev.mrk.meshingress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;

import dev.mrk.meshingress.config.MeshingressProperties;

import java.util.Objects;

@SpringBootApplication
@EnableConfigurationProperties(MeshingressProperties.class)
public class MeshingressApplication {

    static void main(String[] args) {
        SpringApplication.run(MeshingressApplication.class, args);
    }

    @Bean
    ApplicationRunner serverPropertiesPrinter(
            ServerProperties serverProperties,
            @Value("${meshingress.mcp.websocket.path:/mcp/ws}") String wsPath
    ) {
        return args -> {
            String host = serverProperties.getAddress() != null
                    ? serverProperties.getAddress().getHostAddress()
                    : "localhost";
            int port = Objects.requireNonNullElse(serverProperties.getPort(), -1);
            String httpUrl = String.format("http://%s:%d", host, port);
            String wsUrl = String.format("ws://%s:%d%s", host, port, wsPath);

            System.out.println("\n" +
                    "╔═════════════════════════════════════════════════════════════╗\n" +
                    "║               Meshingress Server Started                    ║\n" +
                    "╠═════════════════════════════════════════════════════════════╣\n" +
                    "║ Server Host: " + String.format("%-47s", host) +            "║\n" +
                    "║ Server Port: " + String.format("%-47d", port) +            "║\n" +
                    "║ HTTP URL:    " + String.format("%-47s", httpUrl) +         "║\n" +
                    "║ WebSocket URL: " + String.format("%-45s", wsUrl) +         "║\n" +
                    "╚═════════════════════════════════════════════════════════════╝\n");
        };
    }

}
// ws://100.121.15.11:8080/mcp/ws