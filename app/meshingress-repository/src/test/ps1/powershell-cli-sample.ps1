param(
  [string] $BaseUri = 'http://localhost:8080',
  [string] $GroupId = 'dev.mrk.toolspace',
  [string] $ArtifactId = 'powershell-cli',
  [string] $Version = '0.0.1-SNAPSHOT',
  [string] $JarPath = 'J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\temp\packages\powershell-cli-0.0.1-SNAPSHOT.jar',
  [string] $OutDir = 'J:\Users\jbeas\Repositories\Dev.java-2026\artifacts\meshingress\temp\repository-api-sample',
  [switch] $RunLifecycle = $true,
  [switch] $RunDestructive = $false
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

Write-Host '1. Upload jar'
Send-MeshingressArtifact @common -JarPath $JarPath -RequestedScopes @('SHELL_EXECUTE', 'FILES_READ') |
  ConvertTo-Json -Depth 20

Write-Host '2. Read metadata'
Get-MeshingressArtifactMetadata @common |
  ConvertTo-Json -Depth 20

Write-Host '3. Download artifact file'
Save-MeshingressArtifactFile @common -OutFile (Join-Path $OutDir "$ArtifactId-$Version.jar")

Write-Host '4. Assess artifact'
Start-MeshingressArtifactAssessment @common |
  ConvertTo-Json -Depth 20

Write-Host '5. Read assessment'
Get-MeshingressArtifactAssessment @common |
  ConvertTo-Json -Depth 20

Write-Host '6. List pending reviews'
Get-MeshingressPendingReviews -BaseUri $BaseUri |
  ConvertTo-Json -Depth 20

Write-Host '7. Try downloading exported resources'
foreach ($resourceName in @('application.yaml', 'README.md')) {
  try {
    Save-MeshingressArtifactResource @common -ResourceName $resourceName -OutFile (Join-Path $OutDir $resourceName)
  } catch {
    Write-Warning "Resource not available: $resourceName. $($_.Exception.Message)"
  }
}

if ($RunLifecycle) {
  Write-Host '8. Read metadata for review scopes'
  $metadata = Get-MeshingressArtifactMetadata @common
  $approvedScopes = @($metadata.scopes.inferredScopes)
  $currentApprovedScopes = @($metadata.scopes.approvedScopes)
  $currentDeniedScopes = @($metadata.scopes.deniedScopes | ForEach-Object { $_.scope })
  $unreviewedScopes = @($approvedScopes | Where-Object {
      $currentApprovedScopes -notcontains $_ -and $currentDeniedScopes -notcontains $_
    })

  if ($approvedScopes.Count -eq 0) {
    Write-Warning 'No inferred scopes were found; approval will rely on repository default behavior.'
  } else {
    Write-Host "Approving inferred scopes: $($approvedScopes -join ', ')"
  }

  if ($metadata.trustStatus -eq 'REVIEW_PENDING') {
    Write-Host '9. Approve artifact'
    Approve-MeshingressArtifact @common -ApprovedScopes $approvedScopes -TrustStatus 'APPROVED_TRUSTED' |
      ConvertTo-Json -Depth 20
  } elseif ($unreviewedScopes.Count -gt 0) {
    throw "Artifact is $($metadata.trustStatus), so it cannot be re-approved through this API. Unreviewed inferred scopes remain: $($unreviewedScopes -join ', '). Use a fresh version or delete/reupload this artifact."
  } else {
    Write-Host "9. Artifact is already $($metadata.trustStatus) and all inferred scopes are reviewed; skipping approval."
  }

  Write-Host '10. Publish artifact'
  Publish-MeshingressArtifact @common |
    ConvertTo-Json -Depth 20

  Write-Host '11. Read publication'
  Get-MeshingressArtifactPublication @common |
    ConvertTo-Json -Depth 20

  Write-Host '12. Revoke publication'
  Revoke-MeshingressArtifactPublication @common |
    ConvertTo-Json -Depth 20
}

if ($RunDestructive) {
  Write-Host '13. Soft-delete artifact'
  Remove-MeshingressArtifact @common |
    ConvertTo-Json -Depth 20

  Write-Host '14. Restore artifact'
  Restore-MeshingressArtifact @common |
    ConvertTo-Json -Depth 20
}

Write-Host "Downloaded files are under $OutDir"
