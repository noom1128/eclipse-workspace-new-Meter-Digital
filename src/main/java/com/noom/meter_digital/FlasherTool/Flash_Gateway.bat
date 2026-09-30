@echo off
title 1-Click Meter Gateway Flasher Tool
color 0A

echo ========================================================
echo         1-Click Meter Gateway Flasher Tool
echo ========================================================
echo.

set ESPTOOL_PATH="C:\Users\Nopporn.C\AppData\Local\Arduino15\packages\esp32\tools\esptool_py\5.3.1\esptool.exe"
set FIRMWARE_PATH="..\MeterDigital_Upload_Wifi_API\build\esp32.esp32.esp32\MeterDigital_Upload_Wifi_API.ino.bin"

if not exist %FIRMWARE_PATH% (
    set FIRMWARE_PATH="..\MeterDigital_Upload_Wifi_API\MeterDigital_Upload_Wifi_API.ino.bin"
)

if not exist %FIRMWARE_PATH% (
    echo [ERROR] Firmware binary file not found: %FIRMWARE_PATH%
    echo Please export compiled binary in Arduino IDE (Ctrl + Alt + S)
    echo.
    pause
    exit /b
)

echo Searching for available COM ports...
powershell -Command "Get-CimInstance Win32_SerialPort | Select-Object DeviceID, Caption" 2>NUL
echo.

set /p COMPORT="Enter COM Port (e.g. COM67 and press Enter): "

if "%COMPORT%"=="" (
    echo [ERROR] No COM port entered.
    pause
    exit /b
)

echo.
echo Flashing Firmware to %COMPORT%...
echo --------------------------------------------------------

%ESPTOOL_PATH% --chip esp32 --port %COMPORT% --baud 921600 --before default-reset --after hard_reset write-flash -z 0x10000 %FIRMWARE_PATH%

if %ERRORLEVEL% EQU 0 (
    echo --------------------------------------------------------
    echo [SUCCESS] Firmware Flashed Successfully!
    echo You can now disconnect the USB-C cable.
    echo --------------------------------------------------------
) else (
    echo --------------------------------------------------------
    echo [ERROR] Flashing failed! (Permission Denied / Port Busy)
    echo Please CLOSE Serial Monitor in Arduino IDE and try again.
    echo --------------------------------------------------------
)

echo.
pause
