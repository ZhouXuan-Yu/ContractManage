param(
    [string]$DbPassword,
    [switch]$UseDify,
    [string]$DifyBaseUrl,
    [string]$ExtractApiKey,
    [string]$ReviewApiKey
)

if ([string]::IsNullOrWhiteSpace($DbPassword)) { throw 'Pass the DEV database password with -DbPassword.' }
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_PASSWORD = $DbPassword
if ($UseDify) {
    if ([string]::IsNullOrWhiteSpace($DifyBaseUrl) -or [string]::IsNullOrWhiteSpace($ExtractApiKey) -or [string]::IsNullOrWhiteSpace($ReviewApiKey)) { throw 'Pass DifyBaseUrl, ExtractApiKey, and ReviewApiKey when using Dify.' }
    $env:CONTRACT_AI_PROVIDER = 'dify'
    $env:DIFY_BASE_URL = $DifyBaseUrl
    $env:DIFY_API_KEY_EXTRACT = $ExtractApiKey
    $env:DIFY_API_KEY_REVIEW = $ReviewApiKey
}
mvn.cmd "-Dmaven.repo.local=D:\project\hr-contract\Hrcontract\.m2-local" spring-boot:run
