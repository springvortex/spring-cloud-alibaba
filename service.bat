@echo off
chcp 65001 >nul 2>&1
rem ============================================================
rem  Spring Cloud Alibaba - Service Manager launcher (Windows)
rem  Double-click for interactive menu, or pass CLI args:
rem    service.bat status
rem    service.bat start service-provider
rem    service.bat stop all
rem ============================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0service.ps1" %*
pause