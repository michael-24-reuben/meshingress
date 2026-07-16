

# McpRolesToolsListRequest

Admin request that lists role-visible tools, audit events, and registrations.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**jsonrpc** | [**JsonrpcEnum**](#JsonrpcEnum) | JSON-RPC protocol version. |  |
|**id** | **Object** |  |  |
|**method** | [**MethodEnum**](#MethodEnum) | MCP method name. |  |
|**params** | [**RolesToolListParams**](RolesToolListParams.md) |  |  |



## Enum: JsonrpcEnum

| Name | Value |
|---- | -----|
| _2_0 | &quot;2.0&quot; |



## Enum: MethodEnum

| Name | Value |
|---- | -----|
| ROLES_TOOLS_LIST | &quot;roles/tools/list&quot; |



