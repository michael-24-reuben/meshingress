param(
    [switch]$Probe
)

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$instructionFile = Join-Path $repoRoot ".agents\prompts\pas.instruct.md"
$logDir = Join-Path $repoRoot ".agents\automation\logs"
$lockFile = Join-Path $logDir "daily-pas-agent.lock"
$codexCommand = "C:\nvm4w\nodejs\codex.ps1"

function Write-RunLog {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss.fffzzz"
    Add-Content -LiteralPath $Path -Value "[$timestamp] $Message"
}

if (-not (Test-Path -LiteralPath $instructionFile)) {
    throw "Instruction file not found: $instructionFile"
}

if (-not (Test-Path -LiteralPath $codexCommand)) {
    throw "Codex command not found: $codexCommand"
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (Test-Path -LiteralPath $lockFile) {
    $lockAge = (Get-Date) - (Get-Item -LiteralPath $lockFile).LastWriteTime
    if ($lockAge.TotalHours -lt 8) {
        Write-Host "Daily PAS agent already appears to be running. Lock file: $lockFile"
        exit 0
    }
}

Set-Content -LiteralPath $lockFile -Value (Get-Date -Format "yyyy-MM-ddTHH:mm:sszzz")

$stampSuffix = if ($Probe) { "-probe" } else { "" }
$stamp = "$(Get-Date -Format "yyyyMMdd-HHmmss")$stampSuffix"
$stdoutLog = Join-Path $logDir "daily-pas-agent-$stamp.jsonl"
$stderrLog = Join-Path $logDir "daily-pas-agent-$stamp.err.log"
$runLog = Join-Path $logDir "daily-pas-agent-$stamp.run.log"
$finalMessage = Join-Path $logDir "daily-pas-agent-$stamp.final.md"
$prompt = if ($Probe) {
    "Reply OK only. Do not inspect files, run tools, or edit anything."
} else {
    Get-Content -LiteralPath $instructionFile -Raw
}

Push-Location $repoRoot
try {
    Write-RunLog -Path $runLog -Message "Starting daily PAS agent."
    Write-RunLog -Path $runLog -Message "Repo root: $repoRoot"
    Write-RunLog -Path $runLog -Message "Instruction file: $instructionFile"
    Write-RunLog -Path $runLog -Message "Probe mode: $Probe"
    Write-RunLog -Path $runLog -Message "Codex command: $codexCommand"
    Write-RunLog -Path $runLog -Message "User: $env:USERNAME"
    Write-RunLog -Path $runLog -Message "Computer: $env:COMPUTERNAME"
    Write-RunLog -Path $runLog -Message "PowerShell: $($PSVersionTable.PSVersion)"

    $codexVersion = & $codexCommand --version 2>&1
    Write-RunLog -Path $runLog -Message "Codex version: $codexVersion"

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $prompt | & $codexCommand `
            --ask-for-approval never `
            --sandbox danger-full-access `
            --cd $repoRoot `
            exec `
            --json `
            --output-last-message $finalMessage `
            - 1> $stdoutLog 2> $stderrLog

        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    Write-RunLog -Path $runLog -Message "Codex exit code: $exitCode"

    if ($exitCode -ne 0) {
        throw "Codex daily PAS agent failed with exit code $exitCode. See $stderrLog"
    }

    if (-not (Test-Path -LiteralPath $finalMessage)) {
        throw "Codex daily PAS agent exited successfully but did not write final message: $finalMessage"
    }

    $completed = Select-String -LiteralPath $stdoutLog -Pattern '"type":"turn.completed"' -Quiet
    if (-not $completed) {
        throw "Codex daily PAS agent did not record turn.completed in $stdoutLog"
    }

    Write-RunLog -Path $runLog -Message "Daily PAS agent completed successfully."
} catch {
    Write-RunLog -Path $runLog -Message "ERROR: $($_.Exception.Message)"

    if (Test-Path -LiteralPath $stdoutLog) {
        Write-RunLog -Path $runLog -Message "Last stdout lines:"
        Get-Content -LiteralPath $stdoutLog -Tail 20 | ForEach-Object {
            Write-RunLog -Path $runLog -Message $_
        }
    }

    if (Test-Path -LiteralPath $stderrLog) {
        Write-RunLog -Path $runLog -Message "Last stderr lines:"
        Get-Content -LiteralPath $stderrLog -Tail 20 | ForEach-Object {
            Write-RunLog -Path $runLog -Message $_
        }
    }

    throw
} finally {
    Pop-Location
    Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
}
