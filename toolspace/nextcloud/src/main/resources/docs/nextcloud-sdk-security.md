# Nextcloud MCP Security SDK Manual

Module: `nextcloud-sdk-security`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-security:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.security`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Principals & Context (`Principal`, `PrincipalContext`)](#principals--context)
   - [Fine-Grained Scopes (`Scope`, `Scopes`)](#fine-grained-scopes)
   - [Scope & Policy Evaluators (`ScopeEvaluator`, `ToolAccessPolicy`, `AccountAccessPolicy`)](#scope--policy-evaluators)
   - [Destructive Action Guard (`ConfirmationTokenEvaluator`)](#destructive-action-guard)
   - [Audit Trail & Masking (`AuditSink`, `AuditEvent`, `SecretMasker`)](#audit-trail--masking)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-security` module provides an authorization and audit framework for Nextcloud MCP operations. It enforces fine-grained scopes, principal account boundaries, confirmation token generation/consumption for destructive operations, secret redaction, and audit logging.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.security {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires static org.jetbrains.annotations;
    requires static org.slf4j;

    exports io.github.uriakleahcim.nextcloud.security;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Principals & Context

#### [`Principal`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/Principal.java)
- **Description**: Security subject invoking MCP tools.
- **Components**:
  - `PrincipalId id()`: Principal identifier.
  - `Set<Scope> scopes()`: Explicitly granted scopes.
  - `Set<AccountId> allowedAccounts()`: White-listed account IDs (empty implies all accounts if permitted).
  - `boolean isAdmin()`: Admin bypass flag.
- **Callable**:
  - `static Principal anonymous()`: Anonymous principal with no scopes.
  - `static Principal superAdmin(PrincipalId id)`: Admin principal with all scopes granted.

#### [`PrincipalContext`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/PrincipalContext.java)
- `static Principal current()`: Retrieves active thread principal (or anonymous if unassigned).
- `static AutoCloseable open(Principal principal)`: Scopes principal to current try-with-resources execution block.

---

### Fine-Grained Scopes

#### [`Scope`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/Scope.java)
- **Components**: `String id()`, `String description()`, `boolean destructive()`, `Set<Scope> prerequisites()`.
- **Callable**:
  - `Set<Scope> flattenedPrerequisites()`: Recursively collects all transitive prerequisite scopes.

#### [`Scopes`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/Scopes.java)
- **Hierarchy Namespaces**:
  - `Scopes.Files`: `READ`, `WRITE`, `DELETE`, `SEARCH`, `FAVORITE`, `LIST`, `STAT`, `DOWNLOAD`, `UPLOAD`, `MKDIR`, `MOVE`, `COPY`, `ALL`
  - `Scopes.Shares`: `READ`, `WRITE`, `DELETE`, `ALL`
  - `Scopes.Sharees`: `READ`, `ALL`
  - `Scopes.User`: `READ`, `CAPABILITIES_READ`, `STATUS_READ`, `STATUS_WRITE`, `PREFERENCES_READ`, `PREFERENCES_WRITE`, `ALL`
  - `Scopes.Comments`: `READ`, `LIST`, `WRITE`, `CREATE`, `UPDATE`, `DELETE`, `MARK_READ`, `ALL`
  - `Scopes.Trash`: `READ`, `LIST`, `RESTORE`, `DELETE`, `EMPTY`, `ALL`
  - `Scopes.Versions`: `READ`, `LIST`, `RESTORE`, `DOWNLOAD`, `ALL`
  - `Scopes.Status`: `READ`, `GET`, `PREDEFINED`, `WRITE`, `SET`, `MESSAGE_SET`, `MESSAGE_CLEAR`, `ALL`
  - `Scopes.Admin.Users`: `READ`, `WRITE`, `DISABLE`, `DELETE`, `ALL`
  - `Scopes.Admin.Groups`: `READ`, `WRITE`, `DELETE`, `ALL`
  - `Scopes.Admin.Subadmins`: `READ`, `WRITE`, `ALL`
  - `Scopes.Admin.Apps`: `READ`, `WRITE`, `ALL`
  - `Scopes.Admin.Occ`: `PLAN`, `EXECUTE`, `ALL`
  - `Scopes.Risk`: `DESTRUCTIVE`, `CRITICAL`, `ALL`
  - `Scopes.ALL`: Comprehensive union set of all defined scopes.

---

### Scope & Policy Evaluators

#### [`ScopeEvaluator`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/ScopeEvaluator.java)
- `boolean allows(Set<Scope> granted, Set<Scope> required)`: Verifies if granted scopes contain all required scopes.
- `Set<Scope> expand(Set<Scope> scopes)`: Expands a set of scopes with all transitive prerequisite dependencies.

#### [`ToolAccessPolicy`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/ToolAccessPolicy.java)
- `boolean isAllowed(Principal principal, ToolId toolId, Set<Scope> requiredScopes)`

#### [`AccountAccessPolicy`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/AccountAccessPolicy.java)
- `boolean canAccessAccount(Principal principal, AccountId accountId)`

---

### Destructive Action Guard

#### [`ConfirmationTokenEvaluator`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/ConfirmationTokenEvaluator.java)
- `String generateToken(PrincipalId principalId, ToolId toolId)`: Creates a cryptographically random, single-use, time-bound token (default 5-minute TTL).
- `boolean validateAndConsume(String token, PrincipalId principalId, ToolId toolId)`: Validates matching principal and tool, immediately invalidating the token upon consumption.
- `void purgeExpired()`: Cleans up expired tokens.

---

### Audit Trail & Masking

#### [`AuditSink`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/AuditSink.java)
- Implementations: `InMemoryAuditSink`, `LoggingAuditSink`, `CompositeAuditSink`.
- `void record(AuditEvent event)`: Emits audit record.

#### [`AuditEvent`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/AuditEvent.java)
- **Components**: `Instant timestamp`, `PrincipalId principalId`, `AccountId accountId`, `ToolId toolId`, `InvocationId invocationId`, `boolean success`, `Map<String, Object> details`, `String riskLevel`.

#### [`SecretMasker`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-security/src/main/java/org/mcp/nextcloud/security/SecretMasker.java)
- `String mask(String value)`: Replaces tokens with masked forms.
- `String maskKnownSecrets(String text, Collection<String> secrets)`
- `String maskText(String text)`

---

## Complete SDK Usage Snippets

### 1. Scope Evaluation and Authorization Check
```java
import java.util.Set;
import id.io.github.uriakleahcim.nextcloud.core.AccountId;
import id.io.github.uriakleahcim.nextcloud.core.PrincipalId;
import io.github.uriakleahcim.nextcloud.security.Principal;
import io.github.uriakleahcim.nextcloud.security.Scope;
import io.github.uriakleahcim.nextcloud.security.ScopeEvaluator;
import io.github.uriakleahcim.nextcloud.security.Scopes;

ScopeEvaluator evaluator = new ScopeEvaluator();

// Grant file write permissions and expand dependencies
Set<Scope> granted = evaluator.expand(Set.of(Scopes.Files.WRITE));

Principal principal = new Principal(
        new PrincipalId("app-agent-01"),
        granted,
        Set.of(new AccountId("temporary")),
        false
);

// Check if principal can read files (prerequisite of write)
boolean canRead = evaluator.allows(principal.scopes(), Set.of(Scopes.Files.READ));
System.out.println("Can read files: " + canRead); // true
```

### 2. Guarding Destructive Operations with Confirmation Tokens
```java
import java.time.Duration;
import id.io.github.uriakleahcim.nextcloud.core.PrincipalId;
import id.io.github.uriakleahcim.nextcloud.core.ToolId;
import io.github.uriakleahcim.nextcloud.security.ConfirmationTokenEvaluator;

ConfirmationTokenEvaluator tokenGuard = new ConfirmationTokenEvaluator(Duration.ofMinutes(2));
PrincipalId principalId = new PrincipalId("user-123");
ToolId purgeTool = new ToolId("nextcloud_trash_empty");

// 1. Initial invocation without token returns confirmation requirement
String token = tokenGuard.generateToken(principalId, purgeTool);
System.out.println("Confirmation token for user prompt: " + token);

// 2. Subsequent confirmation invocation
boolean valid = tokenGuard.validateAndConsume(token, principalId, purgeTool);
if (valid) {
    System.out.println("Confirmation valid! Executing destructive trash purge...");
} else {
    System.err.println("Invalid or expired confirmation token!");
}
```
