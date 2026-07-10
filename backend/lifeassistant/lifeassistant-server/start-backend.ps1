$JAVA = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }
$JAR_DIR = Resolve-Path "$PSScriptRoot\..\target\app"
$UPLOAD_DIR = Join-Path (Resolve-Path "$PSScriptRoot\..\..\..") "data\uploads"
New-Item -ItemType Directory -Force -Path $UPLOAD_DIR | Out-Null
$env:UPLOAD_DIR = $UPLOAD_DIR
Set-Location $JAR_DIR
$out = & $JAVA -jar bin\lifeassistant.jar 2>&1
$out | Out-File "$env:TEMP\backend-startup.log" -Encoding utf8
