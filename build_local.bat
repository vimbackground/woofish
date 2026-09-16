@echo off
setlocal
chcp 65001 >nul
where pwsh >nul 2>&1
if %ERRORLEVEL% equ 0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0build_local.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build_local.ps1" %*
)
exit /b %ERRORLEVEL%
