package dev.mrk.meshingress.repository;

import dev.mrk.meshingress.repository.config.MeshingressRepositoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MeshingressRepositoryProperties.class)
public class MeshingressRepositoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeshingressRepositoryApplication.class, args);
    }
}
