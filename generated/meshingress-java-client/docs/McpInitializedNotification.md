

# McpInitializedNotification

Notification sent after the client accepts initialization. Notifications omit id and do not produce a response body.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | **Object** |  |  [optional] |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| NOTIFICATIONS_INITIALIZED | &quot;notifications/initialized&quot; |



