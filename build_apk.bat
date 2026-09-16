@echo off
setlocal
chcp 65001 >nul
call "%~dp0build_local.bat" apk
exit /b %ERRORLEVEL%
