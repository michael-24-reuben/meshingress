param(
  [string] $BaseUri = 'http://localhost:8080',
  [string] $GroupId = 'dev.mrk.toolspace',
  [string] $ArtifactId = 'powershell-cli',
  [string] $Version = '0.0.1-SNAPSHOT',
  [string] $JarPath = 'J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\temp\packages\powershell-cli-0.0.1-SNAPSHOT.jar',
  [string] $OutDir = 'J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\temp\repository-api-sample',
  [switch] $RunLifecycle,
  [switch] $RunDestructive = $true,
  [switch] $RemoveFilesystemOrphan
)

. "$PSScriptRoot\MeshingressRepositoryApi.ps1"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$common = @{
  BaseUri = $BaseUri
  GroupId = $GroupId
  ArtifactId = $ArtifactId
  Version = $Version
}

Write-Host "Using artifact $GroupId/$ArtifactId/$Version"

if ($RunDestructive) {
  Write-Host '1. Delete artifact'
  try {
    Remove-MeshingressArtifact @common |
      ConvertTo-Json -Depth 20
  } catch {
    $message = $_.Exception.Message
    Write-Warning $message

    if ($message -notmatch 'artifact not found') {
      throw
    }

    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..\..'))
    $groupPath = ($GroupId -split '\.') -join [System.IO.Path]::DirectorySeparatorChar
    $artifactDirectory = Join-Path $repoRoot ('repository\artifacts\{0}\{1}\{2}' -f $groupPath, $ArtifactId, $Version)

    if (-not (Test-Path -LiteralPath $artifactDirectory)) {
      Write-Warning "No filesystem artifact directory found at $artifactDirectory"
      return
    }

    Write-Warning "The API cannot delete this coordinate because no metadata record exists."
    Write-Warning "A filesystem artifact directory exists at $artifactDirectory"

    if (-not $RemoveFilesystemOrphan) {
      Write-Warning 'Re-run with -RemoveFilesystemOrphan to archive and remove that stale local directory.'
      return
    }

    $repoArtifactsRoot = Join-Path $repoRoot 'repository\artifacts'
    $targetResolved = (Resolve-Path -LiteralPath $artifactDirectory).Path
    $artifactsRootResolved = (Resolve-Path -LiteralPath $repoArtifactsRoot).Path
    if (-not $targetResolved.StartsWith($artifactsRootResolved, [System.StringComparison]::OrdinalIgnoreCase)) {
      throw "Refusing to remove path outside repository artifacts root: $targetResolved"
    }

    $backupDirectory = Join-Path $OutDir 'orphan-backups'
    New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $archivePath = Join-Path $backupDirectory "$ArtifactId-$Version-orphan-$timestamp.zip"

    Compress-Archive -LiteralPath $artifactDirectory -DestinationPath $archivePath -Force
    Remove-Item -LiteralPath $artifactDirectory -Recurse -Force

    Write-Host "Archived orphan to $archivePath"
    Write-Host "Removed orphan directory $artifactDirectory"
  }
}

Write-Host "Downloaded files are under $OutDir"
