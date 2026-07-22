

# ToolFunctionParams

Function descriptor nested inside a dynamic MCP tool descriptor.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Public MCP function name. |  |
|**title** | **String** | Human-readable function title. |  [optional] |
|**description** | **String** | Human-readable function description. |  [optional] |
|**version** | **Integer** | Function version. Defaults to the parent tool version or 1. |  [optional] |
|**enabled** | **Boolean** | Whether the function is enabled. Defaults to true when omitted. |  [optional] |
|**visibility** | [**VisibilityEnum**](#VisibilityEnum) | Function visibility. Defaults to the parent tool visibility when omitted. |  [optional] |
|**handlerKey** | **String** | Handler key used by the runtime registry to route calls. |  |
|**inputSchema** | **Map&lt;String, Object&gt;** | JSON Schema object describing accepted input arguments. |  [optional] |
|**outputSchema** | **Map&lt;String, Object&gt;** | Optional JSON Schema object describing structured output. |  [optional] |
|**annotations** | **Map&lt;String, Object&gt;** | Optional MCP annotations object such as readOnlyHint or destructiveHint. |  [optional] |



## Enum: VisibilityEnum

| Name | Value |
|---- | -----|
| PUBLIC | &quot;public&quot; |
| PRIVATE | &quot;private&quot; |
| ADMIN | &quot;admin&quot; |



