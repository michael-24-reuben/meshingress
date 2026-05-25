# create-github-backup-branch.ps1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Ensure this is a Git repo
git rev-parse --is-inside-work-tree *> $null

# Format: backup-date-time
# Example: backup-20260525-143012
$timestamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
$branchName = "backup-$timestamp"

# Make sure origin exists
git remote get-url origin *> $null

# Create backup branch from current HEAD
git branch $branchName

# Push backup branch to GitHub
git push -u origin $branchName

Write-Host "Created and pushed backup branch: $branchName"