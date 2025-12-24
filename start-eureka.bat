@echo off
REM Démarrer Eureka Service Registry
echo.
echo ========================================
echo Démarrage Eureka Server (Port 8761)
echo ========================================
echo.
cd /d C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl service-registry spring-boot:run
