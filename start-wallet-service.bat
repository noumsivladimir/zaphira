@echo off
REM Démarrer Wallet Service
echo.
echo ========================================
echo Démarrage Wallet Service (Port 8082)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl wallet-service spring-boot:run
