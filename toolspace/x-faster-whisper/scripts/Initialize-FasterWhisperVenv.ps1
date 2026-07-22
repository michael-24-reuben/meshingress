#requires -Version 7.4
[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param(
    [string] $Python = 'python',
    [switch] $Recreate
)

$ErrorActionPreference = 'Stop'
$moduleRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$vendorRoot = Join-Path $moduleRoot 'src\main\resources\vendor\faster-whisper'
$venvRoot = Join-Path $moduleRoot '.venv'
$venvPython = Join-Path $venvRoot 'Scripts\python.exe'
$stagingRoot = Join-Path $moduleRoot '.provisioning\staging\faster-whisper'

if (-not (Test-Path -LiteralPath $vendorRoot -PathType Container)) {
    throw "Vendored faster-whisper source is unavailable: $vendorRoot"
}

if ($WhatIfPreference) {
    [pscustomobject]@{
        vendorRoot = $vendorRoot
        stagingRoot = $stagingRoot
        virtualEnvironment = $venvRoot
        mutationBoundary = 'Only .venv and .provisioning are changed; vendor source remains read-only.'
    }
    return
}

if ($Recreate -and (Test-Path -LiteralPath $venvRoot)) {
    if ($PSCmdlet.ShouldProcess($venvRoot, 'Remove existing local virtual environment')) {
        Remove-Item -LiteralPath $venvRoot -Recurse -Force
    }
}

if ($PSCmdlet.ShouldProcess($stagingRoot, 'Create disposable copy of vendored faster-whisper source')) {
    Remove-Item -LiteralPath $stagingRoot -Recurse -Force -ErrorAction Ignore
    New-Item -ItemType Directory -Force $stagingRoot | Out-Null
    & robocopy $vendorRoot $stagingRoot /E /XD .git __pycache__ .pytest_cache .mypy_cache /NFL /NDL /NJH /NJS /NP
    if ($LASTEXITCODE -gt 7) { throw "robocopy failed with exit code $LASTEXITCODE" }
}

try {
    if (-not (Test-Path -LiteralPath $venvPython -PathType Leaf)) {
        if ($PSCmdlet.ShouldProcess($venvRoot, 'Create local virtual environment')) {
            & $Python -m venv $venvRoot
            if ($LASTEXITCODE -ne 0) { throw "Virtual environment creation failed with exit code $LASTEXITCODE" }
        }
    }
    if ($PSCmdlet.ShouldProcess($venvRoot, 'Resolve and install faster-whisper runtime dependencies')) {
        & $venvPython -m pip install --upgrade pip
        if ($LASTEXITCODE -ne 0) { throw "pip upgrade failed with exit code $LASTEXITCODE" }
        & $venvPython -m pip install --no-cache-dir $stagingRoot
        if ($LASTEXITCODE -ne 0) { throw "faster-whisper dependency installation failed with exit code $LASTEXITCODE" }
        & $venvPython (Join-Path $moduleRoot 'src\main\resources\python\faster_whisper_bridge.py') --verify
        if ($LASTEXITCODE -ne 0) { throw "faster-whisper import verification failed with exit code $LASTEXITCODE" }
    }
} finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}

[pscustomobject]@{
    status = 'READY'
    pythonExecutable = $venvPython
    vendorMutation = 'none'
    modelProvisioning = 'not performed; ordinary tool calls remain local-files-only'
}
