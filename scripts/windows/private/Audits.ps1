param(
    # `OpenAPI` -> Open the API documentation.
    # `README` -> Open the README file.
    # `LICENSE` -> Open the LICENSE file.
    # `CHANGELOG` -> Open the CHANGELOG file.
    [ValidateSet("OpenAPI", "README", "LICENSE", "CHANGELOG")]
    [string]$Docs = "README",

    [string]$ServerAddress = "",

    [ValidateRange(0, 65535)]
    [int]$ServerPort = 0
)

function Test-MeshingressRootPom {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$PomPath
    )

    if (-not (Test-Path -LiteralPath $PomPath -PathType Leaf)) {
        return $false
    }

    try {
        [xml]$pom = Get-Content `
            -LiteralPath $PomPath `
            -Raw `
            -ErrorAction Stop

        $namespaceManager = [System.Xml.XmlNamespaceManager]::new(
            $pom.NameTable
        )

        $namespaceManager.AddNamespace(
            "m",
            "http://maven.apache.org/POM/4.0.0"
        )

        # These paths intentionally select the project's coordinates,
        # not the coordinates under <parent>.
        $groupIdNode = $pom.SelectSingleNode(
            "/m:project/m:groupId",
            $namespaceManager
        )

        $artifactIdNode = $pom.SelectSingleNode(
            "/m:project/m:artifactId",
            $namespaceManager
        )

        $packagingNode = $pom.SelectSingleNode(
            "/m:project/m:packaging",
            $namespaceManager
        )

        if ($null -eq $groupIdNode -or $null -eq $artifactIdNode) {
            return $false
        }

        $groupIdMatches =
            $groupIdNode.InnerText.Trim() -ceq "dev.mrk.meshingress"

        $artifactIdMatches =
            $artifactIdNode.InnerText.Trim() -ceq "meshingress"

        # Optional but useful for distinguishing the aggregator/root POM.
        $packagingMatches =
            $null -ne $packagingNode -and
            $packagingNode.InnerText.Trim() -ceq "pom"

        return (
            $groupIdMatches -and
            $artifactIdMatches -and
            $packagingMatches
        )
    }
    catch {
        Write-Verbose "Rejected invalid POM '$PomPath': $($_.Exception.Message)"
        return $false
    }
}

function Find-ProjectRoot {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$StartDirectory
    )

    $resolvedStart = Resolve-Path `
        -LiteralPath $StartDirectory `
        -ErrorAction Stop

    $directoryPath = $resolvedStart.Path

    while (-not [string]::IsNullOrWhiteSpace($directoryPath)) {
        $pomPath = Join-Path $directoryPath "pom.xml"

        if (Test-MeshingressRootPom -PomPath $pomPath) {
            return $directoryPath
        }

        Write-Verbose "Rejected project-root candidate: $directoryPath"

        $parent = Split-Path -Path $directoryPath -Parent

        if (
            [string]::IsNullOrWhiteSpace($parent) -or
            $parent -eq $directoryPath
        ) {
            break
        }

        $directoryPath = $parent
    }

    throw [System.IO.DirectoryNotFoundException]::new(
        "Could not find a Meshingress root POM containing " +
        "'dev.mrk.meshingress:meshingress' from '$StartDirectory'."
    )
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

$projectRootCandidate = Join-Path $PSScriptRoot "..\..\.."

$projectRoot = Find-ProjectRoot `
    -StartDirectory $projectRootCandidate

switch ($Docs) {
    "OpenAPI" {
        $serverPropertiesPath = Join-Path $projectRoot "app\meshingress-server\src\main\resources\application.properties"
        if ([string]::IsNullOrWhiteSpace($ServerAddress)) {
            $ServerAddress = Get-PropertiesValue `
                -Path $serverPropertiesPath `
                -Name "server.address" `
                -DefaultValue "127.0.0.1"
        }
        if ($ServerPort -eq 0) {
            $ServerPort = [int](Get-PropertiesValue `
                -Path $serverPropertiesPath `
                -Name "server.port" `
                -DefaultValue "4737")
        }

        $openApiPath = Join-Path $projectRoot "docs\meshingress-openapi.yaml"
        Write-Host "Downloading OpenAPI document..."
        Invoke-WebRequest `
          "http://${ServerAddress}:${ServerPort}/v3/api-docs.yaml" `
          -OutFile $openApiPath
        Write-Host "OpenAPI document saved to: $openApiPath"
        Get-Content -LiteralPath $openApiPath
        #Invoke-Item -LiteralPath $openApiPath
    }
    "README" {
        # Open the README file
        Write-Host "Opening README file..."
        $readmePath = Join-Path $projectRoot "README.md"
        if (Test-Path -LiteralPath $readmePath -PathType Leaf) {
            Get-Content -LiteralPath $readmePath
        } else {
            Write-Host "README file not found at: $readmePath"
        }
    }
    "LICENSE" {
        # Open the LICENSE file
        Write-Host "Opening LICENSE file..."
        $licensePath = Join-Path $projectRoot "LICENSE"
        if (Test-Path -LiteralPath $licensePath -PathType Leaf) {
            Get-Content -LiteralPath $licensePath
        } else {
            Write-Host "LICENSE file not found at: $licensePath"
        }
    }
    "CHANGELOG" {
        # Open the CHANGELOG file
        Write-Host "Opening CHANGELOG file..."
        $changelogPath = Join-Path $projectRoot "CHANGELOG.md"
        if (Test-Path -LiteralPath $changelogPath -PathType Leaf) {
            Get-Content -LiteralPath $changelogPath
        } else {
            Write-Host "CHANGELOG file not found at: $changelogPath"
        }
    }
}
