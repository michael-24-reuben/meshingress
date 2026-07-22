

# ScannerResult


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**scanner** | **String** |  |  [optional] |
|**scannerVersion** | **String** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**findings** | [**List&lt;Finding&gt;**](Finding.md) |  |  [optional] |
|**rawSummary** | **Map&lt;String, Object&gt;** |  |  [optional] |
|**rawReportPath** | [**ScannerResultRawReportPath**](ScannerResultRawReportPath.md) |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PASSED | &quot;PASSED&quot; |
| REVIEW | &quot;REVIEW&quot; |
| BLOCKED | &quot;BLOCKED&quot; |
| FAILED | &quot;FAILED&quot; |



