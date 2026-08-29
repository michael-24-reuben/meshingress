# Nextcloud MCP Config SDK Manual

Module: `nextcloud-sdk-config`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-config:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.config`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Configuration Records (`io.github.uriakleahcim.nextcloud.config`)](#configuration-records-orgmcpnextcloudconfig)
   - [Config Loaders & Path Resolution](#config-loaders--path-resolution)
   - [Secret Resolution & Environment (`EnvFile`, `EnvironmentSecretResolver`)](#secret-resolution--environment)
   - [Local Account Store & Repository (`LocalUserAccountStore`)](#local-account-store--repository)
   - [Validation Engine (`io.github.uriakleahcim.nextcloud.config.validation`)](#validation-engine-orgmcpnextcloudconfigvalidation)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-config` module provides strongly-typed configuration models, YAML loaders, environment/dotenv secret resolvers, multi-account storage, and semantic validation rules for Nextcloud MCP server and client instances.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.config {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires jakarta.validation;

    exports io.github.uriakleahcim.nextcloud.config;
    exports io.github.uriakleahcim.nextcloud.config.validation;

    opens io.github.uriakleahcim.nextcloud.config to com.fasterxml.jackson.databind;
    opens io.github.uriakleahcim.nextcloud.config.validation to com.fasterxml.jackson.databind;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-config</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Configuration Records (`io.github.uriakleahcim.nextcloud.config`)

#### [`NextcloudMcpConfig`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/NextcloudMcpConfig.java)
- **Description**: Root configuration record for the server and client runtime.
- **Components**:
  - `ServerConfig server()`: Server networking and protocol configuration.
  - `Map<String, NextcloudAccountConfig> accounts()`: Immutable map of account keys to account definitions.
  - `ToolPolicyConfig tools()`: Tool execution permissions and catalog settings.
  - `NextcloudAdminConfig admin()`: Admin feature configuration.

#### [`NextcloudAccountConfig`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/NextcloudAccountConfig.java)
- **Description**: Individual Nextcloud account credential and endpoint model.
- **Components**:
  - `String baseUrl()`: Base Nextcloud URL (e.g., `https://cloud.example.com`).
  - `String username()`: Login username or ID (e.g., `temporary`).
  - `String appPassword()`: App-specific password or token.
  - `boolean enabled()`: Whether account is enabled for use.
  - `boolean admin()`: Whether account possesses administrative privileges.
  - `String profileUrl()`: Optional profile URL for identification.

#### [`NextcloudAdminConfig`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/NextcloudAdminConfig.java)
- **Description**: Admin orchestration policy record.
- **Components**:
  - `boolean enabled()`: Whether admin capabilities are enabled.
  - `String accountId()`: Account key in `accounts` map designated for admin operations.

#### [`ServerConfig`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/ServerConfig.java)
- **Components**: `String host()`, `int port()`, `String path()`, `String transport()`.

#### [`ToolPolicyConfig`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/ToolPolicyConfig.java)
- **Components**: `List<String> enabled()`, `List<String> disabled()`, `int rateLimitPerMinute()`.

---

### Config Loaders & Path Resolution

#### [`ConfigPaths`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/ConfigPaths.java)
- `static Optional<Path> findFromSystemSources(String explicitPath)`: Checks explicit path parameter, system property `nextcloud.mcp.config`, environment variable `NEXTCLOUD_MCP_CONFIG`, or standard candidate files (`config/server.yaml`, `nextcloud-sdk.yaml`, etc.).
- `static Optional<Path> findDefault()`: Traverses directory tree upward looking for default YAML configuration paths.

#### [`YamlConfigLoader`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/YamlConfigLoader.java)
- `new YamlConfigLoader()`: Default constructor initializing Jackson YAML mapper.
- `NextcloudMcpConfig load(Path path) throws IOException`: Deserializes YAML configuration file and transparently merges persisted local accounts.

---

### Secret Resolution & Environment

#### [`EnvFile`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/EnvFile.java)
- `static Map<String, String> load(Path path)`: Parses `.env` format files with support for quotes, comments, and `export` prefixes.
- `static Optional<Path> findDefault()`: Traverses directories upward seeking `.env` file.

#### [`EnvironmentSecretResolver`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/EnvironmentSecretResolver.java)
- `new EnvironmentSecretResolver()`: Resolves secrets from `System.getenv()`, `System.getProperty()`, and local `.env` scratch file.
- `Optional<String> resolve(String key)`: Looks up secret key.

---

### Local Account Store & Repository

#### [`LocalUserAccountRepository`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/LocalUserAccountRepository.java)
- `static List<LocalUserAccountRecord> list(Path configPath)`: Lists local user accounts stored under `config/db/u/*.json`.
- `static void save(Path configPath, LocalUserAccountRecord record)`: Persists an account record.
- `static void delete(Path configPath, String accountKey)`: Deletes an account JSON file.

#### [`LocalUserAccountStore`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/LocalUserAccountStore.java)
- `static NextcloudMcpConfig mergeUserAccounts(NextcloudMcpConfig config, Path configPath)`: Combines static YAML configuration with dynamic local accounts and resolves default admin flags.

---

### Validation Engine (`io.github.uriakleahcim.nextcloud.config.validation`)

#### [`ConfigValidator`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/validation/ConfigValidator.java)
- `List<ConfigValidationError> validate(NextcloudMcpConfig config)`: Performs validation:
  - Account URIs must be absolute with scheme and host.
  - Username and app passwords must not be blank.
  - Admin accounts must be explicitly enabled.
  - Designated admin account reference must exist in accounts map and be flagged `admin: true` and `enabled: true`.

#### [`ConfigValidationError`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-config/src/main/java/org/mcp/nextcloud/config/validation/ConfigValidationError.java)
- `String path()`: JSON/YAML path where error was detected (e.g., `accounts.temporary.baseUrl`).
- `String message()`: Descriptive error message.

---

## Complete SDK Usage Snippets

### 1. Loading and Validating Server Configuration

```java
import java.nio.file.Path;
import java.util.List;

import io.github.uriakleahcim.nextcloud.config.ConfigPaths;
import io.github.uriakleahcim.nextcloud.config.NextcloudMcpConfig;
import io.github.uriakleahcim.nextcloud.config.YamlConfigLoader;
import validation.io.github.uriakleahcim.nextcloud.config.ConfigValidationError;
import validation.io.github.uriakleahcim.nextcloud.config.ConfigValidator;

Path configPath = ConfigPaths.findFromSystemSources("config/server.yaml")
        .orElseThrow(() -> new IllegalStateException("Config file not found"));

YamlConfigLoader loader = new YamlConfigLoader();
NextcloudMcpConfig config = loader.load(configPath);

ConfigValidator validator = new ConfigValidator();
List<ConfigValidationError> errors = validator.validate(config);
if(!errors.

isEmpty()){
        errors.

forEach(e ->System.err.

println(e.path() +": "+e.

message()));
        throw new

IllegalArgumentException("Invalid Nextcloud MCP Configuration");
}

        System.out.

println("Loaded "+config.accounts().

size() +" accounts successfully.");
```

### 2. Resolving Secrets from Environment and `.env`

```java
import io.github.uriakleahcim.nextcloud.config.EnvironmentSecretResolver;

EnvironmentSecretResolver secretResolver = new EnvironmentSecretResolver();
String appPassword = secretResolver.resolve("NEXTCLOUD_APP_PASSWORD")
        .orElseThrow(() -> new IllegalStateException("NEXTCLOUD_APP_PASSWORD not configured"));
```
