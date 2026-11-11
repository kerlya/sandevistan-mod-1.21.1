@echo off
setlocal
set "V=8.10.2"
set "B=%USERPROFILE%\.gradle\bootstrap"
set "D=%B%\gradle-%V%"
set "Z=%B%\gradle-%V%-bin.zip"
if not exist "%D%\bin\gradle.bat" (
 if not exist "%B%" mkdir "%B%"
 echo Downloading Gradle %V%...
 powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%V%-bin.zip' -OutFile '%Z%'"
 if errorlevel 1 exit /b 1
 powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%Z%' -DestinationPath '%B%' -Force"
 if errorlevel 1 exit /b 1
)
"%D%\bin\gradle.bat" %*
endlocal
