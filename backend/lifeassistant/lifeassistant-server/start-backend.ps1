$JAVA = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }
$JAR_DIR = Resolve-Path "$PSScriptRoot\..\target\app"
Set-Location $JAR_DIR
$out = & $JAVA -jar bin\lifeassistant.jar 2>&1
$out | Out-File "$env:TEMP\backend-startup.log" -Encoding utf8
