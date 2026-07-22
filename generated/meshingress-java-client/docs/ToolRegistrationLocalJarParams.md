

# ToolRegistrationLocalJarParams

Local JAR registration source for a runtime tool module.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**path** | **String** | Filesystem path to the local tool JAR. Either path or jarPath may be supplied. |  [optional] |
|**jarPath** | **String** | Legacy filesystem path to the local tool JAR. Prefer path for new callers. |  [optional] |
|**checksumSha256** | **String** | Expected SHA-256 checksum for the JAR. |  [optional] |
|**sha256** | **String** | Legacy SHA-256 checksum field. Prefer checksumSha256 for new callers. |  [optional] |



