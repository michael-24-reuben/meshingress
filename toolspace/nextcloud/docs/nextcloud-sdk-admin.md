# Nextcloud MCP Admin SDK Manual

Module: `nextcloud-sdk-admin`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-admin:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.admin`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Admin Composite Client (`NextcloudAdminClient`)](#admin-composite-client)
   - [Admin Credentials Model (`NextcloudAdminCredentials`)](#admin-credentials-model)
   - [User Provisioning Client (`NextcloudAdminUsersClient`)](#user-provisioning-client)
   - [Group Provisioning Client (`NextcloudAdminGroupsClient`)](#group-provisioning-client)
   - [App Lifecycle Client (`NextcloudAdminAppsClient`)](#app-lifecycle-client)
   - [OCC Command Bridge (`NextcloudAdminOccBridge`)](#occ-command-bridge)
   - [Admin Models & Enums](#admin-models--enums)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-admin` module provides administrative SDK operations for Nextcloud instances. It leverages the Nextcloud Provisioning API to manage users, quotas, group memberships, sub-admin privileges, apps (enable/disable), and generates deterministic `occ` command plans for server-level operations.

> [!IMPORTANT]
> Admin operations are high-privilege and should only be executed using accounts with verified administrative status.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.admin {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires transitive io.github.uriakleahcim.nextcloud.config;
    requires transitive io.github.uriakleahcim.nextcloud.http;
    requires transitive io.github.uriakleahcim.nextcloud.client;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.github.uriakleahcim.nextcloud.admin;

    opens io.github.uriakleahcim.nextcloud.admin to com.fasterxml.jackson.databind;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-admin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Admin Composite Client

#### [`NextcloudAdminClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminClient.java)
- **Constructor**:
  - `new NextcloudAdminClient(HttpClientAdapter httpClient, NextcloudAdminCredentials credentials)`
- **Accessors**:
  - `AdminAuthClient auth()`: Validates admin identity and capabilities.
  - `NextcloudAdminUsersClient users()`: Manages users and quotas.
  - `NextcloudAdminGroupsClient groups()`: Manages groups and memberships.
  - `NextcloudAdminAppsClient apps()`: Controls app lifecycle.
  - `NextcloudAdminSharesSupport shares()`: Access to admin sharing support.

---

### Admin Credentials Model

#### [`NextcloudAdminCredentials`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminCredentials.java)
- **Components**: `String accountId()`, `URI baseUri()`, `String username()`, `String appPassword()`.
- **Factory Methods**:
  - `static NextcloudAdminCredentials of(String accountId, String baseUri, String username, String appPassword)`
  - `static NextcloudAdminCredentials from(NextcloudMcpConfig config)`

---

### User Provisioning Client

#### [`NextcloudAdminUsersClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminUsersClient.java)
- `List<String> listUsers(String search, Integer limit, Integer offset)`: Lists or filters user IDs.
- `AdminUser getUser(String userId)`: Fetches user metadata, quota, and status.
- `AdminUserCreated createUser(AdminUserCreateRequest request)`: Provisions a new Nextcloud user.
- `AdminProvisioningOperation updateUserField(String userId, String key, String value)`: Updates quota, display name, email, or password.
- `AdminProvisioningOperation disableUser(String userId)`: Suspends user account.
- `AdminProvisioningOperation enableUser(String userId)`: Re-activates user account.
- `AdminProvisioningOperation deleteUser(String userId)`: Permanently deletes user account.
- `List<String> getUserGroups(String userId)`: Gets user's joined groups.
- `List<String> getSubadminGroups(String userId)`: Gets groups where user is a sub-admin.
- `AdminProvisioningOperation resendWelcomeEmail(String userId)`: Resends onboarding email.
- `List<String> editableFields()`: Returns list of user fields that can be mutated.

---

### Group Provisioning Client

#### [`NextcloudAdminGroupsClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminGroupsClient.java)
- `List<String> listGroups(String search, Integer limit, Integer offset)`: Lists group IDs.
- `AdminGroup createGroup(String groupId)`: Creates a new user group.
- `List<String> getGroupMembers(String groupId)`: Lists members of a group.
- `List<String> getGroupSubadmins(String groupId)`: Lists subadmins of a group.
- `AdminProvisioningOperation updateGroupDisplayName(String groupId, String displayName)`: Updates group display name.
- `AdminProvisioningOperation deleteGroup(String groupId)`: Deletes group.
- `AdminProvisioningOperation addUserToGroup(String userId, String groupId)`: Adds user to group.
- `AdminProvisioningOperation removeUserFromGroup(String userId, String groupId)`: Removes user from group.
- `AdminProvisioningOperation promoteSubadmin(String userId, String groupId)`: Promotes user to group subadmin.
- `AdminProvisioningOperation demoteSubadmin(String userId, String groupId)`: Demotes user from group subadmin.

---

### App Lifecycle Client

#### [`NextcloudAdminAppsClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminAppsClient.java)
- `List<String> listApps()` / `listEnabledApps()` / `listDisabledApps()` / `listApps(String filter)`: Lists app IDs.
- `AdminApp getAppInfo(String appId)`: Retrieves app name, version, and enabled status.
- `AdminAppOperation enableApp(String appId)`: Enables an installed app.
- `AdminAppOperation disableApp(String appId)`: Disables an installed app.

---

### OCC Command Bridge

#### [`NextcloudAdminOccBridge`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/NextcloudAdminOccBridge.java)
- `new NextcloudAdminOccBridge()` / `new NextcloudAdminOccBridge(String containerName)`
- `AdminOccCommandPlan maintenanceMode(boolean enabled)`: Generates `php occ maintenance:mode --on|--off` plan.
- `AdminOccCommandPlan filesScan(String userId)`: Generates `php occ files:scan <userId>` plan.
- `AdminOccCommandPlan configGet(String app, String key)`: Generates `php occ config:app:get <app> <key>` plan.
- `AdminOccCommandPlan configSet(String app, String key, String value)`: Generates `php occ config:app:set` plan.
- `AdminOccCommandPlan backgroundJobList()`: Generates `php occ background-job:list` plan.
- `AdminOccCommandPlan recoverAdminGroup(String userId)`: Generates `php occ group:adduser admin <userId>` plan.

---

### Admin Models & Enums

- [`AdminUser`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/AdminUser.java): `(String id, String displayName, String email, boolean enabled, String quota, JsonNode raw)`
- [`AdminGroup`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/AdminGroup.java): `(String id, String displayName, JsonNode raw)`
- [`AdminApp`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/AdminApp.java): `(String id, String name, String version, Boolean enabled, JsonNode raw)`
- [`AdminRiskLevel`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/AdminRiskLevel.java): `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- [`AdminUserCreateRequest`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-admin/src/main/java/org/mcp/nextcloud/admin/AdminUserCreateRequest.java): `(String userId, String password, String displayName, String email, List<String> groups, String quota, String language)`

---

## Complete SDK Usage Snippets

### 1. Provisioning a User and Managing Group Membership

```java
import java.util.List;

import io.github.uriakleahcim.nextcloud.admin.AdminUser;
import io.github.uriakleahcim.nextcloud.admin.AdminUserCreateRequest;
import io.github.uriakleahcim.nextcloud.admin.AdminUserCreated;
import io.github.uriakleahcim.nextcloud.admin.NextcloudAdminClient;
import io.github.uriakleahcim.nextcloud.admin.NextcloudAdminCredentials;
import io.github.uriakleahcim.nextcloud.http.JdkHttpClientAdapter;

NextcloudAdminCredentials credentials = NextcloudAdminCredentials.of(
        "primary-admin",
        "https://cloud.example.com",
        "admin",
        "admin-app-password"
);

NextcloudAdminClient adminClient = new NextcloudAdminClient(new JdkHttpClientAdapter(), credentials);

// 1. Create a group
adminClient.

groups().

createGroup("engineering");

// 2. Create user with quota and initial group
AdminUserCreateRequest createReq = new AdminUserCreateRequest(
        "jdoe",
        "InitialTempPass123!",
        "Jane Doe",
        "jdoe@example.com",
        List.of("engineering"),
        "10 GB",
        "en"
);
AdminUserCreated created = adminClient.users().createUser(createReq);
System.out.

println("Created user: "+created.userId());

// 3. Promote user to sub-admin for the group
        adminClient.

groups().

promoteSubadmin("jdoe","engineering");

// 4. Retrieve user details
AdminUser user = adminClient.users().getUser("jdoe");
System.out.

println("User quota: "+user.quota() +", enabled: "+user.

enabled());
```

### 2. Planning an OCC File Scan

```java
import io.github.uriakleahcim.nextcloud.admin.AdminOccCommandPlan;
import io.github.uriakleahcim.nextcloud.admin.NextcloudAdminOccBridge;

NextcloudAdminOccBridge occBridge = new NextcloudAdminOccBridge("nextcloud-aio-nextcloud");
AdminOccCommandPlan scanPlan = occBridge.filesScan("jdoe");

System.out.

println("Operation: "+scanPlan.operation());
        System.out.

println("Risk Level: "+scanPlan.riskLevel());
        System.out.

println("Docker Command: "+String.join(" ", scanPlan.command()));
// Prints: docker exec -u www-data nextcloud-aio-nextcloud php occ files:scan jdoe
```
