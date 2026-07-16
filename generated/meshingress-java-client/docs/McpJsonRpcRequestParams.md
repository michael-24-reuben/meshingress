

# McpJsonRpcRequestParams

Method-specific parameters. See the method-specific request schemas for concrete shapes.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**protocolVersion** | **String** | Protocol version requested by the client. |  [optional] |
|**capabilities** | **Object** |  |  [optional] |
|**clientInfo** | [**McpClientInfo**](McpClientInfo.md) | Client identity. |  [optional] |
|**name** | **String** | Name of the tool to disable. |  |
|**arguments** | **Map&lt;String, Object&gt;** | Tool-specific arguments object. Its schema is supplied by tools/list for each tool. |  [optional] |
|**tool** | [**ToolDescriptorParams**](ToolDescriptorParams.md) | Dynamic tool descriptor to register as an alias. |  |
|**mode** | [**ModeEnum**](#ModeEnum) | Deletion mode. The current server supports disable only. |  [optional] |
|**phase** | [**PhaseEnum**](#PhaseEnum) | Registration lifecycle phase. Phase registration requires this field. |  |
|**toolId** | **String** | Optional tool id override to use during installation. |  [optional] |
|**localJar** | [**ToolRegistrationLocalJarParams**](ToolRegistrationLocalJarParams.md) | Local JAR source details. |  [optional] |
|**maven** | [**ToolRegistrationMavenParams**](ToolRegistrationMavenParams.md) | Maven coordinate source details. |  [optional] |
|**bundle** | [**ToolRegistrationBundleParams**](ToolRegistrationBundleParams.md) | Bundle source details. |  [optional] |
|**nativeTool** | [**ToolRegistrationNativeParams**](ToolRegistrationNativeParams.md) | Native built-in source details. |  [optional] |
|**replace** | **Boolean** | Replace an existing dynamic registration when true. |  [optional] |
|**publication** | [**ArtifactPublicationRecord**](ArtifactPublicationRecord.md) | Signed artifact publication to install. |  [optional] |
|**coordinate** | [**ArtifactCoordinate**](ArtifactCoordinate.md) | Repository coordinate to fetch a signed publication record from when publication is omitted. |  [optional] |
|**patch** | [**ToolPatchParams**](ToolPatchParams.md) | Patch to apply to the registry tool entry. |  |
|**includeDisabled** | **Boolean** | Include disabled registry entries in the listing. |  [optional] |
|**includePrivate** | **Boolean** | Include private role-visible tools in the listing. |  [optional] |



## Enum: ModeEnum

| Name | Value |
|---- | -----|
| DISABLE | &quot;disable&quot; |



## Enum: PhaseEnum

| Name | Value |
|---- | -----|
| CHECK | &quot;check&quot; |
| INSTALL | &quot;install&quot; |
| ACTIVATE | &quot;activate&quot; |
| DEACTIVATE | &quot;deactivate&quot; |
| DELETE | &quot;delete&quot; |



