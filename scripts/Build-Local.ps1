$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
# Windows locks a running executable JAR; fail before Maven can replace it.
foreach ($mode in @('app','test')) {
  $record = Join-Path $projectRoot ".runtime/$mode-processes.json"
  if (Test-Path -LiteralPath $record) {
    foreach ($entry in @((Get-Content -Raw $record | ConvertFrom-Json))) {
      $process = Get-Process -Id $entry.id -ErrorAction SilentlyContinue
      if ($process -and $process.ProcessName -eq 'java' -and $process.StartTime -and $process.StartTime.ToUniversalTime() -eq ([datetimeoffset]$entry.started).UtcDateTime) {
        throw "Stop the recorded $mode API with Stop-Local.ps1 (add -TestApi for tests) before building."
      }
    }
  }
}
$portable=Join-Path (Split-Path $projectRoot -Parent) 'AURUM Coffee/.runtime'
if(!$env:JAVA_HOME){$env:JAVA_HOME=(Get-ChildItem $portable -Directory -Filter 'jdk-*'|Select-Object -First 1).FullName}
$maven=if(Get-Command mvn.cmd -ErrorAction SilentlyContinue){(Get-Command mvn.cmd).Source}else{Join-Path $portable 'apache-maven-3.9.11/bin/mvn.cmd'}
& $maven "-Dmaven.repo.local=$portable/m2" -f backend/pom.xml verify
if($LASTEXITCODE -ne 0){throw 'Backend build failed.'}
npm.cmd --prefix frontend run build
if($LASTEXITCODE -ne 0){throw 'Frontend build failed.'}
