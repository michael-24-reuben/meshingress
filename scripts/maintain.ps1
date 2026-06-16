param(
    [string]$MockitoVersion = "5.23.0",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

function Find-ProjectRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$StartDirectory
    )

    $dir = Resolve-Path $StartDirectory

    while ($null -ne $dir) {
        $pom = Join-Path $dir.Path "pom.xml"
        $repoModule = Join-Path $dir.Path "app/meshingress-repository"

        if ((Test-Path $pom) -and (Test-Path $repoModule)) {
            return $dir.Path
        }

        $parent = Split-Path $dir.Path -Parent

        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $dir.Path) {
            break
        }

        $dir = Resolve-Path $parent
    }

    throw "Could not find project root. Expected a directory containing pom.xml and app/meshingress-repository."
}


function Test-NonEmptyFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return ((Test-Path $Path) -and ((Get-Item $Path).Length -gt 0))
}

function Ensure-MockitoAgent {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot,

        [string]$Version = "5.23.0",

        [switch]$Force
    )

    $relativeAgentPath = "app/meshingress-repository/target/agents/mockito-core.jar"
    $agentPath = Join-Path $ProjectRoot $relativeAgentPath

    $tokenPath = Join-Path $ProjectRoot ".cache/meshingress/agents/org.mockito.mockito-core.$Version.installed.json"

    $scriptDir = Split-Path -Parent $PSCommandPath
    $downloadScript = Join-Path $scriptDir "download-mockito-agent.ps1"

    if (-not (Test-Path $downloadScript)) {
        throw "Missing script: $downloadScript"
    }

    if (-not $Force) {
        if ((Test-Path $tokenPath) -and (Test-NonEmptyFile -Path $agentPath)) {
            Write-Host "Mockito agent already installed."
            Write-Host "Agent: $agentPath"
            Write-Host "Token: $tokenPath"
            return
        }
    }

    Write-Host "Installing Mockito agent..."

    & $downloadScript `
        -ProjectRoot $ProjectRoot `
        -Version $Version `
        -OutputPath $relativeAgentPath

    if (-not (Test-NonEmptyFile -Path $agentPath)) {
        throw "Mockito agent install failed: jar missing or empty at $agentPath"
    }

    $tokenDir = Split-Path -Parent $tokenPath

    if (-not (Test-Path $tokenDir)) {
        New-Item -ItemType Directory -Path $tokenDir -Force | Out-Null
    }

    $sourceUrl = "https://repo1.maven.org/maven2/org/mockito/mockito-core/$Version/mockito-core-$Version.jar"
    $sizeBytes = (Get-Item $agentPath).Length

    $metadata = [ordered]@{
        artifact = "org.mockito:mockito-core"
        version = $Version
        file = $relativeAgentPath
        source = $sourceUrl
        installedAt = (Get-Date).ToString("o")
        sizeBytes = $sizeBytes
        status = "installed"
    }

    $metadata |
        ConvertTo-Json -Depth 5 |
        Set-Content -Path $tokenPath -Encoding UTF8

    Write-Host "Mockito agent installed."
    Write-Host "Agent: $agentPath"
    Write-Host "Token: $tokenPath"
}

function Invoke-MeshingressMaintenance {
    param(
        [string]$MockitoVersion = "5.23.0",
        [switch]$Force
    )

    $scriptDir = Split-Path -Parent $PSCommandPath
    $projectRoot = Find-ProjectRoot -StartDirectory $scriptDir

    Write-Host "Project root: $projectRoot"

    Ensure-MockitoAgent `
        -ProjectRoot $projectRoot `
        -Version $MockitoVersion `
        -Force:$Force

    Write-Host "Maintenance checks complete."
}

Invoke-MeshingressMaintenance `
    -MockitoVersion $MockitoVersion `
    -Force:$Force