@echo off
setlocal

:: Meshingress Studio Web - Root Launch Command
set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js is not found in PATH.
    echo Please install Node.js ^(version 18 or higher^) from https://nodejs.org/
    exit /b 1
)

if not exist "%ROOT_DIR%scripts\launcher.js" (
    echo [ERROR] Launcher engine not found at "%ROOT_DIR%scripts\launcher.js"
    exit /b 1
)

node "%ROOT_DIR%scripts\launcher.js" %*
exit /b %ERRORLEVEL%
