# Export student_pro database snapshot for version control
# Usage: .\scripts\db-export.ps1

param(
    [string]$Host = $env:MYSQL_HOST,
    [string]$Port = $env:MYSQL_PORT,
    [string]$User = $env:MYSQL_USER,
    [string]$Password = $env:MYSQL_PASSWORD,
    [string]$Database = $env:MYSQL_DATABASE,
    [string]$Container = $env:MYSQL_CONTAINER
)

$ErrorActionPreference = "Stop"

if (-not $Host) { $Host = "127.0.0.1" }
if (-not $Port) { $Port = "3306" }
if (-not $User) { $User = "root" }
if (-not $Password) { $Password = "root" }
if (-not $Database) { $Database = "student_pro" }
if (-not $Container) { $Container = "mysql-container" }

$RepoRoot = Split-Path -Parent $PSScriptRoot
$OutDir = Join-Path $RepoRoot "server\server\src\main\resources\sql\data"
$OutFile = Join-Path $OutDir "student_pro_snapshot.sql"
$TempFile = Join-Path $env:TEMP "student_pro_snapshot_$([Guid]::NewGuid().ToString('N')).sql"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$Header = @"
-- student_pro database snapshot
-- exported at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')
-- database: $Database
-- NOTE: contains hashed passwords and chat history; do not publish production data to public repos.

"@

Set-Content -Path $TempFile -Value $Header -Encoding UTF8

function Invoke-DockerDump {
    param([string]$Name)
    $null = docker ps --filter "name=^/${Name}$" --format "{{.Names}}" 2>$null
    if ($LASTEXITCODE -ne 0) { return $false }
    $running = docker ps --filter "name=^/${Name}$" --format "{{.Names}}" 2>$null
    if (-not $running) { return $false }
    docker exec $Name mysqldump -u$User -p$Password `
        --single-transaction --routines --triggers --set-gtid-purged=OFF --hex-blob `
        --default-character-set=utf8mb4 $Database 2>$null | Out-File -FilePath $TempFile -Append -Encoding utf8
    return $LASTEXITCODE -eq 0
}

function Invoke-LocalDump {
    $dump = Get-Command mysqldump -ErrorAction SilentlyContinue
    if (-not $dump) { return $false }
    & mysqldump -h$Host -P$Port -u$User "-p$Password" `
        --single-transaction --routines --triggers --set-gtid-purged=OFF --hex-blob `
        --default-character-set=utf8mb4 $Database 2>$null | Out-File -FilePath $TempFile -Append -Encoding utf8
    return $LASTEXITCODE -eq 0
}

$ok = $false
if (Invoke-DockerDump -Name $Container) {
    Write-Host "[db-export] exported via docker container: $Container"
    $ok = $true
} elseif (Invoke-LocalDump) {
    Write-Host "[db-export] exported via local mysqldump"
    $ok = $true
}

if (-not $ok) {
    Remove-Item -Force $TempFile -ErrorAction SilentlyContinue
    Write-Error "[db-export] failed: mysql container '$Container' not running and local mysqldump unavailable"
}

Move-Item -Force $TempFile $OutFile
$sizeKb = [math]::Round((Get-Item $OutFile).Length / 1KB, 1)
Write-Host "[db-export] saved -> $OutFile ($sizeKb KB)"
