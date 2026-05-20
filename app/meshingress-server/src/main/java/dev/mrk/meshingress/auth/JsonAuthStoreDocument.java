package dev.mrk.meshingress.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class JsonAuthStoreDocument {

    @Setter
    private int schemaVersion = 1;
    private Bootstrap bootstrap = new Bootstrap();
    private List<AdminUser> admins = new ArrayList<>();
    private List<McpCredential> mcpCredentials = new ArrayList<>();

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap == null ? new Bootstrap() : bootstrap;
    }

    public void setAdmins(List<AdminUser> admins) {
        this.admins = admins == null ? new ArrayList<>() : admins;
    }

    public void setMcpCredentials(List<McpCredential> mcpCredentials) {
        this.mcpCredentials = mcpCredentials == null ? new ArrayList<>() : mcpCredentials;
    }

    @Setter
    @Getter
    public static class Bootstrap {
        private String username;
        private String password;
        private String source;
        private String createdAt;
        private String consumedAt;

    }

    @Setter
    @Getter
    public static class AdminUser {
        private String id;
        private String username;
        private String passwordHash;
        private String email;
        private String displayName;
        private String createdAt;
        private String passwordSource;
    }

    @Setter
    @Getter
    public static class McpCredential {
        private String id;
        private String accessToken;
        private String secretKey;
        private String authToken;
        private String clientId;
        private String subject;
        private String createdAt;
        private String createdByAdminId;
        private boolean enabled = true;
        private boolean admin = true;

    }
}
