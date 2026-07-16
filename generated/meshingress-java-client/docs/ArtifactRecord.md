

# ArtifactRecord


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**coordinate** | [**ArtifactCoordinate**](ArtifactCoordinate.md) |  |  [optional] |
|**type** | [**TypeEnum**](#TypeEnum) |  |  [optional] |
|**trustStatus** | [**TrustStatusEnum**](#TrustStatusEnum) |  |  [optional] |
|**artifactUri** | **String** |  |  [optional] |
|**artifactChecksum** | [**ArtifactChecksum**](ArtifactChecksum.md) |  |  [optional] |
|**files** | [**List&lt;ArtifactFileEntry&gt;**](ArtifactFileEntry.md) |  |  [optional] |
|**scopes** | [**ArtifactScopeDeclaration**](ArtifactScopeDeclaration.md) |  |  [optional] |
|**assessment** | [**ArtifactAssessmentSummary**](ArtifactAssessmentSummary.md) |  |  [optional] |
|**createdAt** | **OffsetDateTime** |  |  [optional] |
|**updatedAt** | **OffsetDateTime** |  |  [optional] |



## Enum: TypeEnum

| Name | Value |
|---- | -----|
| TOOL_MODULE | &quot;TOOL_MODULE&quot; |
| CLI_HARNESS | &quot;CLI_HARNESS&quot; |
| AVAILABILITY_ANNOTATION | &quot;AVAILABILITY_ANNOTATION&quot; |
| AVAILABILITY_POLICY | &quot;AVAILABILITY_POLICY&quot; |
| SCOPE_POLICY | &quot;SCOPE_POLICY&quot; |



## Enum: TrustStatusEnum

| Name | Value |
|---- | -----|
| RECEIVED | &quot;RECEIVED&quot; |
| QUARANTINED | &quot;QUARANTINED&quot; |
| SCANNING | &quot;SCANNING&quot; |
| REVIEW_PENDING | &quot;REVIEW_PENDING&quot; |
| APPROVED_TRUSTED | &quot;APPROVED_TRUSTED&quot; |
| APPROVED_LIMITED | &quot;APPROVED_LIMITED&quot; |
| REJECTED | &quot;REJECTED&quot; |
| BLOCKED_MALWARE | &quot;BLOCKED_MALWARE&quot; |
| BLOCKED_POLICY | &quot;BLOCKED_POLICY&quot; |
| BLOCKED_VULNERABILITY | &quot;BLOCKED_VULNERABILITY&quot; |
| SUPERSEDED | &quot;SUPERSEDED&quot; |
| DELETED | &quot;DELETED&quot; |
| REVOKED | &quot;REVOKED&quot; |



