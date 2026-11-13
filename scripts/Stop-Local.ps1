param([switch]$TestApi)
$projectRoot=Split-Path $PSScriptRoot -Parent
$suffix=if($TestApi){'test'}else{'app'};$file=Join-Path $projectRoot ".runtime/$suffix-processes.json"
if(Test-Path $file){foreach($entry in @((Get-Content -Raw $file|ConvertFrom-Json))){$p=Get-Process -Id $entry.id -ErrorAction SilentlyContinue;if($p -and $p.ProcessName -eq $entry.name -and $p.StartTime -and $p.StartTime.ToUniversalTime() -eq ([datetimeoffset]$entry.started).UtcDateTime){Stop-Process -Id $p.Id}}}
