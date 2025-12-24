#!/bin/bash

#############################################################
# Test Execution Script for Microservices with SQLite+Feign
#
# This script runs tests for all microservices using the
# "test-sync" profile which replaces PostgreSQL with H2
# and Kafka with Feign REST calls.
#
# Usage: ./run-tests.sh [service-name] [log-level]
# Example:
#   ./run-tests.sh                    # Run all tests
#   ./run-tests.sh user-service       # Run user-service tests only
#   ./run-tests.sh wallet-service debug  # Run with debug logs
#############################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR"

SERVICE=${1:-all}
LOG_LEVEL=${2:-info}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="$PROJECT_ROOT/test-reports-$TIMESTAMP"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Main execution
print_header "Zaphira Microservices Test Suite"
print_info "Profile: test-sync (SQLite + Feign)"
print_info "Log Level: $LOG_LEVEL"
print_info "Service: $SERVICE"

mkdir -p "$REPORT_DIR"

# Maven command base
MAVEN_CMD="mvn clean test"
MAVEN_OPTS="-Dspring.profiles.active=test-sync"

# Add log level if not default
if [ "$LOG_LEVEL" != "info" ]; then
    MAVEN_OPTS="$MAVEN_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=$LOG_LEVEL"
fi

# Build maven command based on service
case $SERVICE in
    user-service)
        print_header "Running User-Service Tests"
        cd "$PROJECT_ROOT/user-service"
        $MAVEN_CMD $MAVEN_OPTS 2>&1 | tee "$REPORT_DIR/user-service-test.log"
        TEST_RESULT=$?
        ;;
    
    wallet-service)
        print_header "Running Wallet-Service Tests"
        cd "$PROJECT_ROOT/wallet-service"
        $MAVEN_CMD $MAVEN_OPTS 2>&1 | tee "$REPORT_DIR/wallet-service-test.log"
        TEST_RESULT=$?
        ;;
    
    transaction-service)
        print_header "Running Transaction-Service Tests"
        cd "$PROJECT_ROOT/transaction-service"
        $MAVEN_CMD $MAVEN_OPTS 2>&1 | tee "$REPORT_DIR/transaction-service-test.log"
        TEST_RESULT=$?
        ;;
    
    all)
        print_header "Running All Tests"
        
        services=("user-service" "wallet-service" "transaction-service")
        failed_services=()
        
        for service in "${services[@]}"; do
            print_info "Testing $service..."
            cd "$PROJECT_ROOT/$service"
            
            if ! $MAVEN_CMD $MAVEN_OPTS 2>&1 | tee "$REPORT_DIR/$service-test.log"; then
                failed_services+=("$service")
            else
                print_success "$service passed"
            fi
        done
        
        cd "$PROJECT_ROOT"
        TEST_RESULT=0
        
        if [ ${#failed_services[@]} -gt 0 ]; then
            TEST_RESULT=1
            print_error "Failed services:"
            for service in "${failed_services[@]}"; do
                echo "  - $service"
            done
        fi
        ;;
    
    *)
        print_error "Unknown service: $SERVICE"
        echo "Available services: user-service, wallet-service, transaction-service, all"
        exit 1
        ;;
esac

# Summary
print_header "Test Execution Summary"
print_info "Reports saved to: $REPORT_DIR"

if [ $TEST_RESULT -eq 0 ]; then
    print_success "All tests passed!"
    exit 0
else
    print_error "Some tests failed. Check logs for details."
    exit 1
fi
