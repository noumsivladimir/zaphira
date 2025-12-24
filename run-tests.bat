@echo off
REM #############################################################
REM # Test Execution Script for Windows
REM # 
REM # Tests all microservices using "test-sync" profile
REM # (SQLite + Feign instead of PostgreSQL + Kafka)
REM #
REM # Usage: run-tests.bat [service-name] [log-level]
REM # Example:
REM #   run-tests.bat                    - Run all tests
REM #   run-tests.bat user-service       - Run user-service tests
REM #   run-tests.bat wallet-service debug - Run with debug logs
REM #############################################################

setlocal enabledelayedexpansion

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"

REM Parse arguments
set "SERVICE=%1"
set "LOG_LEVEL=%2"

if "%SERVICE%"=="" set "SERVICE=all"
if "%LOG_LEVEL%"=="" set "LOG_LEVEL=info"

REM Generate timestamp for reports
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set "REPORT_DIR=%PROJECT_ROOT%test-reports-%mydate%_%mytime%"

REM Create report directory
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"

echo.
echo ========================================
echo Zaphira Microservices Test Suite
echo ========================================
echo Profile: test-sync (SQLite + Feign)
echo Log Level: %LOG_LEVEL%
echo Service: %SERVICE%
echo Reports: %REPORT_DIR%
echo.

REM Maven base command
set "MAVEN_CMD=mvn clean test"
set "MAVEN_OPTS=-Dspring.profiles.active=test-sync"

if not "%LOG_LEVEL%"=="info" (
    set "MAVEN_OPTS=%MAVEN_OPTS% -Dorg.slf4j.simpleLogger.defaultLogLevel=%LOG_LEVEL%"
)

REM Execute based on service
if "%SERVICE%"=="user-service" (
    echo Running User-Service Tests...
    cd /d "%PROJECT_ROOT%user-service"
    call %MAVEN_CMD% %MAVEN_OPTS% > "%REPORT_DIR%\user-service-test.log" 2>&1
    set TEST_RESULT=!ERRORLEVEL!
) else if "%SERVICE%"=="wallet-service" (
    echo Running Wallet-Service Tests...
    cd /d "%PROJECT_ROOT%wallet-service"
    call %MAVEN_CMD% %MAVEN_OPTS% > "%REPORT_DIR%\wallet-service-test.log" 2>&1
    set TEST_RESULT=!ERRORLEVEL!
) else if "%SERVICE%"=="transaction-service" (
    echo Running Transaction-Service Tests...
    cd /d "%PROJECT_ROOT%transaction-service"
    call %MAVEN_CMD% %MAVEN_OPTS% > "%REPORT_DIR%\transaction-service-test.log" 2>&1
    set TEST_RESULT=!ERRORLEVEL!
) else if "%SERVICE%"=="all" (
    echo Running All Tests...
    set TEST_RESULT=0
    
    for %%S in (user-service wallet-service transaction-service) do (
        echo.
        echo Testing %%S...
        cd /d "%PROJECT_ROOT%%%S"
        call %MAVEN_CMD% %MAVEN_OPTS% > "%REPORT_DIR%\%%S-test.log" 2>&1
        if !ERRORLEVEL! neq 0 (
            set TEST_RESULT=1
            echo ❌ %%S failed
        ) else (
            echo ✅ %%S passed
        )
    )
    
    cd /d "%PROJECT_ROOT%"
) else (
    echo ❌ Unknown service: %SERVICE%
    echo Available services: user-service, wallet-service, transaction-service, all
    exit /b 1
)

REM Print summary
echo.
echo ========================================
echo Test Execution Summary
echo ========================================
echo Reports saved to: %REPORT_DIR%
echo.

if %TEST_RESULT% equ 0 (
    echo ✅ All tests passed!
    exit /b 0
) else (
    echo ❌ Some tests failed. Check logs for details.
    exit /b 1
)

endlocal
