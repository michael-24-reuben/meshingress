# ArtifactControllerApi

All URIs are relative to *http://100.121.15.11:4737*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**approve**](ArtifactControllerApi.md#approve) | **POST** /artifact/{groupId}/{artifactId}/{version}/approve |  |
| [**assess**](ArtifactControllerApi.md#assess) | **POST** /artifact/{groupId}/{artifactId}/{version}/assess |  |
| [**assessment**](ArtifactControllerApi.md#assessment) | **GET** /artifact/{groupId}/{artifactId}/{version}/assessment |  |
| [**callFile**](ArtifactControllerApi.md#callFile) | **GET** /artifact/{groupId}/{artifactId}/{version}/file |  |
| [**delete1**](ArtifactControllerApi.md#delete1) | **POST** /artifact/{groupId}/{artifactId}/{version}/delete |  |
| [**metadata**](ArtifactControllerApi.md#metadata) | **GET** /artifact/{groupId}/{artifactId}/{version}/metadata |  |
| [**pendingReviewQueue**](ArtifactControllerApi.md#pendingReviewQueue) | **GET** /artifact/reviews/pending |  |
| [**publication**](ArtifactControllerApi.md#publication) | **GET** /artifact/{groupId}/{artifactId}/{version}/publication |  |
| [**publish**](ArtifactControllerApi.md#publish) | **POST** /artifact/{groupId}/{artifactId}/{version}/publish |  |
| [**reject**](ArtifactControllerApi.md#reject) | **POST** /artifact/{groupId}/{artifactId}/{version}/reject |  |
| [**resource**](ArtifactControllerApi.md#resource) | **GET** /artifact/{groupId}/{artifactId}/{version}/resources/{resourceName} |  |
| [**restore**](ArtifactControllerApi.md#restore) | **POST** /artifact/{groupId}/{artifactId}/{version}/restore |  |
| [**revoke**](ArtifactControllerApi.md#revoke) | **POST** /artifact/{groupId}/{artifactId}/{version}/revoke |  |
| [**upload**](ArtifactControllerApi.md#upload) | **POST** /artifact/{groupId}/{artifactId}/{version} |  |
| [**uploadedJarMetadata**](ArtifactControllerApi.md#uploadedJarMetadata) | **GET** /artifact/jars |  |


<a id="approve"></a>
# **approve**
> ArtifactRecord approve(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    ArtifactReviewRequest artifactReviewRequest = new ArtifactReviewRequest(); // ArtifactReviewRequest | 
    try {
      ArtifactRecord result = apiInstance.approve(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#approve");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |
| **artifactReviewRequest** | [**ArtifactReviewRequest**](ArtifactReviewRequest.md)|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="assess"></a>
# **assess**
> ArtifactRecord assess(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      ArtifactRecord result = apiInstance.assess(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#assess");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="assessment"></a>
# **assessment**
> List&lt;ScannerResult&gt; assessment(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      List<ScannerResult> result = apiInstance.assessment(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#assessment");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**List&lt;ScannerResult&gt;**](ScannerResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="callFile"></a>
# **callFile**
> File callFile(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      File result = apiInstance.callFile(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#callFile");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**File**](File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/octet-stream

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="delete1"></a>
# **delete1**
> ArtifactRecord delete1(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    ArtifactReviewRequest artifactReviewRequest = new ArtifactReviewRequest(); // ArtifactReviewRequest | 
    try {
      ArtifactRecord result = apiInstance.delete1(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#delete1");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |
| **artifactReviewRequest** | [**ArtifactReviewRequest**](ArtifactReviewRequest.md)|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="metadata"></a>
# **metadata**
> ArtifactRecord metadata(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      ArtifactRecord result = apiInstance.metadata(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#metadata");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="pendingReviewQueue"></a>
# **pendingReviewQueue**
> List&lt;ArtifactReviewQueueItem&gt; pendingReviewQueue(authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      List<ArtifactReviewQueueItem> result = apiInstance.pendingReviewQueue(authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#pendingReviewQueue");
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
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**List&lt;ArtifactReviewQueueItem&gt;**](ArtifactReviewQueueItem.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="publication"></a>
# **publication**
> ArtifactPublicationRecord publication(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      ArtifactPublicationRecord result = apiInstance.publication(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#publication");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**ArtifactPublicationRecord**](ArtifactPublicationRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="publish"></a>
# **publish**
> ArtifactPublicationRecord publish(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      ArtifactPublicationRecord result = apiInstance.publish(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#publish");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**ArtifactPublicationRecord**](ArtifactPublicationRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="reject"></a>
# **reject**
> ArtifactRecord reject(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    ArtifactReviewRequest artifactReviewRequest = new ArtifactReviewRequest(); // ArtifactReviewRequest | 
    try {
      ArtifactRecord result = apiInstance.reject(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#reject");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |
| **artifactReviewRequest** | [**ArtifactReviewRequest**](ArtifactReviewRequest.md)|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="resource"></a>
# **resource**
> File resource(groupId, artifactId, version, resourceName, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      File result = apiInstance.resource(groupId, artifactId, version, resourceName, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#resource");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **resourceName** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**File**](File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="restore"></a>
# **restore**
> ArtifactRecord restore(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    ArtifactReviewRequest artifactReviewRequest = new ArtifactReviewRequest(); // ArtifactReviewRequest | 
    try {
      ArtifactRecord result = apiInstance.restore(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#restore");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |
| **artifactReviewRequest** | [**ArtifactReviewRequest**](ArtifactReviewRequest.md)|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="revoke"></a>
# **revoke**
> ArtifactPublicationRecord revoke(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    ArtifactReviewRequest artifactReviewRequest = new ArtifactReviewRequest(); // ArtifactReviewRequest | 
    try {
      ArtifactPublicationRecord result = apiInstance.revoke(groupId, artifactId, version, authorization, xRepositoryRole, xRepositoryActor, xRequestId, artifactReviewRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#revoke");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |
| **artifactReviewRequest** | [**ArtifactReviewRequest**](ArtifactReviewRequest.md)|  | [optional] |

### Return type

[**ArtifactPublicationRecord**](ArtifactPublicationRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="upload"></a>
# **upload**
> ArtifactRecord upload(groupId, artifactId, version, _file, type, packaging, requestedScopes, authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String groupId = "groupId_example"; // String | 
    String artifactId = "artifactId_example"; // String | 
    String version = "version_example"; // String | 
    File _file = new File("/path/to/file"); // File | 
    String type = "TOOL_MODULE"; // String | 
    String packaging = "jar"; // String | 
    List<String> requestedScopes = Arrays.asList(); // List<String> | 
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      ArtifactRecord result = apiInstance.upload(groupId, artifactId, version, _file, type, packaging, requestedScopes, authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#upload");
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
| **groupId** | **String**|  | |
| **artifactId** | **String**|  | |
| **version** | **String**|  | |
| **_file** | **File**|  | |
| **type** | **String**|  | [optional] [default to TOOL_MODULE] [enum: TOOL_MODULE, CLI_HARNESS, AVAILABILITY_ANNOTATION, AVAILABILITY_POLICY, SCOPE_POLICY] |
| **packaging** | **String**|  | [optional] [default to jar] |
| **requestedScopes** | [**List&lt;String&gt;**](String.md)|  | [optional] |
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**ArtifactRecord**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="uploadedJarMetadata"></a>
# **uploadedJarMetadata**
> List&lt;ArtifactRecord&gt; uploadedJarMetadata(authorization, xRepositoryRole, xRepositoryActor, xRequestId)



### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ArtifactControllerApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://100.121.15.11:4737");

    ArtifactControllerApi apiInstance = new ArtifactControllerApi(defaultClient);
    String authorization = "authorization_example"; // String | 
    String xRepositoryRole = "xRepositoryRole_example"; // String | 
    String xRepositoryActor = "xRepositoryActor_example"; // String | 
    String xRequestId = "xRequestId_example"; // String | 
    try {
      List<ArtifactRecord> result = apiInstance.uploadedJarMetadata(authorization, xRepositoryRole, xRepositoryActor, xRequestId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ArtifactControllerApi#uploadedJarMetadata");
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
| **authorization** | **String**|  | [optional] |
| **xRepositoryRole** | **String**|  | [optional] |
| **xRepositoryActor** | **String**|  | [optional] |
| **xRequestId** | **String**|  | [optional] |

### Return type

[**List&lt;ArtifactRecord&gt;**](ArtifactRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

