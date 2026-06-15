param(
    [string]$JarPath = ".\app\meshingress-server\target\meshingress.jar",
    [ValidateSet("Jar", "Maven")]
    [string]$Mode,
    [bool]$KeepSystemAwake = $true,
    [bool]$KeepDisplayAwake = $false,
    [switch]$TurnScreenOff,
    [int]$DisplayTimeoutMinutes = 0,
    [switch]$Build,
    [int]$Port,
    [string]$Profile,
    [string]$AdminToken,
    [int]$KeepAliveSeconds = 30
)

$ErrorActionPreference = "Stop"

# Win32 API Sleep Prevention & Display Control
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WinSleep {
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern uint SetThreadExecutionState(uint esFlags);
}

public static class WinMonitor {
    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern IntPtr SendMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
}
"@

$ES_CONTINUOUS      = [uint32]0x80000000L
$ES_SYSTEM_REQUIRED = [uint32]0x00000001
$ES_DISPLAY_REQUIRED = [uint32]0x00000002

function Enable-AwakeMode {
    [uint32]$flags = $ES_CONTINUOUS
    if ($KeepSystemAwake) {
        $flags = $flags -bor $ES_SYSTEM_REQUIRED
    }
    if ($KeepDisplayAwake) {
        $flags = $flags -bor $ES_DISPLAY_REQUIRED
    }
    [WinSleep]::SetThreadExecutionState($flags) | Out-Null
}

function Disable-AwakeMode {
    [WinSleep]::SetThreadExecutionState($ES_CONTINUOUS) | Out-Null
}

function Trigger-ScreenOff {
    # HWND_BROADCAST = 0xffff, WM_SYSCOMMAND = 0x0112, SC_MONITORPOWER = 0xf170, 2 = Off
    [WinMonitor]::SendMessage(0xffff, 0x0112, 0xf170, 2) | Out-Null
}

# 1. Build if requested
if ($Build) {
    Write-Host "Building Meshingress server..." -ForegroundColor Cyan
    & .\mvnw.cmd clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE"
    }
}

# 2. Determine execution mode and target command
if ([string]::IsNullOrEmpty($Mode)) {
    if (Test-Path $JarPath) {
        $Mode = "Jar"
    } else {
        $Mode = "Maven"
    }
}

Write-Host "Running in Mode: $Mode" -ForegroundColor Cyan

# 3. Handle temporary display timeout
$originalAcMinutes = $null
$originalDcMinutes = $null

if ($DisplayTimeoutMinutes -gt 0) {
    Write-Host "Reading original display timeout settings..." -ForegroundColor Yellow
    try {
        $powercfgQuery = powercfg /q SCHEME_CURRENT SUB_VIDEO VIDEOIDLE
        $acLine = $powercfgQuery | Select-String "Current AC Power Setting Index:"
        $dcLine = $powercfgQuery | Select-String "Current DC Power Setting Index:"
        
        if ($acLine -match "0x([0-9a-fA-F]+)") {
            $acSec = [System.Convert]::ToInt32($Matches[1], 16)
            $originalAcMinutes = if ($acSec -eq 0) { 0 } else { [Math]::Max(1, [Math]::Round($acSec / 60)) }
        }
        if ($dcLine -match "0x([0-9a-fA-F]+)") {
            $dcSec = [System.Convert]::ToInt32($Matches[1], 16)
            $originalDcMinutes = if ($dcSec -eq 0) { 0 } else { [Math]::Max(1, [Math]::Round($dcSec / 60)) }
        }
        
        Write-Host "Temporarily setting display timeout to $DisplayTimeoutMinutes minutes..." -ForegroundColor Yellow
        powercfg /change monitor-timeout-ac $DisplayTimeoutMinutes
        powercfg /change monitor-timeout-dc $DisplayTimeoutMinutes
    } catch {
        Write-Warning "Could not manage powercfg display timeout settings: $_"
    }
}

# 4. Prepare Spring Boot/Java system arguments
$springArgs = @()
if ($Port -gt 0) {
    $springArgs += "--server.port=$Port"
}
if ($Profile) {
    $springArgs += "--spring.profiles.active=$Profile"
}
if ($AdminToken) {
    $springArgs += "--meshingress.mcp.roles.admin-token=$AdminToken"
}

# 5. Enable awake mode
Write-Host "Enabling Awake state... System prevention: $KeepSystemAwake, Display prevention: $KeepDisplayAwake" -ForegroundColor Green
Enable-AwakeMode

try {
    $process = $null
    if ($Mode -eq "Jar") {
        if (-not (Test-Path $JarPath)) {
            throw "JAR not found: $JarPath. Please compile the project first using '-Build' or select 'Maven' mode."
        }
        Write-Host "Starting Meshingress server from JAR..." -ForegroundColor Green
        Write-Host "JAR: $JarPath" -ForegroundColor Gray
        
        $argList = @("-jar", "`"$JarPath`"") + $springArgs
        $process = Start-Process `
            -FilePath "java" `
            -ArgumentList $argList `
            -PassThru `
            -NoNewWindow
    } else {
        Write-Host "Starting Meshingress server via Maven (spring-boot:run)..." -ForegroundColor Green
        
        # Assemble run arguments for Maven boot plugin
        $mavenArgs = @("-pl", "app/meshingress-server", "-am", "spring-boot:run")
        if ($springArgs.Count -gt 0) {
            $joinedSpringArgs = $springArgs -join " "
            $mavenArgs += "-Dspring-boot.run.arguments=`"$joinedSpringArgs`""
        }
        
        $process = Start-Process `
            -FilePath ".\mvnw.cmd" `
            -ArgumentList $mavenArgs `
            -PassThru `
            -NoNewWindow
    }

    # 6. Turn screen off immediately if switch set
    if ($TurnScreenOff) {
        Write-Host "Turning screen off immediately as requested..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2 # Give a brief moment for the process to output some initial lines before screen turns off
        Trigger-ScreenOff
    }

    # 7. Monitor the process
    while (-not $process.HasExited) {
        # Refresh the system-required state periodically.
        [uint32]$flags = 0
        if ($KeepSystemAwake) { $flags = $flags -bor $ES_SYSTEM_REQUIRED }
        if ($KeepDisplayAwake) { $flags = $flags -bor $ES_DISPLAY_REQUIRED }
        if ($flags -gt 0) {
            [WinSleep]::SetThreadExecutionState($flags) | Out-Null
        }
        Start-Sleep -Seconds $KeepAliveSeconds
    }

    Write-Host "Server exited with code $($process.ExitCode)." -ForegroundColor Cyan
    exit $process.ExitCode
}
finally {
    Disable-AwakeMode
    
    # Restore original display timeout
    if ($null -ne $originalAcMinutes) {
        Write-Host "Restoring original AC display timeout to $originalAcMinutes minutes..." -ForegroundColor Yellow
        powercfg /change monitor-timeout-ac $originalAcMinutes
    }
    if ($null -ne $originalDcMinutes) {
        Write-Host "Restoring original DC display timeout to $originalDcMinutes minutes..." -ForegroundColor Yellow
        powercfg /change monitor-timeout-dc $originalDcMinutes
    }
    
    Write-Host "Sleep prevention released." -ForegroundColor Green
}