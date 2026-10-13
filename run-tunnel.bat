@echo off
title Cloudflare Free Domain Tunnel Generator
color 0B
echo ====================================================================
echo          CLOUDFLARE FREE DOMAIN & HTTPS TUNNEL GENERATOR
echo ====================================================================
echo.
echo [*] Launching Cloudflare Tunnel for Meter Digital HTTPS Server (Port 8443)...
echo [*] Creating Free Public Domain + SSL Certificate...
echo.
echo ====================================================================
echo   PLEASE COPY YOUR FREE DOMAIN URL FROM BELOW (trycloudflare.com)
echo ====================================================================
echo.

if not exist cloudflared.exe (
    echo [*] cloudflared.exe not found. Downloading latest official release from Cloudflare...
    curl -Lo cloudflared.exe https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
)

cloudflared.exe tunnel --url https://localhost:8443 --no-tls-verify

pause
