@echo off
echo ========================================
echo Testing Email Verification Registration
echo ========================================

echo Starting services...
echo.

echo 1. Starting Eureka Server...
start "Eureka Server" cmd /c "cd config-server && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=eureka"

timeout /t 10 /nobreak > nul

echo 2. Starting Config Server...
start "Config Server" cmd /c "cd config-server && mvn spring-boot:run"

timeout /t 10 /nobreak > nul

echo 3. Starting Service Registry...
start "Service Registry" cmd /c "cd service-registry && mvn spring-boot:run"

timeout /t 15 /nobreak > nul

echo 4. Starting Notification Service...
start "Notification Service" cmd /c "cd notification-service && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=kafka"

timeout /t 10 /nobreak > nul

echo 5. Starting User Service...
start "User Service" cmd /c "cd user-service && mvn spring-boot:run"

echo.
echo Services are starting... Waiting 30 seconds for full startup...
timeout /t 30 /nobreak > nul

echo.
echo ========================================
echo Testing User Registration with Email Verification
echo ========================================

echo Testing user registration...
curl -X POST http://localhost:8082/api/users/register ^
  -H "Content-Type: application/json" ^
  -d "{^
    \"phoneNumber\": \"+237690000001\",^
    \"email\": \"test@example.com\",^
    \"pin\": \"1234\",^
    \"firstName\": \"Test\",^
    \"lastName\": \"User\",^
    \"dateOfBirth\": \"1990-01-01\",^
    \"country\": \"Cameroon\",^
    \"neighborhood\": \"Test Neighborhood\",^
    \"city\": \"Yaounde\",^
    \"region\": \"Centre\",^
    \"preferredLanguage\": \"fr\"^
  }"

echo.
echo ========================================
echo Check the logs above for:
echo - User registration with PENDING_VERIFICATION status
echo - OTP generation and email sending via Kafka
echo - Email event consumption in notification service
echo ========================================

pause