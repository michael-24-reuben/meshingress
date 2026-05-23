param(
    [string]$JarPath = ".\app\meshingress-server\target\meshingress.jar",
    [int]$KeepAliveSeconds = 30
)

$ErrorActionPreference = "Stop"

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WinSleep {
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern uint SetThreadExecutionState(uint esFlags);
}
"@

$ES_CONTINUOUS      = 0x80000000
$ES_SYSTEM_REQUIRED = 0x00000001
# Optional: uncomment if you also want the monitor kept on.
# $ES_DISPLAY_REQUIRED = 0x00000002

function Enable-AwakeMode {
    [WinSleep]::SetThreadExecutionState(
        $ES_CONTINUOUS -bor $ES_SYSTEM_REQUIRED
    ) | Out-Null
}

function Disable-AwakeMode {
    [WinSleep]::SetThreadExecutionState($ES_CONTINUOUS) | Out-Null
}

if (-not (Test-Path $JarPath)) {
    throw "JAR not found: $JarPath"
}

Write-Host "Starting Meshingress server..."
Write-Host "JAR: $JarPath"
Write-Host "Sleep prevention enabled while server is running."

Enable-AwakeMode

try {
    $process = Start-Process `
        -FilePath "java" `
        -ArgumentList @("-jar", "`"$JarPath`"") `
        -PassThru `
        -NoNewWindow

    while (-not $process.HasExited) {
        # Refresh the system-required state periodically.
        [WinSleep]::SetThreadExecutionState($ES_SYSTEM_REQUIRED) | Out-Null
        Start-Sleep -Seconds $KeepAliveSeconds
    }

    Write-Host "Server exited with code $($process.ExitCode)."
    exit $process.ExitCode
}
finally {
    Disable-AwakeMode
    Write-Host "Sleep prevention released."
}