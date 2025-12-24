@echo off
REM Démarrer Notification Service
echo.
echo ========================================
echo Démarrage Notification Service (Port 8084)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl notification-service spring-boot:run
