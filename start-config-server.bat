@echo off
REM Démarrer Config Server
echo.
echo ========================================
echo Démarrage Config Server (Port 8888)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl config-server spring-boot:run
