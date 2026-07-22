<#
.SYNOPSIS
Dispatches Meshingress lifecycle commands on Windows.

.DESCRIPTION
Routes each lifecycle action to its Windows action script. Shared runtime behavior
remains private to this platform and will be replaced by the universal launcher later.
#>
param(
    [ValidateSet("Menu", "Start", "Restart", "Stop", "Status", "Logs")]
    [string]$Action = "Menu",
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

$actionScript = Join-Path $PSScriptRoot "actions\$Action.ps1"
if (-not (Test-Path -LiteralPath $actionScript -PathType Leaf)) {
    throw "Windows Meshingress action is not available: $actionScript"
}

$forwardedParameters = @{}
foreach ($parameter in $PSBoundParameters.GetEnumerator()) {
    if ($parameter.Key -ne "Action") {
        $forwardedParameters[$parameter.Key] = $parameter.Value
    }
}

& $actionScript @forwardedParameters
