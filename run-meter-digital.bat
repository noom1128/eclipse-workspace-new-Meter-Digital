@echo off
title Meter Digital Pro Server System
color 0A
echo ====================================================================
echo             METER DIGITAL PRO - ENERGY MANAGEMENT SYSTEM
echo ====================================================================
echo.
echo [1/3] Checking environment and database...
if not exist "dorm.db" (
    echo [*] Initializing SQLite database dorm.db...
)

echo [2/3] Starting Meter Digital Backend Server (Dual Ports: 8443 HTTPS & 8080 HTTP)...
echo [*] Web & Mobile Portal (HTTPS): https://localhost:8443
echo [*] Mobile Meter Reader        : https://localhost:8443/meter-reader
echo [*] ESP32-CAM IoT Ingestion    : http://localhost:8080/api/meter/submit-photo
echo [*] Download Center            : https://localhost:8443/download
echo.
echo ====================================================================
echo   SERVER IS RUNNING! PLEASE KEEP THIS WINDOW OPEN.
echo   Press Ctrl+C to stop the server when finished.
echo ====================================================================
echo.

where java >nul 2>nul
if %errorlevel% equ 0 (
    if exist "meter-digital.jar" (
        java -jar meter-digital.jar
    ) else if exist "target\meter-digital-0.0.1-SNAPSHOT.jar" (
        java -jar target\meter-digital-0.0.1-SNAPSHOT.jar
    ) else (
        echo [ERROR] Application JAR file not found.
        pause
    )
) else (
    echo [*] Java runtime not in PATH, launching via Maven Wrapper...
    call .\mvnw.cmd spring-boot:run
)

pause
