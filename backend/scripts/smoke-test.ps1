param([string]$BaseUrl = 'http://127.0.0.1:19090')
$ErrorActionPreference = 'Stop'
$jsonHeaders = @{ 'Content-Type'='application/json'; 'X-Role'='ADMIN'; 'X-User-Id'='10001'; 'X-Org-Id'='100'; 'X-Department-Id'='101' }
function Get-Json($Path, $Headers=$jsonHeaders) { Invoke-RestMethod -Uri "$BaseUrl$Path" -Headers $Headers -Method Get }
function Send-Json($Path, $Body, $Method='Post', $Headers=$jsonHeaders) { Invoke-RestMethod -Uri "$BaseUrl$Path" -Headers $Headers -Method $Method -Body ($Body | ConvertTo-Json -Depth 8) }
function Expect-Fail($Action, [int]$Status) { $actual = 0; try { & $Action | Out-Null } catch { $actual = [int]$_.Exception.Response.StatusCode }; if ($actual -ne $Status) { throw "Expected HTTP $Status but received $actual" } }

$health = Get-Json '/actuator/health'
if ($health.status -ne 'UP') { throw 'Backend health is not UP' }
$types = Get-Json '/api/contract-types'; $parties = Get-Json '/api/trade-parties'
if (!$types.Count -or !$parties.Count) { throw 'Base dictionaries are empty' }
$name = "Smoke contract $(Get-Date -Format 'yyyyMMddHHmmss')"
$created = Send-Json '/api/contracts' @{ typeId=$types[0].id; partyId=$parties[0].id; name=$name; remark='SQLite smoke test'; templateId=$null; totalAmount=1000; currency='CNY'; paymentDirection=2; expireDate='2030-12-31' }
$id = $created.id
$detail = Get-Json "/api/contracts/$id"
if ($detail.name -ne $name -or $detail.totalAmount -ne 1000) { throw 'Contract formal fields were not persisted' }
Send-Json "/api/contracts/$id/content-versions" '正文回归测试内容' | Out-Null
Send-Json "/api/contracts/$id/ai/extract" @{} | Out-Null
Send-Json "/api/contracts/$id/ai/review" @{} | Out-Null
Send-Json "/api/contracts/$id/approval" @{} | Out-Null
$authBody = @{ contractId=$id; targetUserId='10002'; view=$true; edit=$false; export=$true; legalConfirm=$false; expireTime=$null; remark='smoke authorization' }
Send-Json '/api/contract-authorizations' $authBody | Out-Null
$authorizedHeaders = @{ 'Content-Type'='application/json'; 'X-Role'='VIEWER'; 'X-User-Id'='10002'; 'X-Org-Id'='999'; 'X-Department-Id'='999' }
$authorized = Get-Json "/api/contracts/$id" $authorizedHeaders
if ($authorized.id -ne $id) { throw 'Named contract authorization did not grant view access' }
$content = Get-Json "/api/contracts/$id/content-versions" $authorizedHeaders
if (!$content.Count) { throw 'Named contract authorization did not grant content view access' }
Expect-Fail { Invoke-RestMethod -Uri "$BaseUrl/api/contracts/$id/content-versions" -Headers $authorizedHeaders -Method Post -Body 'Unauthorized content update' } 403
$stats = Get-Json '/api/statistics/overview' $authorizedHeaders
if ($null -eq $stats.total) { throw 'Statistics endpoint did not return a scoped result' }
Write-Output "SMOKE PASS contract=$id authorized-view=true edit-denied=true"
