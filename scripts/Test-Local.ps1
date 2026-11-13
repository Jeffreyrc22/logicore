$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
try {& "$PSScriptRoot/Start-Local.ps1" -TestApi;$config=Get-Content -Raw .runtime/local-env.json|ConvertFrom-Json;$env:ADMIN_EMAIL=$config.ADMIN_EMAIL;$env:ADMIN_PASSWORD=$config.ADMIN_PASSWORD;$env:TEST_API_URL='http://127.0.0.1:8091';$env:TEST_PSQL=Join-Path (Split-Path $projectRoot -Parent) "AURUM Coffee/.runtime/pgsql/bin/psql.exe";node --test tests/api.integration.test.mjs;if($LASTEXITCODE -ne 0){throw 'Integration checks failed.'}}finally{& "$PSScriptRoot/Stop-Local.ps1" -TestApi}
