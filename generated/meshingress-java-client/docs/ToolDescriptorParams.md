

# ToolDescriptorParams

Dynamic MCP tool descriptor accepted by role-gated registry methods.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Public MCP tool name. |  |
|**title** | **String** | Human-readable tool title. |  [optional] |
|**description** | **String** | Human-readable tool description. |  |
|**version** | **Integer** | Descriptor version. Defaults to 1 when omitted. |  [optional] |
|**enabled** | **Boolean** | Whether the tool is enabled. Defaults to true when omitted. |  [optional] |
|**visibility** | [**VisibilityEnum**](#VisibilityEnum) | Registry visibility for the tool. |  [optional] |
|**handlerKey** | **String** | Handler key used by the runtime registry to route calls. |  |
|**inputSchema** | **Map&lt;String, Object&gt;** | JSON Schema object describing accepted input arguments. |  [optional] |
|**outputSchema** | **Map&lt;String, Object&gt;** | Optional JSON Schema object describing structured output. |  [optional] |
|**annotations** | **Map&lt;String, Object&gt;** | Optional MCP annotations object such as readOnlyHint or destructiveHint. |  [optional] |
|**functions** | [**List&lt;ToolFunctionParams&gt;**](ToolFunctionParams.md) | Optional function-level descriptors. If omitted, the tool descriptor is used as a single function. |  [optional] |



## Enum: VisibilityEnum

| Name | Value |
|---- | -----|
| PUBLIC | &quot;public&quot; |
| PRIVATE | &quot;private&quot; |
| ADMIN | &quot;admin&quot; |



