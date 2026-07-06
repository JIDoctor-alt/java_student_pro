# Install Git hooks for auto database export on commit
$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

git config core.hooksPath .githooks
Write-Host "[install-hooks] core.hooksPath = .githooks"
Write-Host "[install-hooks] done. Each commit will auto-export student_pro_snapshot.sql before staging."
