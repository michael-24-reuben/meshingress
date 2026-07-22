

# ToolPatchParams

Patch object for roles/tools/update.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**title** | **String** | Replacement human-readable title. |  [optional] |
|**description** | **String** | Replacement human-readable description. |  [optional] |
|**enabled** | **Boolean** | Replacement enabled flag. |  [optional] |
|**visibility** | [**VisibilityEnum**](#VisibilityEnum) | Replacement registry visibility. |  [optional] |
|**handlerKey** | **String** | Replacement handler key. |  [optional] |
|**inputSchema** | **Map&lt;String, Object&gt;** | Replacement JSON Schema object describing accepted input arguments. |  [optional] |
|**outputSchema** | **Map&lt;String, Object&gt;** | Replacement JSON Schema object describing structured output. |  [optional] |
|**annotations** | **Map&lt;String, Object&gt;** | Replacement MCP annotations object. |  [optional] |



## Enum: VisibilityEnum

| Name | Value |
|---- | -----|
| PUBLIC | &quot;public&quot; |
| PRIVATE | &quot;private&quot; |
| ADMIN | &quot;admin&quot; |



