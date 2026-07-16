

# ToolRegistrationParams

Phase-aware parameters for roles/tools/register.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**phase** | [**PhaseEnum**](#PhaseEnum) | Registration lifecycle phase. Phase registration requires this field. |  |
|**toolId** | **String** | Stable registry tool id. |  [optional] |
|**name** | **String** | Human-readable registration name. |  [optional] |
|**tool** | [**ToolRegistrationToolParams**](ToolRegistrationToolParams.md) | Existing tool reference for lifecycle operations. |  [optional] |
|**localJar** | [**ToolRegistrationLocalJarParams**](ToolRegistrationLocalJarParams.md) | Local JAR source details. |  [optional] |
|**maven** | [**ToolRegistrationMavenParams**](ToolRegistrationMavenParams.md) | Maven coordinate source details. |  [optional] |
|**bundle** | [**ToolRegistrationBundleParams**](ToolRegistrationBundleParams.md) | Bundle source details. |  [optional] |
|**nativeTool** | [**ToolRegistrationNativeParams**](ToolRegistrationNativeParams.md) | Native built-in source details. |  [optional] |
|**replace** | **Boolean** | Replace an existing dynamic registration when true. |  [optional] |



## Enum: PhaseEnum

| Name | Value |
|---- | -----|
| CHECK | &quot;check&quot; |
| INSTALL | &quot;install&quot; |
| ACTIVATE | &quot;activate&quot; |
| DEACTIVATE | &quot;deactivate&quot; |
| DELETE | &quot;delete&quot; |



