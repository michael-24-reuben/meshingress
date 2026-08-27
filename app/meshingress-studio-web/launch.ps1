<#
.SYNOPSIS
    Meshingress Studio Web - Root PowerShell Launch Script

.DESCRIPTION
    Validates environment, cleans previous build artifacts, compiles TypeScript and Vite bundles,
    verifies generated artifacts, and starts the development server (npm run dev).

.EXAMPLE
    .\launch.ps1
    .\launch.ps1 --build-only
    .\launch.ps1 --clean-only
    .\launch.ps1 --port 3000
    .\launch.ps1 --skip-build
#>

[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ForwardArgs
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -Path $ScriptDir

# Check Node.js runtime
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCmd) {
    Write-Host "[ERROR] Node.js is not found in PATH." -ForegroundColor Red
    Write-Host "Please install Node.js (>= 18.0.0) from https://nodejs.org/" -ForegroundColor Yellow
    exit 1
}

$launcherScript = Join-Path $ScriptDir "scripts\launcher.js"
if (-not (Test-Path $launcherScript)) {
    Write-Host "[ERROR] Launcher script not found at $launcherScript" -ForegroundColor Red
    exit 1
}

try {
    if ($ForwardArgs) {
        & node $launcherScript @ForwardArgs
    } else {
        & node $launcherScript
    }
    exit $LASTEXITCODE
}
catch {
    Write-Host "`n[ERROR] Launch failed: $_" -ForegroundColor Red
    exit 1
}
