@echo off
setlocal

:: Meshingress Studio Web - Scripts Launcher CMD
set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."

cd /d "%ROOT_DIR%"

where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js is not found in PATH.
    echo Please install Node.js ^(version 18 or higher^) from https://nodejs.org/
    exit /b 1
)

node "%SCRIPT_DIR%launcher.js" %*
exit /b %ERRORLEVEL%
