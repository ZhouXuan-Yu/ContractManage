param(
  [string]$Database = "..\data\contract-system.db",
  [string]$OutputDirectory = "..\data\backups"
)

$ErrorActionPreference = 'Stop'
$databasePath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $Database))
$outputPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $OutputDirectory))
if (-not (Test-Path -LiteralPath $databasePath)) { throw "SQLite database not found: $databasePath" }
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = Join-Path $outputPath "contract-system-$stamp.db"
Copy-Item -LiteralPath $databasePath -Destination $backupPath
Get-FileHash -Algorithm SHA256 -LiteralPath $backupPath | Select-Object Path, Hash
