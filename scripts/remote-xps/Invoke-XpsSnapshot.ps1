[CmdletBinding()]
param(
    [string] $SourceRoot,
    [string] $Remote = 'alphasunny@alphasunny-xps-8700.tail93ea23.ts.net'
)

$ErrorActionPreference = 'Stop'
if (-not $SourceRoot) { $SourceRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot) }

function Convert-XpsGlobToRegex([string] $Pattern) {
    $result = [System.Text.StringBuilder]::new()
    for ($index = 0; $index -lt $Pattern.Length; $index++) {
        $character = $Pattern[$index]
        if ($character -eq '*' -and $index + 1 -lt $Pattern.Length -and $Pattern[$index + 1] -eq '*') {
            [void] $result.Append('.*'); $index++
        } elseif ($character -eq '*') {
            [void] $result.Append('[^/]*')
        } elseif ($character -eq '?') {
            [void] $result.Append('[^/]')
        } else {
            [void] $result.Append([regex]::Escape([string] $character))
        }
    }
    return $result.ToString()
}

function Test-XpsRuleMatch([string] $RelativePath, [pscustomobject] $Rule) {
    $candidates = @($RelativePath)
    if ($Rule.DirectoryOnly) {
        $parts = $RelativePath.Split('/')
        $candidates += for ($length = 1; $length -lt $parts.Length; $length++) { $parts[0..($length - 1)] -join '/' }
    }
    foreach ($candidate in $candidates) {
        $expression = if ($Rule.HasSlash) { '^' + $Rule.Regex + '$' } else { '(^|.*/)' + $Rule.Regex + '$' }
        if ($candidate -match $expression) { return $true }
    }
    return $false
}

$source = (Resolve-Path -LiteralPath $SourceRoot).Path
$policy = Join-Path $source '.xpsignore'
if (-not (Test-Path -LiteralPath $policy)) { $policy = Join-Path $source '.gitignore' }
if (-not (Test-Path -LiteralPath $policy)) { throw 'Neither .xpsignore nor .gitignore exists.' }

$rules = foreach ($line in Get-Content -LiteralPath $policy) {
    $value = $line.Trim()
    if (-not $value -or $value.StartsWith('#')) { continue }
    $negated = $value.StartsWith('!')
    if ($negated) { $value = $value.Substring(1) }
    $anchored = $value.StartsWith('/')
    if ($anchored) { $value = $value.Substring(1) }
    $directoryOnly = $value.EndsWith('/')
    if ($directoryOnly) { $value = $value.TrimEnd('/') }
    if ($value) {
        [pscustomobject]@{
            Negated = $negated
            DirectoryOnly = $directoryOnly
            HasSlash = $anchored -or $value.Contains('/')
            Regex = Convert-XpsGlobToRegex $value
        }
    }
}

function Test-XpsIgnored([string] $RelativePath) {
    $ignored = $false
    foreach ($rule in $rules) {
        if (Test-XpsRuleMatch $RelativePath $rule) { $ignored = -not $rule.Negated }
    }
    return $ignored
}

$directories = [System.Collections.Generic.List[string]]::new()
$files = [System.Collections.Generic.List[string]]::new()
function Collect-XpsEntries([System.IO.DirectoryInfo] $Directory, [string] $Prefix) {
    foreach ($entry in $Directory.GetFileSystemInfos()) {
        if (-not $Prefix -and $entry.Name -eq '.git') { continue }
        if (($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) { continue }
        $relative = if ($Prefix) { "$Prefix/$($entry.Name)" } else { $entry.Name }
        if ($entry -is [System.IO.DirectoryInfo]) {
            if (-not (Test-XpsIgnored $relative)) { [void] $directories.Add($relative) }
            Collect-XpsEntries $entry $relative
        } elseif (-not (Test-XpsIgnored $relative)) {
            [void] $files.Add($relative)
        }
    }
}
Collect-XpsEntries ([System.IO.DirectoryInfo]::new($source)) ''

$archivePath = Join-Path ([System.IO.Path]::GetTempPath()) ("meshingress-xps-snapshot-" + [guid]::NewGuid().ToString('N') + '.zip')
try {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open($archivePath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($directory in $directories) { [void] $archive.CreateEntry($directory + '/') }
        foreach ($file in $files) {
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $source ($file -replace '/', '\\')), $file, [System.IO.Compression.CompressionLevel]::Fastest) | Out-Null
        }
    } finally { $archive.Dispose() }
    & scp $archivePath "${Remote}:/tmp/meshingress-xps-snapshot.zip"
    if ($LASTEXITCODE -ne 0) { throw 'Snapshot archive upload failed.' }
    & scp (Join-Path $PSScriptRoot 'xps-snapshot-remote.sh') "${Remote}:/tmp/xps-snapshot-remote.sh"
    if ($LASTEXITCODE -ne 0) { throw 'Snapshot update script upload failed.' }
    & ssh $Remote 'bash /tmp/xps-snapshot-remote.sh'
    if ($LASTEXITCODE -ne 0) { throw 'XPS snapshot commit failed.' }
    Write-Host "XPS readable snapshot updated from $($files.Count) files using $(Split-Path -Leaf $policy)."
} finally {
    Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
}
