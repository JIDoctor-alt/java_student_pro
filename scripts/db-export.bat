@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0db-export.ps1" %*
exit /b %ERRORLEVEL%
