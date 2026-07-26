<#
.SYNOPSIS
Convenient Windows entry point for Meshingress lifecycle commands.

.DESCRIPTION
Forwards to the Windows action dispatcher without requiring callers to select the
platform-specific script path.
#>
param(
    [ValidateSet("Menu", "Start", "Restart", "Stop", "Status", "Logs")]
    [string]$Action = "Menu",
    [ValidateSet("OpenAPI", "README", "LICENSE", "CHANGELOG")]
    [string]$Docs,
    [switch]$Headless,
    [switch]$Detached,
    [switch]$Debug,
    [switch]$Build,
    [switch]$SkipTests,
    [switch]$EnableJvmDebug,
    [ValidateRange(1024, 65535)][int]$ServerDebugPort = 5005,
    [switch]$Foreground,
    [switch]$ForceRestart,
    [ValidateRange(10, 900)][int]$StartupTimeoutSeconds = 120,
    [string]$ServerAddress = "",
    [ValidateRange(0, 65535)][int]$ServerPort = 0,
    [switch]$SkipServerHealthCheck
)

if ($PSBoundParameters.ContainsKey("Action") -and $PSBoundParameters.ContainsKey("Docs")) {
    throw "Specify either -Action or -Docs, not both."
}

$windowsDispatcher = Join-Path $PSScriptRoot "scripts\windows\Launcher.ps1"
if (-not (Test-Path -LiteralPath $windowsDispatcher -PathType Leaf)) {
    throw "Windows Meshingress dispatcher not found: $windowsDispatcher"
}

& $windowsDispatcher @PSBoundParameters
