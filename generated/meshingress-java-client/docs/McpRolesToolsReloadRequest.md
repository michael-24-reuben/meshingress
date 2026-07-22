

# McpRolesToolsReloadRequest

Admin request that reports current runtime reload support and registration state.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | **Object** |  |  [optional] |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| ROLES_TOOLS_RELOAD | &quot;roles/tools/reload&quot; |



