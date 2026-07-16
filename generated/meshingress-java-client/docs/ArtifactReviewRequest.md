

# ArtifactReviewRequest


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**approvedScopes** | **List&lt;String&gt;** |  |  [optional] |
|**deniedScopes** | [**List&lt;DeniedScope&gt;**](DeniedScope.md) |  |  [optional] |
|**trustStatus** | [**TrustStatusEnum**](#TrustStatusEnum) |  |  [optional] |
|**reviewer** | **String** |  |  [optional] |
|**notes** | **String** |  |  [optional] |



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



