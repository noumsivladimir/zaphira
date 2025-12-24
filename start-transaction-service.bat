@echo off
REM Démarrer Transaction Service
echo.
echo ========================================
echo Démarrage Transaction Service (Port 8083)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl transaction-service spring-boot:run
