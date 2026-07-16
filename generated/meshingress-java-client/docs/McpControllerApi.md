# McpControllerApi

All URIs are relative to *http://100.121.15.11:4737*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**delete**](McpControllerApi.md#delete) | **DELETE** /mcp |  |
| [**get**](McpControllerApi.md#get) | **GET** /mcp |  |
| [**post**](McpControllerApi.md#post) | **POST** /mcp | Dispatch MCP JSON-RPC requests |


<a id="delete"></a>
# **delete**
> delete()



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.McpControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    McpControllerApi apiInstance = new McpControllerApi(defaultClient);
    try {
      apiInstance.delete();
    } catch (ApiException e) {
      System.err.println("Exception when calling McpControllerApi#delete");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="get"></a>
# **get**
> get()



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.McpControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    McpControllerApi apiInstance = new McpControllerApi(defaultClient);
    try {
      apiInstance.get();
    } catch (ApiException e) {
      System.err.println("Exception when calling McpControllerApi#get");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="post"></a>
# **post**
> JsonNode post(postRequest, authorization, xMcpRole, xMcpAdmin, mcpSessionId, xRequestId)

Dispatch MCP JSON-RPC requests

Accepts JSON-RPC 2.0 single requests, notifications, and batches for the Meshingress MCP transport.

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.McpControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    McpControllerApi apiInstance = new McpControllerApi(defaultClient);
    PostRequest postRequest = new PostRequest(); // PostRequest | JSON-RPC 2.0 payload. Use a single request object for ordinary calls, omit id for notifications, or send an array of request objects for batches.
    String authorization = "Bearer dev-admin"; // String | Bearer admin token for role-gated MCP methods.
    String xMcpRole = "admin"; // String | MCP role hint. Use admin for role-gated registry methods.
    String xMcpAdmin = "true"; // String | Legacy admin flag accepted for compatibility.
    String mcpSessionId = "session-123"; // String | Client MCP session identifier.
    String xRequestId = "req-123"; // String | Caller request correlation identifier.
    try {
      JsonNode result = apiInstance.post(postRequest, authorization, xMcpRole, xMcpAdmin, mcpSessionId, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpControllerApi#post");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **postRequest** | [**PostRequest**](PostRequest.md)| JSON-RPC 2.0 payload. Use a single request object for ordinary calls, omit id for notifications, or send an array of request objects for batches. | |
| **authorization** | **String**| Bearer admin token for role-gated MCP methods. | [optional] |
| **xMcpRole** | **String**| MCP role hint. Use admin for role-gated registry methods. | [optional] |
| **xMcpAdmin** | **String**| Legacy admin flag accepted for compatibility. | [optional] |
| **mcpSessionId** | **String**| Client MCP session identifier. | [optional] |
| **xRequestId** | **String**| Caller request correlation identifier. | [optional] |

### Return type

[**JsonNode**](JsonNode.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | JSON-RPC response object or response batch. JSON-RPC method failures are represented in the response body error object. |  -  |
| **204** | Notification-only request or batch produced no JSON-RPC response. |  -  |
| **415** | Unsupported media type. |  -  |

