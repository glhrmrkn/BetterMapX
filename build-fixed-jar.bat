@echo off
setlocal
cd /d "%~dp0"
echo Building BetterMapX 0.3.0-alpha...
call gradlew.bat clean build
if errorlevel 1 (
  echo.
  echo BUILD FAILED
  exit /b 1
)
echo.
echo BUILD SUCCESSFUL
echo JAR: %CD%\build\libs\bettermapx-0.3.0-alpha.jar
endlocal
