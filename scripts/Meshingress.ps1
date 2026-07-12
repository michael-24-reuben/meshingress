<#
.SYNOPSIS
Starts and manages the Meshingress MCP server and artifact repository service.

.DESCRIPTION
Runs the packaged Spring Boot jars as managed background processes. Each process has
separate stdout and stderr logs under var/logs/meshingress, while the state file under
var/run records only the processes started by this script.

Use -Debug for Spring Boot debug logging. Use -EnableJvmDebug only for local debugger
attachment; its JDWP listeners are bound to loopback addresses.
#>
param(
    # `Menu` -> Launch the control center (default)
    # `Start` -> Start the services. Contains options for headless, debug, and foreground modes.
    # `Restart` -> Stop and start the services. Inherits the same options as `Start`.
    # `Stop` -> Stop the services.
    # `Status` -> Show the status of the services.
    # `Logs` -> Follow the logs of the services.
    [ValidateSet("Menu", "Start", "Restart", "Stop", "Status", "Logs")]
    [string]$Action = "Menu",

    # If true, and called after `Start`, the script will launch/relaunch itself.
    [switch]$Headless,

    [switch]$Detached,

    # If true, and called after `Start`, indicates that this is the detached child process of a headless launch.
    [switch]$Debug,

    [switch]$Build,

    [switch]$SkipTests,

    [switch]$EnableJvmDebug,

    [ValidateRange(1024, 65535)]
    [int]$ServerDebugPort = 5005,

    [ValidateRange(1024, 65535)]
    [int]$RepositoryDebugPort = 5006,

    [switch]$Foreground,

    [switch]$ForceRestart,

    [ValidateRange(10, 900)]
    [int]$StartupTimeoutSeconds = 120,

    [string]$ServerAddress = "",

    [ValidateRange(0, 65535)]
    [int]$ServerPort = 0,

    [string]$RepositoryAddress = "",

    [ValidateRange(0, 65535)]
    [int]$RepositoryPort = 0,

    [switch]$SkipServerHealthCheck
)


Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$utf8 = [System.Text.UTF8Encoding]::new($false)

try {
    [Console]::InputEncoding  = $utf8
    [Console]::OutputEncoding = $utf8
}
catch {
    # No attached console
}

$OutputEncoding = $utf8

# Particularly relevant to Windows PowerShell 5.1.
if ($PSVersionTable.PSVersion.Major -lt 6) {
    chcp.com 65001 *> $null
}

$asciiArt = Get-Content `
    -Path "$PSScriptRoot\..\data\assets\logo\meshingress-bloody.ascii.txt" `
    -Raw `
    -Encoding utf8

function Find-ProjectRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$StartDirectory
    )

    $directory = Resolve-Path $StartDirectory
    while ($null -ne $directory) {
        if ((Test-Path (Join-Path $directory.Path "pom.xml")) -and
            (Test-Path (Join-Path $directory.Path "app/meshingress-server")) -and
            (Test-Path (Join-Path $directory.Path "app/meshingress-repository"))) {
            return $directory.Path
        }

        $parent = Split-Path $directory.Path -Parent
        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $directory.Path) {
            break
        }

        $directory = Resolve-Path $parent
    }

    Write-Host-Exception -Exception ([System.IO.DirectoryNotFoundException]::new(
        "Could not find the Meshingress project root."
    ))
}

$projectRoot = Find-ProjectRoot -StartDirectory $PSScriptRoot
$logDirectory = Join-Path $projectRoot "var/logs/meshingress"
$runDirectory = Join-Path $projectRoot "var/run"
$statePath = Join-Path $runDirectory "meshingress-services.json"


function Write-Host-Logged {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory, ValueFromPipeline)]
        [AllowEmptyString()]
        [string]$Message,

        [ValidateSet("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL")]
        [string]$Level = "INFO",

        [ValidateSet("launcher", "process", "config", "runtime", "service")]
        [string]$Component = "launcher",

        [string]$Event = "MESSAGE",

        [string]$LogDirectory = $script:logDirectory
    )

    process {
        if ([string]::IsNullOrWhiteSpace($LogDirectory)) {
            throw "LogDirectory is required."
        }

        $timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss.fffK"

        # Keep each log record on one physical line.
        $normalizedMessage = $Message `
            -replace "`r`n", "\n" `
            -replace "`r", "\n" `
            -replace "`n", "\n"

        $levelField = $Level.PadRight(5)

        $line = '{0} [{1}] [{2}] [{3}] pid={4} {5}' -f `
            $timestamp,
            $levelField,
            $Component,
            $Event,
            $PID,
            $normalizedMessage

        # Always write to the log file.
        $logFile = Join-Path $LogDirectory "console.log"
        $parentDirectory = Split-Path -Parent $logFile

        New-Item `
            -ItemType Directory `
            -Path $parentDirectory `
            -Force |
            Out-Null

        Add-Content `
            -LiteralPath $logFile `
            -Value $line `
            -Encoding utf8

        # Interactive mode also writes to the console.
        if (-not $script:Headless) {
            switch ($Level) {
                "WARN" {
                    Microsoft.PowerShell.Utility\Write-Host $Message `
                        -ForegroundColor Yellow
                }

                { $_ -in "ERROR", "FATAL" } {
                    Microsoft.PowerShell.Utility\Write-Host $Message `
                        -ForegroundColor Red
                }

                "DEBUG" {
                    Microsoft.PowerShell.Utility\Write-Host $Message `
                        -ForegroundColor DarkGray
                }

                "TRACE" {
                    Microsoft.PowerShell.Utility\Write-Host $Message `
                        -ForegroundColor DarkGray
                }

                default {
                    Microsoft.PowerShell.Utility\Write-Host $Message
                }
            }
        }
    }
}

function Write-Host-Exception {
    param(
        [Parameter(Mandatory = $true, ValueFromPipeline = $true)]
        [System.Exception]$Exception,

        [Boolean]$CallExit = $true,

        [switch]$Rethrow
    )

    if (-not $Exception.Data.Contains("MeshingressLogged")) {
        Write-Host-Logged "ERROR: $($Exception.Message)"
        if ($Debug) {
            Write-Host-Logged $Exception.ToString()
        }
        $Exception.Data["MeshingressLogged"] = $true
    }

    if ($Rethrow) {
        throw $Exception
    }

    if ($CallExit) {
        exit 1
    }
}

#function Run-HeadlessDetached

function Show-MeshingressControlCenter {
    param(
        [Parameter(Mandatory)]
        [string]$StatePath,

        [Parameter(Mandatory)]
        [string]$ServerAddress,

        [Parameter(Mandatory)]
        [int]$ServerPort,

        [Parameter(Mandatory)]
        [string]$RepositoryAddress,

        [Parameter(Mandatory)]
        [int]$RepositoryPort,

        [Parameter(Mandatory)]
        [string]$LogDirectory
    )

    while ($true) {
        Clear-Host

        $state = Get-State -StatePath $StatePath
        $serverProcess = $null
        $repositoryProcess = $null

        if ($null -ne $state) {
            $serverProcess = Get-ManagedProcess -Service $state.services.server
            $repositoryProcess = Get-ManagedProcess -Service $state.services.repository
        }

        $isRunning = $null -ne $serverProcess -or $null -ne $repositoryProcess

        $runtimeState = if ($isRunning) {
            "RUNNING"
        }
        elseif ($null -ne $state) {
            "STALE STATE"
        }
        else {
            "STOPPED"
        }

        $startedAt = "__"
        $uptime = "__"

        if ($null -ne $state -and $state.startedAt) {
            $started = [DateTimeOffset]::Parse($state.startedAt)
            $startedAt = $started.ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss")

            if ($isRunning) {
                $elapsed = [DateTimeOffset]::UtcNow - $started
                $uptime = "{0:dd\.hh\:mm\:ss}" -f $elapsed
            }
        }

        $javaVersion = try {
            (& java -version 2>&1 | Select-Object -First 1).ToString()
        }
        catch {
            "Unavailable"
        }

        $rows = @(
            "Runtime       $runtimeState"
            "Server        http://$(Get-ProbeAddress $ServerAddress):$ServerPort"
            "Repository    http://$(Get-ProbeAddress $RepositoryAddress):$RepositoryPort"
            "Started       $startedAt"
            "Uptime        $uptime"
            "Java          $javaVersion"
            "Logs          $LogDirectory"
        )

        Write-Host $asciiArt

        Write-MeshingressPanel `
            -Title "MESHINGRESS CONTROL CENTER" `
            -Rows $rows

        Write-Host ""
        Write-Host "  [1] Start              [2] Start headless"
        Write-Host "  [3] Status             [4] Follow logs"
        Write-Host "  [5] Restart            [6] Stop"
        Write-Host "  [Q] Exit"
        Write-Host ""

        switch ((Read-Host "Select").Trim().ToUpperInvariant()) {
            "1" { return "Start" }

            "2" {
                $script:Headless = $true
                return "Start"
            }

            "3" { return "Status" }
            "4" { return "Logs" }
            "5" { return "Restart" }
            "6" { return "Stop" }
            "Q" { return "Exit" }
        }
    }
}

function Write-MeshingressPanel {
    param(
        [Parameter(Mandatory)]
        [string]$Title,

        [Parameter(Mandatory)]
        [string[]]$Rows,

        [ValidateRange(40, 160)]
        [int]$Width = 93
    )

    $topLeft     = [string]([char]0x2554) # ╔
    $topRight    = [string]([char]0x2557) # ╗
    $bottomLeft  = [string]([char]0x255A) # ╚
    $bottomRight = [string]([char]0x255D) # ╝
    $horizontal  = [string]([char]0x2550) # ═
    $vertical    = [string]([char]0x2551) # ║
    $ellipsis    = [string]([char]0x2026) # …

    $innerWidth = $Width - 4
    $titleText = " $Title "

    $remaining = [Math]::Max(
        0,
        $Width - 2 - $titleText.Length
    )

    $leftLength = [Math]::Floor($remaining / 2)
    $rightLength = $remaining - $leftLength

    $topBorder =
        $topLeft +
        ($horizontal * $leftLength) +
        $titleText +
        ($horizontal * $rightLength) +
        $topRight

    Microsoft.PowerShell.Utility\Write-Host $topBorder

    foreach ($row in $Rows) {
        $display = [string]$row

        if ($display.Length -gt $innerWidth) {
            $display =
                $display.Substring(0, $innerWidth - 1) +
                $ellipsis
        }

        $display = $display.PadRight($innerWidth)

        Microsoft.PowerShell.Utility\Write-Host (
            "$vertical $display $vertical"
        )
    }

    $bottomBorder =
        $bottomLeft +
        ($horizontal * ($Width - 2)) +
        $bottomRight

    Microsoft.PowerShell.Utility\Write-Host $bottomBorder
}

function Get-PropertiesValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$DefaultValue
    )

    $escapedName = [regex]::Escape($Name)
    $match = Select-String -LiteralPath $Path -Pattern "^\s*$escapedName\s*=\s*(.+?)\s*$" |
        Select-Object -First 1

    if ($null -eq $match) {
        return $DefaultValue
    }

    return $match.Matches[0].Groups[1].Value.Trim()
}

function Get-ProbeAddress {
    param([string]$Address)

    if ([string]::IsNullOrWhiteSpace($Address) -or
        $Address -eq "0.0.0.0" -or
        $Address -eq "::") {
        return "127.0.0.1"
    }

    return $Address
}

function Test-TcpEndpoint {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,

        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne(1000, $false)) {
            return $false
        }

        $client.EndConnect($connection)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Test-HealthEndpoint {
    param([Parameter(Mandatory = $true)][string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

function Get-ListeningProcess {
    param([Parameter(Mandatory = $true)][int]$Port)

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $listener) {
        return $null
    }

    return $listener.OwningProcess
}

function Assert-PortAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,

        [Parameter(Mandatory = $true)]
        [string]$Purpose
    )

    $processId = Get-ListeningProcess -Port $Port
    if ($null -ne $processId) {
        Write-Host-Exception -Exception ([System.InvalidOperationException]::new(
            "$Purpose cannot start because port $Port is already listening (PID $processId)."
        ))
    }
}

function Get-State {
    param([Parameter(Mandatory = $true)][string]$StatePath)

    if (-not (Test-Path -LiteralPath $StatePath)) {
        return $null
    }

    try {
        return Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
    }
    catch {
        Write-Host-Exception -Exception ([System.IO.InvalidDataException]::new(
            "The managed-service state file is unreadable: $StatePath. Inspect or remove it before continuing.",
            $_.Exception
        ))
    }
}

function Get-ManagedProcess {
    param([Parameter(Mandatory = $true)]$Service)

    try {
        $process = Get-Process -Id ([int]$Service.pid) -ErrorAction Stop
    }
    catch {
        return $null
    }

    $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($Service.pid)" -ErrorAction SilentlyContinue).CommandLine
    if ([string]::IsNullOrWhiteSpace($commandLine) -or
        $commandLine.IndexOf([string]$Service.jar, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        return $null
    }

    return $process
}

function Stop-ManagedServices {
    param(
        [Parameter(Mandatory = $true)]$State,

        [Parameter(Mandatory = $true)][string]$StatePath
    )

    foreach ($name in @("server", "repository")) {
        $service = $State.services.$name
        if ($null -eq $service) {
            continue
        }

        $process = Get-ManagedProcess -Service $service
        if ($null -eq $process) {
            Write-Host "$name is not running as the recorded managed process."
            continue
        }

        Write-Host-Logged "Stopping $name (PID $($process.Id))..."
        Stop-Process -Id $process.Id -ErrorAction Stop
        $process.WaitForExit(15000) | Out-Null
        if (-not $process.HasExited) {
            Write-Host-Exception -Exception ([System.TimeoutException]::new(
                "$name did not exit within 15 seconds (PID $($process.Id))."
            ))
        }
    }

    Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue
    Write-Host-Logged "Managed Meshingress services stopped."
}

function Invoke-Build {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [switch]$SkipTests
    )

    $mavenWrapper = Join-Path $ProjectRoot "mvnw.cmd"
    if (-not (Test-Path -LiteralPath $mavenWrapper)) {
        Write-Host-Exception -Exception ([System.IO.FileNotFoundException]::new(
            "Maven wrapper not found: $mavenWrapper",
            $mavenWrapper
        ))
    }

    $arguments = @(
        "-pl", "app/meshingress-server,app/meshingress-repository",
        "-am"
    )
    if ($SkipTests) {
        $arguments += "-DskipTests"
    }
    $arguments += "package"

    Write-Host-Logged "Building Meshingress service jars..."
    & $mavenWrapper @arguments
    if ($LASTEXITCODE -ne 0) {
        Write-Host-Exception -Exception ([System.InvalidOperationException]::new(
            "Maven package failed with exit code $LASTEXITCODE."
        ))
    }
}

function ConvertTo-CommandArgument {
    param([Parameter(Mandatory = $true)][string]$Value)

    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    return '"' + ($Value -replace '"', '\"') + '"'
}

function Start-ServiceProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$JavaPath,
        [Parameter(Mandatory = $true)][string]$JarPath,
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$LogDirectory,
        [Parameter(Mandatory = $true)][string]$Timestamp,
        [string[]]$ApplicationArguments = @(),
        [string[]]$JvmArguments = @(),
        [switch]$Hidden
    )

    $stdoutPath = Join-Path $LogDirectory "$Name-$Timestamp.stdout.log"
    $stderrPath = Join-Path $LogDirectory "$Name-$Timestamp.stderr.log"
    $arguments = @($JvmArguments) + @("-jar", $JarPath) + @($ApplicationArguments)
    $argumentLine = (($arguments | ForEach-Object { ConvertTo-CommandArgument -Value $_ }) -join " ")

    Write-Host-Logged "Starting $Name..."
    $startProcessArguments = @{
        FilePath = $JavaPath
        ArgumentList = $argumentLine
        WorkingDirectory = $ProjectRoot
        RedirectStandardOutput = $stdoutPath
        RedirectStandardError = $stderrPath
        PassThru = $true
    }
    if ($Hidden) {
        $startProcessArguments.WindowStyle = "Hidden"
    }

    $process = Start-Process @startProcessArguments

    return [ordered]@{
        name = $Name
        pid = $process.Id
        jar = $JarPath
        stdout = $stdoutPath
        stderr = $stderrPath
        startedAt = $process.StartTime.ToUniversalTime().ToString("o")
    }
}

function Get-RecentLogs {
    param([Parameter(Mandatory = $true)]$Service)

    $lines = @()
    foreach ($path in @($Service.stdout, $Service.stderr)) {
        if (Test-Path -LiteralPath $path) {
            $lines += "--- $path ---"
            $lines += Get-Content -LiteralPath $path -Tail 30 -ErrorAction SilentlyContinue
        }
    }
    return ($lines -join [Environment]::NewLine)
}

function Wait-ForService {
    param(
        [Parameter(Mandatory = $true)]$Service,
        [Parameter(Mandatory = $true)][string]$ProbeAddress,
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds,
        [string]$HealthUrl = ""
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $process = Get-ManagedProcess -Service $Service
        if ($null -eq $process) {
            Write-Host-Exception `
                -Exception ([System.InvalidOperationException]::new(
                    "$($Service.name) exited during startup.`n$(Get-RecentLogs -Service $Service)"
                )) `
                -CallExit $false `
                -Rethrow
        }

        if (Test-TcpEndpoint -HostName $ProbeAddress -Port $Port) {
            if ([string]::IsNullOrWhiteSpace($HealthUrl) -or (Test-HealthEndpoint -Url $HealthUrl)) {
                Write-Host-Logged "$($Service.name) is ready on $ProbeAddress`:$Port (PID $($Service.pid))."
                return
            }
        }

        Start-Sleep -Milliseconds 500
    }

    Write-Host-Exception `
        -Exception ([System.TimeoutException]::new(
            "$($Service.name) did not become ready within $TimeoutSeconds seconds.`n$(Get-RecentLogs -Service $Service)"
        )) `
        -CallExit $false `
        -Rethrow
}

function Follow-Logs {
    param([Parameter(Mandatory = $true)]$State)

    $jobs = @()
    foreach ($name in @("server", "repository")) {
        $service = $State.services.$name
        foreach ($path in @($service.stdout, $service.stderr)) {
            if (-not (Test-Path -LiteralPath $path)) {
                continue
            }

            $jobs += Start-Job -ArgumentList $name, $path -ScriptBlock {
                param($serviceName, $logPath)
                Get-Content -LiteralPath $logPath -Tail 40
                Get-Content -LiteralPath $logPath -Tail 0 -Wait | ForEach-Object {
                    "[$serviceName] $_"
                }
            }
        }
    }

    if ($jobs.Count -eq 0) {
        Write-Host-Exception `
            -Exception ([System.InvalidOperationException]::new(
                "No managed log files are available to follow."
            )) `
            -CallExit $false
        return
    }

    Write-Host "Following managed logs. Press Ctrl+C to stop following; the services will keep running."
    try {
        while ($true) {
            foreach ($job in $jobs) {
                Receive-Job -Job $job -ErrorAction SilentlyContinue
            }
            Start-Sleep -Milliseconds 250
        }
    }
    finally {
        $jobs | Stop-Job -ErrorAction SilentlyContinue
        $jobs | Remove-Job -Force -ErrorAction SilentlyContinue
    }
}


Write-Host-Logged "Starting Meshingress services - [$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')]"

# Read `properties` files and extract the configured `server.address` and `server.port` values. If the user did not specify the server address or port
# * `app/meshingress-server/src/main/resources/application.properties`
# * `app/meshingress-repository/src/main/resources/application.properties`
if ($ServerAddress -eq "") {
    $ServerAddress = Get-PropertiesValue -Path "$PSScriptRoot\..\app\meshingress-server\src\main\resources\application.properties" -Name "server.address" -DefaultValue "127.0.0.1"
    Write-Host-Logged "Server address not specified. Using default: $ServerAddress"
}
if ($RepositoryAddress -eq "") {
    $RepositoryAddress = Get-PropertiesValue -Path "$PSScriptRoot\..\app\meshingress-repository\src\main\resources\application.properties" -Name "server.address" -DefaultValue "127.0.0.1"
    Write-Host-Logged "Repository address not specified. Using default: $RepositoryAddress"
}
if ($ServerPort -eq 0) {
    $ServerPort = [int](Get-PropertiesValue -Path "$PSScriptRoot\..\app\meshingress-server\src\main\resources\application.properties" -Name "server.port" -DefaultValue "4737")
    Write-Host-Logged "Server port not specified. Using default: $ServerPort"
}
if ($RepositoryPort -eq 0) {
    $RepositoryPort = [int](Get-PropertiesValue -Path "$PSScriptRoot\..\app\meshingress-repository\src\main\resources\application.properties" -Name "server.port" -DefaultValue "4738")
    Write-Host-Logged "Repository port not specified. Using default: $RepositoryPort"
}


if ($Action -eq "Menu") {
    $Action = Show-MeshingressControlCenter `
        -StatePath $statePath `
        -ServerAddress $ServerAddress `
        -ServerPort $ServerPort `
        -RepositoryAddress $RepositoryAddress `
        -RepositoryPort $RepositoryPort `
        -LogDirectory $logDirectory

    if ($Action -eq "Exit") {
        return
    }
}

if ($Action -eq "Restart") {
    $state = Get-State -StatePath $statePath

    if ($null -ne $state) {
        Stop-ManagedServices -State $state -StatePath $statePath
    }

    $Action = "Start"
}

if ($Action -eq "Start") {

}

if ($Action -eq "Stop") {
    $state = Get-State -StatePath $statePath
    if ($null -eq $state) {
        Write-Host-Logged "No managed Meshingress services are recorded."
        return
    }
    Stop-ManagedServices -State $state -StatePath $statePath
    return
}

if ($Action -eq "Status") {
    $state = Get-State -StatePath $statePath
    if ($null -eq $state) {
        Write-Host-Logged "No managed Meshingress services are recorded."
        return
    }

    foreach ($name in @("server", "repository")) {
        $service = $state.services.$name
        $process = Get-ManagedProcess -Service $service
        if ($null -eq $process) {
            Write-Host-Logged "${name}: stopped or no longer owned by this launcher"
        }
        else {
            Write-Host-Logged "${name}: running (PID $($process.Id)), stdout: $($service.stdout), stderr: $($service.stderr)"
        }
    }
    return
}

if ($Action -eq "Logs") {
    $state = Get-State -StatePath $statePath
    if ($null -eq $state) {
        Write-Host-Exception -Exception ([System.InvalidOperationException]::new(
            "No managed Meshingress services are recorded. Start them first."
        ))
    }
    Follow-Logs -State $state
    return
}

# Relaunch this script in a detached, hidden PowerShell process.
if ($Headless -and -not $Detached) {
    $powerShellExe = if ($PSVersionTable.PSEdition -eq "Core") {
        "pwsh.exe"
    }
    else {
        "powershell.exe"
    }

    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", "`"$PSCommandPath`"",
        "-Action", "Start",
        "-Headless",
        "-Detached",
        "-StartupTimeoutSeconds", ([string]$StartupTimeoutSeconds),
        "-ServerAddress", $ServerAddress,
        "-ServerPort", ([string]$ServerPort),
        "-RepositoryAddress", $RepositoryAddress,
        "-RepositoryPort", ([string]$RepositoryPort),
        "-ServerDebugPort", ([string]$ServerDebugPort),
        "-RepositoryDebugPort", ([string]$RepositoryDebugPort)
    )

    if ($Debug) {
        $arguments += "-Debug"
    }
    if ($Build) {
        $arguments += "-Build"
    }
    if ($SkipTests) {
        $arguments += "-SkipTests"
    }
    if ($EnableJvmDebug) {
        $arguments += "-EnableJvmDebug"
    }
    if ($ForceRestart) {
        $arguments += "-ForceRestart"
    }
    if ($SkipServerHealthCheck) {
        $arguments += "-SkipServerHealthCheck"
    }

    $process = Start-Process `
        -FilePath $powerShellExe `
        -ArgumentList $arguments `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru

    $message = "Started headless Meshingress process. PID: $($process.Id)"
    Microsoft.PowerShell.Utility\Write-Host $message
    Write-Host-Logged $message
    exit
}


$serverPropertiesPath = Join-Path $projectRoot "app/meshingress-server/src/main/resources/application.properties"
$configuredServerAddress = Get-PropertiesValue -Path $serverPropertiesPath -Name "server.address" -DefaultValue "127.0.0.1"
$configuredServerPort = [int](Get-PropertiesValue -Path $serverPropertiesPath -Name "server.port" -DefaultValue "4737")
$effectiveServerAddress = if ([string]::IsNullOrWhiteSpace($ServerAddress)) { $configuredServerAddress } else { $ServerAddress }
$effectiveServerPort = if ($ServerPort -eq 0) { $configuredServerPort } else { $ServerPort }
$effectiveRepositoryAddress = if ([string]::IsNullOrWhiteSpace($RepositoryAddress)) { "127.0.0.1" } else { $RepositoryAddress }
$effectiveRepositoryPort = if ($RepositoryPort -eq 0) { 8080 } else { $RepositoryPort }

# Clear console and display ASCII art only if not headless and starting services.
Clear-Host

if (!$Headless -and $Action -eq "Start") {
    Write-Host $asciiArt
}

$serverJar = Join-Path $projectRoot "app/meshingress-server/target/meshingress.jar"
$repositoryJar = Join-Path $projectRoot "app/meshingress-repository/target/meshingress-repository.jar"
if ($Build -or -not (Test-Path -LiteralPath $serverJar) -or -not (Test-Path -LiteralPath $repositoryJar)) {
    Invoke-Build -ProjectRoot $projectRoot -SkipTests:$SkipTests
}

if (-not (Test-Path -LiteralPath $serverJar) -or -not (Test-Path -LiteralPath $repositoryJar)) {
    Write-Host-Exception -Exception ([System.IO.FileNotFoundException]::new(
        "Packaged service jars are missing after the build."
    ))
}

$java = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $java) {
    $java = Get-Command java -ErrorAction SilentlyContinue
}
if ($null -eq $java) {
    Write-Host-Exception -Exception ([System.IO.FileNotFoundException]::new(
        "Java was not found on PATH. Meshingress requires Java 25."
    ))
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

$existingState = Get-State -StatePath $statePath
if ($null -ne $existingState) {
    $running = @($existingState.services.server, $existingState.services.repository | Where-Object {
            $null -ne $_ -and $null -ne (Get-ManagedProcess -Service $_)
        })
    if ($running.Count -gt 0) {
        if (-not $ForceRestart) {
            Write-Host-Exception -Exception ([System.InvalidOperationException]::new(
                "Managed Meshingress services are already running. Use -Action Status, -Action Logs, or -ForceRestart."
            ))
        }
        Stop-ManagedServices -State $existingState -StatePath $statePath
    }
    else {
        Remove-Item -LiteralPath $statePath -Force
    }
}

Assert-PortAvailable -Port $effectiveServerPort -Purpose "Meshingress server"
Assert-PortAvailable -Port $effectiveRepositoryPort -Purpose "Meshingress repository"
if ($EnableJvmDebug) {
    Assert-PortAvailable -Port $ServerDebugPort -Purpose "Meshingress server JVM debugger"
    Assert-PortAvailable -Port $RepositoryDebugPort -Purpose "Meshingress repository JVM debugger"
}

$serverArguments = @()
if (-not [string]::IsNullOrWhiteSpace($ServerAddress)) {
    $serverArguments += "--server.address=$ServerAddress"
}
if ($ServerPort -ne 0) {
    $serverArguments += "--server.port=$ServerPort"
}

$repositoryArguments = @()
if (-not [string]::IsNullOrWhiteSpace($RepositoryAddress)) {
    $repositoryArguments += "--server.address=$RepositoryAddress"
}
if ($RepositoryPort -ne 0) {
    $repositoryArguments += "--server.port=$RepositoryPort"
}

if ($Debug) {
    $serverArguments += "--debug", "--logging.level.dev.mrk.meshingress=DEBUG"
    $repositoryArguments += "--debug", "--logging.level.dev.mrk.meshingress=DEBUG"
}

$serverJvmArguments = @()
$repositoryJvmArguments = @()
if ($EnableJvmDebug) {
    $serverJvmArguments += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$ServerDebugPort"
    $repositoryJvmArguments += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$RepositoryDebugPort"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$startedServices = @()
try {
    $repositoryService = Start-ServiceProcess `
        -Name "repository" `
        -JavaPath $java.Source `
        -JarPath $repositoryJar `
        -ProjectRoot $projectRoot `
        -LogDirectory $logDirectory `
        -Timestamp $timestamp `
        -ApplicationArguments $repositoryArguments `
        -JvmArguments $repositoryJvmArguments `
        -Hidden:$Headless
    $startedServices += $repositoryService
    Wait-ForService `
        -Service $repositoryService `
        -ProbeAddress (Get-ProbeAddress -Address $effectiveRepositoryAddress) `
        -Port $effectiveRepositoryPort `
        -TimeoutSeconds $StartupTimeoutSeconds

    $serverService = Start-ServiceProcess `
        -Name "server" `
        -JavaPath $java.Source `
        -JarPath $serverJar `
        -ProjectRoot $projectRoot `
        -LogDirectory $logDirectory `
        -Timestamp $timestamp `
        -ApplicationArguments $serverArguments `
        -JvmArguments $serverJvmArguments `
        -Hidden:$Headless
    $startedServices += $serverService

    $healthUrl = if ($SkipServerHealthCheck) { "" } else { "http://$(Get-ProbeAddress -Address $effectiveServerAddress):$effectiveServerPort/actuator/health" }
    Wait-ForService `
        -Service $serverService `
        -ProbeAddress (Get-ProbeAddress -Address $effectiveServerAddress) `
        -Port $effectiveServerPort `
        -TimeoutSeconds $StartupTimeoutSeconds `
        -HealthUrl $healthUrl
}
catch {
    foreach ($service in $startedServices) {
        $process = Get-ManagedProcess -Service $service
        if ($null -ne $process) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
        }
    }
    Write-Host-Exception -Exception $_.Exception
}

$state = [ordered]@{
    version = 1
    startedAt = (Get-Date).ToUniversalTime().ToString("o")
    services = [ordered]@{
        server = $serverService
        repository = $repositoryService
    }
}
$state | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $statePath -Encoding UTF8

Write-Host-Logged "Meshingress services are running."
Write-Host-Logged "Server health: http://$(Get-ProbeAddress -Address $effectiveServerAddress):$effectiveServerPort/actuator/health"
Write-Host-Logged "Logs: $logDirectory"
Write-Host-Logged "Stop: .\scripts\Meshingress.ps1 -Action Stop"

if ($Foreground) {
    Follow-Logs -State (Get-State -StatePath $statePath)
}
