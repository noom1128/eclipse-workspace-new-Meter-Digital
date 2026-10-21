@echo off
title Build Meter Digital Pro Setup ZIP
echo ==========================================================
echo   Building MeterDigitalPro-Setup.zip for Client Distribution...
echo ==========================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build_zip.ps1"

echo.
pause
