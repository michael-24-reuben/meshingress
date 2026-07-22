#requires -Version 7.6.3
<#
.SYNOPSIS
Builds (optionally), uploads, assesses, approves, publishes, and installs the
x-faster-whisper tool artifact through Meshingress.

.DESCRIPTION
This is an explicit lifecycle runner. It does not clone SYSTRAN/faster-whisper,
install Python packages, provision a model, or change the tool bundle.  It
publishes the already-built Java tool-module JAR and asks Meshingress to install
that signed publication at runtime.

Use -WhatIf to print the target lifecycle before making any server requests.

.EXAMPLE
$env:MESHINGRESS_ADMIN_AUTHORIZATION = 'Bearer <admin-token>'
.\Invoke-FasterWhisperLifecycle.ps1 `
  -ServerBaseUrl 'http://127.0.0.1:4737' `
  -AdminAuthorization $env:MESHINGRESS_ADMIN_AUTHORIZATION `
  -Build `
  -RequestedScopes FILES_READ `
  -ApprovedScopes FILES_READ
#>
[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string] $ServerBaseUrl,

    [Parameter(Mandatory)]
    [ValidatePattern('^Bearer\s+\S+')]
    [string] $AdminAuthorization,

    [string] $GroupId = 'org.toolspace.fasterwhisper',

    [string] $ArtifactId = 'x-faster-whisper',

    [string] $Version = '0.0.1-SNAPSHOT',

    [string] $ToolId = 'faster-whisper',

    [string] $ArtifactJar,

    [switch] $Build,

    [string[]] $RequestedScopes = @(),

    [string[]] $ApprovedScopes = @(),

    [ValidateSet('APPROVED_TRUSTED', 'APPROVED_LIMITED')]
    [string] $TrustStatus = 'APPROVED_TRUSTED',

    [string] $Actor = 'x-faster-whisper-lifecycle',

    [string] $Reviewer = 'x-faster-whisper-lifecycle',

    [string] $ApprovalNotes = 'Explicit x-faster-whisper publication lifecycle.'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$moduleRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent (Split-Path -Parent $moduleRoot)
$mavenWrapper = Join-Path $repositoryRoot 'mvnw.cmd'

if ([string]::IsNullOrWhiteSpace($ArtifactJar)) {
    $ArtifactJar = Join-Path $moduleRoot "target/$ArtifactId-$Version.jar"
}

function Join-ArtifactUri {
    param([Parameter(Mandatory)] [AllowEmptyString()] [string] $Suffix)

    $base = $ServerBaseUrl.TrimEnd('/')
    $segments = @($GroupId, $ArtifactId, $Version)
    if (-not [string]::IsNullOrWhiteSpace($Suffix)) {
        $segments += $Suffix.Trim('/')
    }
    $segments = $segments | ForEach-Object { [uri]::EscapeDataString($_) }
    "$base/artifact/$($segments -join '/')"
}

function Add-Query {
    param(
        [Parameter(Mandatory)] [string] $Uri,
        [hashtable] $Values
    )

    $pairs = foreach ($key in $Values.Keys) {
        foreach ($value in @($Values[$key])) {
            if ($null -ne $value -and -not [string]::IsNullOrWhiteSpace("$value")) {
                '{0}={1}' -f [uri]::EscapeDataString($key), [uri]::EscapeDataString("$value")
            }
        }
    }
    if ($pairs.Count -eq 0) {
        return $Uri
    }
    "${Uri}?$($pairs -join '&')"
}

function New-RepositoryHeaders {
    param([Parameter(Mandatory)] [string] $Role)

    @{
        'X-Repository-Role' = $Role
        'X-Repository-Actor' = $Actor
        'X-Request-Id' = "x-faster-whisper-$Role-$([guid]::NewGuid().ToString('N'))"
    }
}

function Invoke-CurlJson {
    param(
        [Parameter(Mandatory)] [ValidateSet('POST')] [string] $Method,
        [Parameter(Mandatory)] [string] $Uri,
        [hashtable] $Headers = @{},
        [object] $JsonBody,
        [string] $MultipartFile
    )

    $arguments = @('--silent', '--show-error', '--write-out', "`n__HTTP_STATUS__:%{http_code}", '-X', $Method)
    foreach ($header in $Headers.GetEnumerator()) {
        $arguments += @('-H', "$($header.Key): $($header.Value)")
    }
    if ($PSBoundParameters.ContainsKey('JsonBody')) {
        # Publication records contain nested, signed review metadata.  Keep the complete
        # record intact or the server's signature verifier must reject the installation.
        $arguments += @('-H', 'Content-Type: application/json', '--data-binary', ($JsonBody | ConvertTo-Json -Depth 100 -Compress))
    }
    if ($PSBoundParameters.ContainsKey('MultipartFile')) {
        $arguments += @('-F', "file=@$MultipartFile")
    }
    $arguments += $Uri

    $output = & curl.exe @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "curl.exe failed with exit code $LASTEXITCODE for $Method $Uri"
    }
    $text = $output -join "`n"
    $match = [regex]::Match($text, '(?s)^(.*)\r?\n__HTTP_STATUS__:(\d{3})\s*$')
    if (-not $match.Success) {
        throw "Response did not contain an HTTP status marker: $text"
    }
    $body = $match.Groups[1].Value.Trim()
    $status = [int] $match.Groups[2].Value
    if ($status -ge 400) {
        throw "$Method $Uri failed with HTTP $status. Response body: $body"
    }
    if ([string]::IsNullOrWhiteSpace($body)) {
        return $null
    }
    try {
        return $body | ConvertFrom-Json -Depth 30
    } catch {
        throw "$Method $Uri returned a non-JSON response: $body"
    }
}

if ($Build -and -not $WhatIfPreference) {
    if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf)) {
        throw "Maven wrapper not found: $mavenWrapper"
    }
    if (-not $PSCmdlet.ShouldProcess($moduleRoot, 'Build x-faster-whisper JAR')) {
        return
    }
    & $mavenWrapper -pl 'toolspace/x-faster-whisper' -am -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
}

$uploadUri = Add-Query -Uri (Join-ArtifactUri -Suffix '') -Values @{
    type = 'TOOL_MODULE'
    packaging = 'jar'
    requestedScopes = $RequestedScopes
}
$assessUri = Join-ArtifactUri -Suffix 'assess'
$approveUri = Join-ArtifactUri -Suffix 'approve'
$publishUri = Join-ArtifactUri -Suffix 'publish'
$mcpUri = "$($ServerBaseUrl.TrimEnd('/'))/mcp"

if ($WhatIfPreference) {
    [pscustomobject]@{
        build = [bool] $Build
        jar = [IO.Path]::GetFullPath($ArtifactJar)
        upload = "POST $uploadUri"
        assess = "POST $assessUri"
        approve = "POST $approveUri"
        publish = "POST $publishUri"
        install = "POST $mcpUri (roles/tools/installPublication)"
    } | Format-List
    return
}

if (-not (Test-Path -LiteralPath $ArtifactJar -PathType Leaf)) {
    throw "Artifact JAR not found: $ArtifactJar. Build it first or pass -ArtifactJar."
}
if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    throw 'curl.exe is required for multipart artifact upload but was not found on PATH.'
}

if (-not $PSCmdlet.ShouldProcess("$GroupId/$ArtifactId/$Version", 'Upload, assess, approve, publish, and install tool artifact')) {
    return
}

Write-Host "Uploading $ArtifactJar" -ForegroundColor Cyan
$upload = Invoke-CurlJson -Method POST -Uri $uploadUri -Headers (New-RepositoryHeaders -Role 'uploader') -MultipartFile $ArtifactJar

Write-Host 'Assessing uploaded artifact' -ForegroundColor Cyan
$assessment = Invoke-CurlJson -Method POST -Uri $assessUri -Headers (New-RepositoryHeaders -Role 'reviewer')

if ($ApprovedScopes.Count -eq 0) {
    $ApprovedScopes = $RequestedScopes
}
$approval = @{
    approvedScopes = $ApprovedScopes
    deniedScopes = @()
    trustStatus = $TrustStatus
    reviewer = $Reviewer
    notes = $ApprovalNotes
}
Write-Host 'Approving assessed artifact' -ForegroundColor Cyan
$approved = Invoke-CurlJson -Method POST -Uri $approveUri -Headers (New-RepositoryHeaders -Role 'reviewer') -JsonBody $approval

Write-Host 'Publishing signed artifact record' -ForegroundColor Cyan
$publication = Invoke-CurlJson -Method POST -Uri $publishUri -Headers (New-RepositoryHeaders -Role 'publisher')

$installRequest = @{
    jsonrpc = '2.0'
    id = "x-faster-whisper-$([guid]::NewGuid().ToString('N'))"
    method = 'roles/tools/installPublication'
    params = @{
        toolId = $ToolId
        # Reuse the signed record returned by publish.  Coordinate-only installation
        # intentionally needs repository.api-base-url, while local repository mode
        # can safely install this already-returned publication directly.
        publication = $publication
    }
}
Write-Host 'Installing signed publication through MCP' -ForegroundColor Cyan
$install = Invoke-CurlJson -Method POST -Uri $mcpUri -Headers @{ Authorization = $AdminAuthorization } -JsonBody $installRequest
$installError = $install.PSObject.Properties['error']
if ($null -ne $installError) {
    throw "roles/tools/installPublication returned JSON-RPC error $($installError.Value.code): $($installError.Value.message)"
}

[pscustomobject]@{
    uploadTrustStatus = $upload.trustStatus
    assessmentTrustStatus = $assessment.trustStatus
    approvalTrustStatus = $approved.trustStatus
    publicationSignatureKeyId = $publication.signatureKeyId
    installSucceeded = $install.result.installed
    registrationId = $install.result.registrationId
    runtimeModuleId = $install.result.runtimeModuleId
} | Format-List
