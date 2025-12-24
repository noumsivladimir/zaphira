@echo off
REM Démarrer Auth Service
echo.
echo ========================================
echo Démarrage Auth Service (Port 8081)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl auth spring-boot:run
