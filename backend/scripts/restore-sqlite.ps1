param(
  [Parameter(Mandatory = $true)][string]$Backup,
  [string]$Database = "..\data\contract-system.db"
)

$ErrorActionPreference = 'Stop'
$backupPath = [IO.Path]::GetFullPath($Backup)
$databasePath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $Database))
if (-not (Test-Path -LiteralPath $backupPath)) { throw "Backup not found: $backupPath" }
Copy-Item -LiteralPath $backupPath -Destination $databasePath -Force
Get-FileHash -Algorithm SHA256 -LiteralPath $databasePath | Select-Object Path, Hash
