param([switch]$TestApi)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$config=Get-Content -Raw .runtime/local-env.json|ConvertFrom-Json
$portable=Join-Path (Split-Path $projectRoot -Parent) 'AURUM Coffee/.runtime'
$toolchain=@{javaHome=if($env:JAVA_HOME){$env:JAVA_HOME}else{(Get-ChildItem $portable -Directory -Filter 'jdk-*'|Select-Object -First 1).FullName}}
& (Join-Path $portable "../scripts/Start-Database.ps1")
$env:POSTGRES_PASSWORD=$config.POSTGRES_PASSWORD;$env:POSTGRES_USER='logicore';$env:ADMIN_EMAIL=$config.ADMIN_EMAIL;$env:ADMIN_PASSWORD=$config.ADMIN_PASSWORD;$env:SESSION_SECURE='false'
$port=if($TestApi){8091}else{8090};$database=if($TestApi){'logicore_test'}else{'logicore'}
$env:APP_PORT="$port";$env:DATABASE_URL="jdbc:postgresql://127.0.0.1:55432/$database"
$suffix=if($TestApi){'test'}else{'app'};$processFile=".runtime/$suffix-processes.json"
if(Test-Path $processFile){foreach($entry in @((Get-Content -Raw $processFile|ConvertFrom-Json))){$p=Get-Process -Id $entry.id -ErrorAction SilentlyContinue;if($p -and $p.ProcessName -eq $entry.name -and $p.StartTime -and $p.StartTime.ToUniversalTime() -eq ([datetimeoffset]$entry.started).UtcDateTime){throw 'LogiCore is already running. Stop it with Stop-Local.ps1 first.'}}}
$jar=Join-Path $projectRoot 'backend/target/logicore-api-1.0.0.jar'
$backend=Start-Process -FilePath "$($toolchain.javaHome)/bin/java.exe" -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput ".runtime/$suffix-api.log" -RedirectStandardError ".runtime/$suffix-api-error.log" -PassThru
$entries=@(@{id=$backend.Id;started=$backend.StartTime.ToUniversalTime().ToString('o');name='java'});$entries|ConvertTo-Json|Set-Content $processFile
$ready=$false;for($i=0;$i -lt 45;$i++){try{if((Invoke-WebRequest "http://127.0.0.1:$port/actuator/health" -TimeoutSec 2).StatusCode -eq 200){$ready=$true;break}}catch{Start-Sleep -Seconds 1}}
if(!$ready){throw 'API failed to start. Inspect .runtime logs.'}
if(!$TestApi){$node=(Get-Command node).Source;$cli=Join-Path $projectRoot 'frontend/node_modules/@angular/cli/bin/ng.js';$web=Start-Process -FilePath $node -ArgumentList @(('"'+$cli+'"'),'serve','--host','127.0.0.1','--port','4200','--proxy-config','proxy.conf.json') -WorkingDirectory "$projectRoot/frontend" -WindowStyle Hidden -RedirectStandardOutput '.runtime/web.log' -RedirectStandardError '.runtime/web-error.log' -PassThru;$entries+=@{id=$web.Id;started=$web.StartTime.ToUniversalTime().ToString('o');name='node'};$entries|ConvertTo-Json|Set-Content $processFile;Write-Output 'LogiCore: http://127.0.0.1:4200'}else{Write-Output 'LogiCore isolated test API: http://127.0.0.1:8091'}
