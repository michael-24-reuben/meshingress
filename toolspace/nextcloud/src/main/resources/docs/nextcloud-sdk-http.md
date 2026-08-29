# Nextcloud MCP HTTP SDK Manual

Module: `nextcloud-sdk-http`  
Artifact: `io.github.uriakleahcim.nextcloud:nextcloud-sdk-http:0.0.1-SNAPSHOT`  
JPMS Module: `io.github.uriakleahcim.nextcloud.http`

---

## Table of Contents

1. [Overview](#overview)
2. [Module Architecture & Encapsulation](#module-architecture--encapsulation)
3. [Maven Dependency Declaration](#maven-dependency-declaration)
4. [API Reference & Callable Blocks](#api-reference--callable-blocks)
   - [HTTP Abstractions (`HttpClientAdapter`, `HttpRequestSpec`, `HttpResponseSpec`)](#http-abstractions)
   - [HTTP Methods & Headers (`HttpMethod`, `HttpHeadersBuilder`)](#http-methods--headers)
   - [Nextcloud HTTP Request Factory (`NextcloudHttpRequestFactory`)](#nextcloud-http-request-factory)
   - [Authentication Helpers (`BasicAuth`, `BearerAuth`)](#authentication-helpers)
   - [Resilience & Flow Control (`RetryPolicy`, `RateLimitPolicy`)](#resilience--flow-control)
5. [Complete SDK Usage Snippets](#complete-sdk-usage-snippets)

---

## Overview

The `nextcloud-sdk-http` module provides an extensible HTTP client abstraction, concrete JDK 25 `HttpClient` adapter, request/response models, OCS and WebDAV URL builders with per-segment encoding, retry handling, and rate limiting.

---

## Module Architecture & Encapsulation

```java
module io.github.uriakleahcim.nextcloud.http {
    requires transitive io.github.uriakleahcim.nextcloud.core;
    requires java.net.http;
    requires org.slf4j;

    exports io.github.uriakleahcim.nextcloud.http;
}
```

---

## Maven Dependency Declaration

```xml
<dependency>
    <groupId>io.github.uriakleahcim.nextcloud</groupId>
    <artifactId>nextcloud-sdk-http</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## API Reference & Callable Blocks

### HTTP Abstractions

#### [`HttpClientAdapter`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/HttpClientAdapter.java)
- **Description**: Contract for executing HTTP requests against remote Nextcloud endpoints.
- **Callable**:
  - `HttpResponseSpec send(HttpRequestSpec request) throws IOException, InterruptedException`: Synchronously executes request and returns normalized response.

#### [`JdkHttpClientAdapter`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/JdkHttpClientAdapter.java)
- **Description**: Production `HttpClientAdapter` implementation backed by `java.net.http.HttpClient`.
- **Callable**:
  - `new JdkHttpClientAdapter()`: Initializes default JDK HTTP client with follow redirects (NORMAL) and 30s connection timeout.
  - `new JdkHttpClientAdapter(HttpClient client)`: Wraps an existing configured `HttpClient`.
  - `new JdkHttpClientAdapter(HttpClient client, Duration requestTimeout)`: Configures default per-request timeout.

#### [`HttpRequestSpec`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/HttpRequestSpec.java)
- **Components**:
  - `HttpMethod method()`: HTTP method.
  - `URI uri()`: Target absolute URI.
  - `Map<String, List<String>> headers()`: HTTP request headers.
  - `byte[] body()`: Request body bytes (optional, null when empty).
  - `Duration timeout()`: Custom request timeout override.
- **Callable**:
  - `new HttpRequestSpec(HttpMethod method, URI uri, Map<String, List<String>> headers, byte[] body)`
  - `new HttpRequestSpec(HttpMethod method, URI uri, Map<String, List<String>> headers, byte[] body, Duration timeout)`

#### [`HttpResponseSpec`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/HttpResponseSpec.java)
- **Components**:
  - `int statusCode()`: HTTP response status code (e.g., 200, 207, 404).
  - `Map<String, List<String>> headers()`: Normalized response headers.
  - `byte[] body()`: Raw response body bytes.
- **Callable**:
  - `boolean isSuccessful()`: Returns true if status code is between 200 and 299 inclusive.
  - `String bodyString()`: Decodes body bytes as UTF-8 string.
  - `Optional<String> firstHeader(String name)`: Case-insensitive header lookup.

---

### HTTP Methods & Headers

#### [`HttpMethod`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/HttpMethod.java)
- Values: `GET`, `POST`, `PUT`, `DELETE`, `PROPFIND`, `PROPPATCH`, `MKCOL`, `MOVE`, `COPY`, `REPORT`.

#### [`HttpHeadersBuilder`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/HttpHeadersBuilder.java)
- `HttpHeadersBuilder header(String name, String value)`: Adds header.
- `HttpHeadersBuilder ocsJsonDefaults()`: Injects `OCS-APIRequest: true` and `Accept: application/json`.
- `Map<String, List<String>> build()`: Builds immutable multi-map of headers.

---

### Nextcloud HTTP Request Factory

#### [`NextcloudHttpRequestFactory`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/NextcloudHttpRequestFactory.java)
- **Constructor**:
  - `new NextcloudHttpRequestFactory(URI baseUri, String username, String appPassword)`
- **OCS Endpoints**:
  - `HttpRequestSpec getOcs(String path)`
  - `HttpRequestSpec getOcs(String path, Map<String, String> query)`
  - `HttpRequestSpec postOcs(String path)`
  - `HttpRequestSpec postOcsForm(String path, Map<String, String> fields)`
  - `HttpRequestSpec putOcsForm(String path, Map<String, String> fields)`
  - `HttpRequestSpec deleteOcs(String path)`
  - `HttpRequestSpec deleteOcsForm(String path, Map<String, String> fields)`
- **WebDAV Endpoints**:
  - `HttpRequestSpec webDav(HttpMethod method, String userId, String userPath, boolean collection, Map<String, String> headers, byte[] body)`
  - `HttpRequestSpec webDavTrash(HttpMethod method, String userId, String trashPath, Map<String, String> headers, byte[] body)`
  - `HttpRequestSpec webDavVersions(HttpMethod method, String userId, String fileId, String versionId, Map<String, String> headers, byte[] body)`
  - `HttpRequestSpec webDavComments(HttpMethod method, String fileId, String commentId, Map<String, String> headers, byte[] body)`
  - `URI webDavDestinationUri(String userId, String userPath, boolean collection)`
  - `URI webDavTrashRestoreDestinationUri(String userId, String destinationPath)`
  - `URI webDavVersionsRestoreDestinationUri(String userId, String destinationPath)`
- **Path Encoders**:
  - `static String encodePathSegment(String value)`: Percent-encodes path segments safely with UTF-8.

---

### Authentication Helpers

#### [`BasicAuth`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/BasicAuth.java)
- `static String authorizationHeader(String username, String appPassword)`: Formats standard `Basic <base64>` header value.

#### [`BearerAuth`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/BearerAuth.java)
- `static String authorizationHeader(String token)`: Formats standard `Bearer <token>` header value.

---

### Resilience & Flow Control

#### [`RetryPolicy`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/RetryPolicy.java)
- `new RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff)`
- `<T> T execute(Callable<T> task) throws Exception`: Executes task with exponential backoff on transient errors.

#### [`RateLimitPolicy`](file:///C:/Users/jbeas/Repositories/Dev.java-2026/artifacts/nextcloud-sdk/lib/nextcloud-sdk-http/src/main/java/org/mcp/nextcloud/http/RateLimitPolicy.java)
- `new RateLimitPolicy(int permitsPerMinute)`
- `boolean tryAcquire()`: Returns true if limit has not been exceeded.
- `void acquire() throws InterruptedException`: Blocks until token is available.

---

## Complete SDK Usage Snippets

### 1. Direct OCS Call via Request Factory and JDK Adapter
```java
import java.net.URI;
import io.github.uriakleahcim.nextcloud.http.HttpClientAdapter;
import io.github.uriakleahcim.nextcloud.http.HttpRequestSpec;
import io.github.uriakleahcim.nextcloud.http.HttpResponseSpec;
import io.github.uriakleahcim.nextcloud.http.JdkHttpClientAdapter;
import io.github.uriakleahcim.nextcloud.http.NextcloudHttpRequestFactory;

HttpClientAdapter client = new JdkHttpClientAdapter();
NextcloudHttpRequestFactory factory = new NextcloudHttpRequestFactory(
        URI.create("https://cloud.example.com"),
        "temporary",
        "app-password-token"
);

HttpRequestSpec request = factory.getOcs("/ocs/v1.php/cloud/user");
HttpResponseSpec response = client.send(request);

if (response.isSuccessful()) {
    System.out.println("OCS Response: " + response.bodyString());
} else {
    System.err.println("HTTP " + response.statusCode() + ": " + response.bodyString());
}
```

### 2. Issuing a WebDAV PROPFIND with XML Payload
```java
import java.nio.charset.StandardCharsets;
import java.util.Map;
import io.github.uriakleahcim.nextcloud.http.HttpMethod;
import io.github.uriakleahcim.nextcloud.http.HttpRequestSpec;
import io.github.uriakleahcim.nextcloud.http.HttpResponseSpec;

String propfindXml = """
        <?xml version="1.0" encoding="utf-8" ?>
        <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
            <d:prop>
                <d:getlastmodified/>
                <d:getcontentlength/>
                <d:getcontenttype/>
                <d:resourcetype/>
                <oc:fileid/>
            </d:prop>
        </d:propfind>
        """;

HttpRequestSpec propfindReq = factory.webDav(
        HttpMethod.PROPFIND,
        "temporary",
        "Documents/notes.txt",
        false,
        Map.of("Depth", "0", "Content-Type", "application/xml"),
        propfindXml.getBytes(StandardCharsets.UTF_8)
);

HttpResponseSpec resp = client.send(propfindReq);
System.out.println("Status: " + resp.statusCode()); // 207 Multi-Status
```
