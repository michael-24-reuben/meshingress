<#
#▮# Mockito Agent Download Script
#▮
#▮## Purpose
#▮
#▮This PowerShell script downloads the Mockito Core JAR and places it at:
#▮
#▮```txt
#▮app/meshingress-repository/target/agents/mockito-core.jar
#▮```
#▮
#▮The downloaded JAR is intended to be used as a Java agent during Maven test execution.
#▮
#▮## Why This Script Exists
#▮
#▮Recent JDK versions warn when Mockito dynamically attaches its inline mock-maker agent at runtime. The warning indicates that dynamic agent
#▮ loading will be restricted or disabled by default in future JDK releases.
#▮
#▮Instead of allowing Mockito to self-attach during tests, the preferred approach is to explicitly provide Mockito as a JVM agent using:
#▮
#▮```txt
#▮-javaagent:path/to/mockito-core.jar
#▮```
#▮
#▮This script prepares that agent JAR in a predictable local path so the Maven Surefire or Failsafe plugin can reference it directly.
#▮
#▮## What the Script Accomplishes
#▮
#▮The script performs the following actions:
#▮
#▮1. Selects a Mockito Core version to download.
#▮2. Builds the Maven Central download URL for that version.
#▮3. Creates the target agent directory if it does not already exist.
#▮4. Downloads the Mockito Core JAR from Maven Central.
#▮5. Saves it as:
#▮
#▮```txt
#▮app/meshingress-repository/target/agents/mockito-core.jar
#▮```
#▮
#▮6. Verifies that the file exists after download.
#▮7. Verifies that the downloaded file is not empty.
#▮8. Prints a success message with the downloaded file size.
#▮
#▮## Why the JAR Is Renamed
#▮
#▮The original Maven Central artifact name includes the version, for example:
#▮
#▮```txt
#▮mockito-core-5.23.0.jar
#▮```
#▮
#▮The script saves it as:
#▮
#▮```txt
#▮mockito-core.jar
#▮```
#▮
#▮This keeps the Maven test configuration stable. The `argLine` does not need to change every time the Mockito version changes.
#▮
#▮## Intended Maven Usage
#▮
#▮After the script runs, Maven Surefire can reference the agent like this:
#▮
#▮```xml
#▮<argLine>
#▮    -javaagent:${project.basedir}/target/agents/mockito-core.jar
#▮</argLine>
#▮```
#▮
#▮For the `app/meshingress-repository` module, this resolves to:
#▮
#▮```txt
#▮app/meshingress-repository/target/agents/mockito-core.jar
#▮```
#▮
#▮## Why This Is Better Than Dynamic Self-Attach
#▮
#▮Explicitly loading Mockito as a Java agent avoids the JDK warning about dynamic agent loading. It also makes the test JVM configuration more future-compatible
#▮ because Mockito no longer needs to attach itself after the JVM has already started.
#▮
#▮## Scope
#▮
#▮This script is only meant for test execution support. It should not be used by the Meshingress application runtime.
#▮
#▮The downloaded JAR belongs under `target/`, meaning it is a generated build artifact and should normally not be committed to source control.
#▮
#▮## Expected Outcome
#▮
#▮After running the script, test execution can use Mockito’s inline mocking support without triggering the self-attachment warning, provided the Maven test plugin is
#▮ configured to use the downloaded JAR as a `-javaagent`.
#▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮▮
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot,

    [string]$Version = "5.23.0",

    [string]$OutputPath = "app/meshingress-repository/target/agents/mockito-core.jar"
)

$ErrorActionPreference = "Stop"

$repoBase = "https://repo1.maven.org/maven2"
$groupPath = "org/mockito/mockito-core"
$fileName = "mockito-core-$Version.jar"
$url = "$repoBase/$groupPath/$Version/$fileName"

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$resolvedOutputPath = Join-Path $resolvedProjectRoot $OutputPath
$outputDir = Split-Path -Parent $resolvedOutputPath

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Write-Host "Downloading Mockito Core $Version..."
Write-Host "Source: $url"
Write-Host "Target: $resolvedOutputPath"

Invoke-WebRequest `
    -Uri $url `
    -OutFile $resolvedOutputPath `
    -UseBasicParsing

if (-not (Test-Path $resolvedOutputPath)) {
    throw "Download failed: file was not created."
}

$size = (Get-Item $resolvedOutputPath).Length

if ($size -le 0) {
    throw "Download failed: downloaded file is empty."
}

Write-Host "Downloaded mockito-core.jar successfully."
Write-Host "Size: $size bytes"