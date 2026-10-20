@echo off
title Create Meter Digital Desktop Shortcut
echo ==========================================================
echo   Creating Meter Digital Shortcut on your Desktop...
echo ==========================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ws = New-Object -ComObject WScript.Shell; $desk = [Environment]::GetFolderPath('Desktop'); $s = $ws.CreateShortcut((Join-Path $desk 'Meter Digital Pro.lnk')); $s.TargetPath = '%~dp0run-meter-digital.bat'; $s.WorkingDirectory = '%~dp0'; $s.IconLocation = '%~dp0meter-pro.ico,0'; $s.Description = 'Meter Digital Pro Server System'; $s.Save()"

if %errorlevel% equ 0 (
    echo [OK] Shortcut 'Meter Digital Pro' with icon successfully created on Desktop!
) else (
    echo [ERROR] Failed to create shortcut.
)

echo.
pause
