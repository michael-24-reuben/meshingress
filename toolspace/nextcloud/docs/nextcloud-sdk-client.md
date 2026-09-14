# Nextcloud MCP Client SDK Manual

Module: `nextcloud-sdk-client`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-client:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.client`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [Client Factory & Composite Root (`NextcloudClient`)](#client-factory--composite-root)
   - [Credentials Model (`NextcloudCredentials`)](#credentials-model)
   - [Files Client (`NextcloudFilesClient`)](#files-client)
   - [Shares & Sharees Clients (`NextcloudSharesClient`, `NextcloudShareesClient`)](#shares--sharees-clients)
   - [Trashbin Client (`NextcloudTrashClient`)](#trashbin-client)
   - [Versions Client (`NextcloudVersionsClient`)](#versions-client)
   - [Comments Client (`NextcloudCommentsClient`)](#comments-client)
   - [User & Status Clients (`NextcloudUsersClient`, `NextcloudUserStatusClient`)](#user--status-clients)
   - [Interactive Login Flow v2 (`NextcloudLoginFlowClient`)](#interactive-login-flow-v2)
   - [Domain Records & Data Models](#domain-records--data-models)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-client` module is the primary Java client for interacting with standard Nextcloud user APIs. It encapsulates WebDAV endpoints (file management, trashbin, file version history, collaborative comments) and OCS REST APIs (user capabilities, profile resolution, file sharing, and custom user statuses).

Internal XML/JSON parsing logic (`WebDavParser`, `OcsParser`) is package-private and encapsulated within the `io.github.uriakleahcim.nextcloud.client` module.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.client {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires transitive io.github.uriakleahcim.nextcloud.config;
    requires transitive io.github.uriakleahcim.nextcloud.http;
    requires java.xml;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.github.uriakleahcim.nextcloud.client;

    opens io.github.uriakleahcim.nextcloud.client to com.fasterxml.jackson.databind;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### Client Factory & Composite Root

#### [`NextcloudClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudClient.java)
- **Constructor**:
  - `new NextcloudClient(HttpClientAdapter httpClient, NextcloudCredentials credentials)`: Instantiates a thread-safe composite client managing sub-clients.
- **Sub-Client Accessors**:
  - `NextcloudUsersClient users()`
  - `NextcloudFilesClient files()`
  - `NextcloudSharesClient shares()`
  - `NextcloudShareesClient sharees()`
  - `NextcloudTrashClient trash()`
  - `NextcloudVersionsClient versions()`
  - `NextcloudCommentsClient comments()`
  - `NextcloudUserStatusClient status()`
- **Static Helpers**:
  - `static NextcloudLoginFlowClient loginFlow(HttpClientAdapter httpClient)`: Creates unauthenticated Login Flow v2 client.

---

### Credentials Model

#### [`NextcloudCredentials`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudCredentials.java)
- **Components**: `URI baseUri()`, `String username()`, `String appPassword()`.
- **Factory Methods**:
  - `static NextcloudCredentials of(String baseUri, String username, String appPassword)`
  - `static NextcloudCredentials from(NextcloudAccountConfig accountConfig)`
  - `static NextcloudCredentials from(LocalUserAccountRecord record)`

---

### Files Client

#### [`NextcloudFilesClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudFilesClient.java)
- `List<WebDavResource> list(String userId, String path)` / `list(NextcloudUser user, String path)`: PROPFIND Depth: 1.
- `WebDavResource stat(String userId, String path)` / `stat(NextcloudUser user, String path)`: PROPFIND Depth: 0.
- `WebDavOperation mkdir(String userId, String path)` / `mkdir(NextcloudUser user, String path)`: MKCOL collection directory.
- `WebDavOperation upload(String userId, String path, byte[] body)` / `upload(NextcloudUser user, String path, byte[] body)`: PUT octet-stream.
- `byte[] download(String userId, String path)` / `download(NextcloudUser user, String path)`: GET file bytes.
- `WebDavOperation delete(String userId, String path)` / `delete(NextcloudUser user, String path)`: DELETE file or collection.
- `WebDavOperation move(String userId, String fromPath, String toPath)`: MOVE with `Destination` header.
- `WebDavOperation copy(String userId, String fromPath, String toPath)`: COPY with `Destination` header.
- `WebDavOperation favorite(String userId, String path, boolean favorite)`: PROPPATCH `oc:favorite`.
- `List<WebDavResource> search(String userId, String query)`: WebDAV SEARCH query with `DAV:like` condition.

---

### Shares & Sharees Clients

#### [`NextcloudSharesClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudSharesClient.java)
- `List<ShareInfo> listShares()` / `listShares(String path)`: Lists user shares.
- `ShareInfo getShare(String shareId)`: Retrieves share metadata.
- `ShareInfo createShare(ShareCreateRequest request)`: Creates user, group, public link, or federated share.
- `ShareInfo updateShare(String shareId, ShareUpdateRequest request)`: Updates share expiration, password, or permissions.
- `void deleteShare(String shareId)`: Revokes share.
- `void sendShareEmail(String shareId)`: Triggers share notification email.

#### [`NextcloudShareesClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudShareesClient.java)
- `List<Sharee> search(String query, String itemType, int page, int perPage)`: Autocompletes users/groups/circles for sharing.

---

### Trashbin Client

#### [`NextcloudTrashClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudTrashClient.java)
- `List<TrashItem> list(String userId)` / `list(NextcloudUser user)`: Lists deleted items in trashbin.
- `WebDavOperation restore(String userId, String trashId, String destinationPath)`: Moves item from trashbin back into active files.
- `WebDavOperation delete(String userId, String trashId)`: Permanently purges specific trash item.
- `WebDavOperation empty(String userId)`: Empties entire user trashbin.

---

### Versions Client

#### [`NextcloudVersionsClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudVersionsClient.java)
- `List<FileVersion> list(String userId, String fileId)`: Lists previous historical revisions for a file.
- `WebDavOperation restore(String userId, String fileId, String versionId, String destinationPath)`: Restores previous revision.
- `byte[] download(String userId, String fileId, String versionId)`: Downloads raw content of a specific revision.

---

### Comments Client

#### [`NextcloudCommentsClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudCommentsClient.java)
- `List<CommentInfo> list(String fileId)`: Lists comments attached to a file ID.
- `CommentInfo create(String fileId, String message)` / `create(String fileId, String actorId, String message)`: Posts comment.
- `WebDavOperation update(String fileId, String commentId, String message)`: Edits existing comment text.
- `WebDavOperation delete(String fileId, String commentId)`: Deletes comment.
- `WebDavOperation markRead(String fileId, String dateTime)`: Updates user comment read marker.

---

### User & Status Clients

#### [`NextcloudUsersClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudUsersClient.java)
- `NextcloudUser currentUser()`: Resolves authenticated user ID, display name, and email via `GET /ocs/v1.php/cloud/user`.
- `NextcloudCapabilities capabilities()`: Resolves server version and enabled app capabilities.

#### [`NextcloudUserStatusClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudUserStatusClient.java)
- `UserStatusInfo get()` / `get(String userId)`: Gets current user presence status.
- `UserStatusInfo setStatus(String statusType)`: Sets status type (`online`, `busy`, `away`, `dnd`, `offline`).
- `UserStatusInfo setCustomMessage(String message, String statusIcon, Long clearAt)`: Sets custom message and emoji.
- `UserStatusInfo setPredefinedMessage(String messageId, Long clearAt)`: Sets predefined status template.
- `boolean clearMessage()`: Clears status message.
- `List<PredefinedStatus> listPredefined()`: Lists server predefined status templates.

---

### Interactive Login Flow v2

#### [`NextcloudLoginFlowClient`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudLoginFlowClient.java)
- `LoginFlowInitiation initiate(URI baseUri)`: Initiates Login Flow v2 session returning browser login URL and poll token.
- `Optional<LoginFlowResult> poll(LoginFlowInitiation initiation)`: Polls server until user authorizes in browser, yielding created `appPassword`.

---

### Domain Records & Data Models

- [`NextcloudUser`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/NextcloudUser.java): `(String id, String displayName, String email, boolean enabled, JsonNode raw)`
- [`WebDavResource`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/WebDavResource.java): `(String href, String path, String name, boolean collection, long contentLength, String contentType, String etag, String lastModified, String fileId, ...)`
- [`ShareInfo`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/ShareInfo.java): `(String id, String path, Integer shareType, String shareWith, String shareWithDisplayName, String url, Integer permissions, ...)`
- [`TrashItem`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/TrashItem.java): `(String href, String trashId, String originalLocation, String deletionTime, boolean collection, ...)`
- [`FileVersion`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/FileVersion.java): `(String href, String versionId, String lastModified, String etag, long contentLength, ...)`
- [`CommentInfo`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/CommentInfo.java): `(String id, String parentId, String message, String actorType, String actorId, String actorDisplayName, ...)`
- [`UserStatusInfo`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-client/src/main/java/org/mcp/nextcloud/client/UserStatusInfo.java): `(String userId, String status, String statusType, boolean statusIsUserDefined, String message, ...)`

---

## Complete SDK Usage Snippets

### 1. Authenticating and Browsing Files

```java
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.uriakleahcim.nextcloud.client.NextcloudClient;
import io.github.uriakleahcim.nextcloud.client.NextcloudCredentials;
import io.github.uriakleahcim.nextcloud.client.NextcloudUser;
import io.github.uriakleahcim.nextcloud.client.WebDavResource;
import io.github.uriakleahcim.nextcloud.http.JdkHttpClientAdapter;

NextcloudCredentials credentials = NextcloudCredentials.of(
        "https://alphasunny11-07.tail93ea23.ts.net",
        "temporary",
        "app-password-token"
);

NextcloudClient client = new NextcloudClient(new JdkHttpClientAdapter(), credentials);

// 1. Resolve current user
NextcloudUser user = client.users().currentUser();
System.out.

println("Authenticated as "+user.displayName() +" (ID: "+user.

id() +")");

// 2. Create directory and upload file
        client.

files().

mkdir(user, "ProjectReports");
client.

files().

upload(user, "ProjectReports/summary.txt","Q1 Summary Content".getBytes(StandardCharsets.UTF_8));

// 3. List directory contents
List<WebDavResource> files = client.files().list(user, "ProjectReports");
for(
WebDavResource res :files){
        System.out.

println("Resource: "+res.name() +" (bytes: "+res.

contentLength() +")");
        }
```

### 2. Managing Shares and Comments

```java
import io.github.uriakleahcim.nextcloud.client.CommentInfo;
import io.github.uriakleahcim.nextcloud.client.ShareCreateRequest;
import io.github.uriakleahcim.nextcloud.client.ShareInfo;
import io.github.uriakleahcim.nextcloud.client.ShareType;

// Create public link share with read permission
ShareCreateRequest shareReq = new ShareCreateRequest(
        "ProjectReports/summary.txt",
        ShareType.PUBLIC_LINK,
        null,
        1,      // Read permissions
        false,  // publicUpload
        "reportPass123",
        "2026-12-31",
        "Shared for review",
        "Client Report"
);
        ShareInfo share = client.shares().createShare(shareReq);
System.out.

        println("Share Link: "+share.url());

        // Post a comment on the file
        CommentInfo comment = client.comments().create("12345", "Please review before Monday!");
System.out.

        println("Comment ID "+comment.id() +": "+comment.

        message());
```
