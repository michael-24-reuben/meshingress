Set-StrictMode -Version 2.0

function Join-MeshingressRepositoryArtifactUri {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Suffix,
    [hashtable] $Query
  )

  $base = $BaseUri.TrimEnd('/')
  $path = '/api/v1/artifact/{0}/{1}/{2}' -f `
    [uri]::EscapeDataString($GroupId), `
    [uri]::EscapeDataString($ArtifactId), `
    [uri]::EscapeDataString($Version)

  if (-not [string]::IsNullOrWhiteSpace($Suffix)) {
    $path = "$path/$($Suffix.TrimStart('/'))"
  }

  $queryPairs = @()
  if ($Query) {
    foreach ($key in $Query.Keys) {
      $value = $Query[$key]
      if ($null -eq $value) {
        continue
      }
      if ($value -is [array]) {
        foreach ($item in $value) {
          if ($null -ne $item -and "$item" -ne '') {
            $queryPairs += ('{0}={1}' -f [uri]::EscapeDataString($key), [uri]::EscapeDataString("$item"))
          }
        }
      } elseif ("$value" -ne '') {
        $queryPairs += ('{0}={1}' -f [uri]::EscapeDataString($key), [uri]::EscapeDataString("$value"))
      }
    }
  }

  if ($queryPairs.Count -gt 0) {
    return '{0}{1}?{2}' -f $base, $path, ($queryPairs -join '&')
  }
  return '{0}{1}' -f $base, $path
}

function New-MeshingressRepositoryHeaders {
  param(
    [Parameter(Mandatory = $true)] [string] $Role,
    [string] $Actor = 'manual-api',
    [string] $RequestId,
    [string] $Authorization
  )

  $headers = @{
    'X-Repository-Role' = $Role
    'X-Repository-Actor' = $Actor
  }

  if (-not [string]::IsNullOrWhiteSpace($RequestId)) {
    $headers['X-Request-Id'] = $RequestId
  }
  if (-not [string]::IsNullOrWhiteSpace($Authorization)) {
    $headers['Authorization'] = $Authorization
  }

  return $headers
}

function Invoke-MeshingressRepositoryJson {
  param(
    [Parameter(Mandatory = $true)] [ValidateSet('Get', 'Post')] [string] $Method,
    [Parameter(Mandatory = $true)] [string] $Uri,
    [Parameter(Mandatory = $true)] [hashtable] $Headers,
    [object] $Body
  )

  $parameters = @{
    Method = $Method
    Uri = $Uri
    Headers = $Headers
  }

  if ($PSBoundParameters.ContainsKey('Body')) {
    $parameters['ContentType'] = 'application/json'
    $parameters['Body'] = ($Body | ConvertTo-Json -Depth 20)
  }

  try {
    Invoke-RestMethod @parameters
  } catch {
    $statusCode = $null
    $responseBody = $null

    if ($_.Exception.Response) {
      try {
        $statusCode = [int] $_.Exception.Response.StatusCode
      } catch {
        $statusCode = $_.Exception.Response.StatusCode
      }

      try {
        $stream = $_.Exception.Response.GetResponseStream()
        if ($stream) {
          $reader = New-Object System.IO.StreamReader($stream)
          $responseBody = $reader.ReadToEnd()
          $reader.Close()
        }
      } catch {
        $responseBody = $null
      }
    }

    if ([string]::IsNullOrWhiteSpace($responseBody) -and $_.ErrorDetails -and $_.ErrorDetails.Message) {
      $responseBody = $_.ErrorDetails.Message
    }

    if ([string]::IsNullOrWhiteSpace($responseBody)) {
      $responseBody = $_.Exception.Message
    }

    throw "Repository request failed with HTTP $statusCode. Response body: $responseBody"
  }
}

function Send-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [Parameter(Mandatory = $true)] [string] $JarPath,
    [string] $Type = 'TOOL_MODULE',
    [string] $Packaging = 'jar',
    [string[]] $RequestedScopes = @(),
    [string] $Role = 'uploader',
    [string] $Actor = 'manual-upload',
    [string] $RequestId = "upload-$ArtifactId-$Version"
  )

  if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar not found: $JarPath"
  }

  $query = @{
    type = $Type
    packaging = $Packaging
    requestedScopes = $RequestedScopes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Query $query

  Write-Host "POST $uri"
  $arguments = @(
    '--silent',
    '--show-error',
    '--write-out', "`n__HTTP_STATUS__:%{http_code}",
    '-X', 'POST',
    '-H', "X-Repository-Role: $Role",
    '-H', "X-Repository-Actor: $Actor",
    '-H', "X-Request-Id: $RequestId",
    '-F', "file=@$JarPath",
    $uri
  )

  $output = & curl.exe @arguments
  if ($LASTEXITCODE -ne 0) {
    throw "curl.exe failed with exit code $LASTEXITCODE"
  }
  $text = ($output -join "`n")
  $match = [regex]::Match($text, '(?s)^(.*)\r?\n__HTTP_STATUS__:(\d{3})\s*$')
  if (-not $match.Success) {
    throw "curl.exe response did not include an HTTP status marker: $text"
  }
  $bodyText = $match.Groups[1].Value.Trim()
  $statusCode = [int] $match.Groups[2].Value
  if ($statusCode -ge 400) {
    throw "Repository upload failed with HTTP $statusCode. Response body: $bodyText"
  }
  if ([string]::IsNullOrWhiteSpace($bodyText)) {
    return $null
  }
  $bodyText | ConvertFrom-Json
}

function Get-MeshingressArtifactMetadata {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-read'
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'metadata'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-MeshingressRepositoryJson -Method Get -Uri $uri -Headers $headers
}

function Save-MeshingressArtifactFile {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [Parameter(Mandatory = $true)] [string] $OutFile,
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-download'
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'file'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-WebRequest -Method Get -Uri $uri -Headers $headers -OutFile $OutFile
}

function Save-MeshingressArtifactResource {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [Parameter(Mandatory = $true)] [ValidateSet('application.properties', 'README.md')] [string] $ResourceName,
    [Parameter(Mandatory = $true)] [string] $OutFile,
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-download'
  )

  $resource = 'resources/{0}' -f [uri]::EscapeDataString($ResourceName)
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix $resource
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-WebRequest -Method Get -Uri $uri -Headers $headers -OutFile $OutFile
}

function Start-MeshingressArtifactAssessment {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-assess',
    [string] $RequestId = "assess-$ArtifactId-$Version"
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'assess'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers
}

function Get-MeshingressArtifactAssessment {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-read'
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'assessment'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-MeshingressRepositoryJson -Method Get -Uri $uri -Headers $headers
}

function Get-MeshingressPendingReviews {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-review'
  )

  $uri = "$($BaseUri.TrimEnd('/'))/artifact/reviews/pending"
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-MeshingressRepositoryJson -Method Get -Uri $uri -Headers $headers
}

function Approve-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string[]] $ApprovedScopes = @(),
    [object[]] $DeniedScopes = @(),
    [ValidateSet('APPROVED_TRUSTED', 'APPROVED_LIMITED')] [string] $TrustStatus = 'APPROVED_TRUSTED',
    [string] $Reviewer = 'manual-review',
    [string] $Notes = 'Approved from repository API PowerShell sample.',
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-review',
    [string] $RequestId = "approve-$ArtifactId-$Version"
  )

  $body = @{
    approvedScopes = $ApprovedScopes
    deniedScopes = $DeniedScopes
    trustStatus = $TrustStatus
    reviewer = $Reviewer
    notes = $Notes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'approve'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers -Body $body
}

function Reject-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [object[]] $DeniedScopes = @(),
    [string] $Reviewer = 'manual-review',
    [string] $Notes = 'Rejected from repository API PowerShell sample.',
    [string] $Role = 'reviewer',
    [string] $Actor = 'manual-review',
    [string] $RequestId = "reject-$ArtifactId-$Version"
  )

  $body = @{
    deniedScopes = $DeniedScopes
    reviewer = $Reviewer
    notes = $Notes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'reject'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers -Body $body
}

function Publish-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Role = 'publisher',
    [string] $Actor = 'manual-publish',
    [string] $RequestId = "publish-$ArtifactId-$Version"
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'publish'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers
}

function Get-MeshingressArtifactPublication {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Role = 'publisher',
    [string] $Actor = 'manual-read'
  )

  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'publication'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor
  Invoke-MeshingressRepositoryJson -Method Get -Uri $uri -Headers $headers
}

function Revoke-MeshingressArtifactPublication {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Reviewer = 'manual-publish',
    [string] $Notes = 'Revoked from repository API PowerShell sample.',
    [string] $Role = 'publisher',
    [string] $Actor = 'manual-publish',
    [string] $RequestId = "revoke-$ArtifactId-$Version"
  )

  $body = @{
    reviewer = $Reviewer
    notes = $Notes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'revoke'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers -Body $body
}

function Remove-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Reviewer = 'manual-admin',
    [string] $Notes = 'Soft deleted from repository API PowerShell sample.',
    [string] $Role = 'admin',
    [string] $Actor = 'manual-admin',
    [string] $RequestId = "delete-$ArtifactId-$Version"
  )

  $body = @{
    reviewer = $Reviewer
    notes = $Notes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'delete'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers -Body $body
}

function Restore-MeshingressArtifact {
  param(
    [string] $BaseUri = 'http://localhost:8080',
    [Parameter(Mandatory = $true)] [string] $GroupId,
    [Parameter(Mandatory = $true)] [string] $ArtifactId,
    [Parameter(Mandatory = $true)] [string] $Version,
    [string] $Reviewer = 'manual-admin',
    [string] $Notes = 'Restored from repository API PowerShell sample.',
    [string] $Role = 'admin',
    [string] $Actor = 'manual-admin',
    [string] $RequestId = "restore-$ArtifactId-$Version"
  )

  $body = @{
    reviewer = $Reviewer
    notes = $Notes
  }
  $uri = Join-MeshingressRepositoryArtifactUri -BaseUri $BaseUri -GroupId $GroupId -ArtifactId $ArtifactId -Version $Version -Suffix 'restore'
  $headers = New-MeshingressRepositoryHeaders -Role $Role -Actor $Actor -RequestId $RequestId
  Invoke-MeshingressRepositoryJson -Method Post -Uri $uri -Headers $headers -Body $body
}
