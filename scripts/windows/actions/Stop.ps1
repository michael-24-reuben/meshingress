param(
    [switch]$Headless, [switch]$Detached, [switch]$Debug, [switch]$Build,
    [switch]$SkipTests, [switch]$EnableJvmDebug, [int]$ServerDebugPort,
    [switch]$Foreground, [switch]$ForceRestart, [int]$StartupTimeoutSeconds,
    [string]$ServerAddress, [int]$ServerPort, [switch]$SkipServerHealthCheck
)

& (Join-Path $PSScriptRoot "..\private\Runtime.ps1") -Action Stop @PSBoundParameters
