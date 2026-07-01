# Import student_pro database snapshot
# Usage: .\scripts\db-import.ps1

param(
    [string]$User = $env:MYSQL_USER,
    [string]$Password = $env:MYSQL_PASSWORD,
    [string]$Database = $env:MYSQL_DATABASE,
    [string]$Container = $env:MYSQL_CONTAINER
)

$ErrorActionPreference = "Stop"

if (-not $User) { $User = "root" }
if (-not $Password) { $Password = "root" }
if (-not $Database) { $Database = "student_pro" }
if (-not $Container) { $Container = "mysql-container" }

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Snapshot = Join-Path $RepoRoot "server\server\src\main\resources\sql\data\student_pro_snapshot.sql"

if (-not (Test-Path $Snapshot)) {
    Write-Error "[db-import] snapshot not found: $Snapshot"
}

$running = docker ps --filter "name=^/${Container}$" --format "{{.Names}}" 2>$null
if ($running) {
    Get-Content $Snapshot -Raw -Encoding UTF8 | docker exec -i $Container mysql -u$User -p$Password $Database
    Write-Host "[db-import] restored via docker container: $Container"
    exit 0
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysql) {
    Get-Content $Snapshot -Raw -Encoding UTF8 | & mysql -h127.0.0.1 -u$User "-p$Password" $Database
    Write-Host "[db-import] restored via local mysql client"
    exit 0
}

Write-Error "[db-import] failed: no docker container or local mysql client"
