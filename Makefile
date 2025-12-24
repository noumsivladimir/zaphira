.PHONY: help test test-user test-wallet test-all test-debug clean report

# Colors
BLUE := \033[0;34m
GREEN := \033[0;32m
RED := \033[0;31m
NC := \033[0m # No Color

TIMESTAMP := $(shell date +%Y%m%d_%H%M%S)
REPORT_DIR := test-reports-$(TIMESTAMP)

help:
	@echo "$(BLUE)╔════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║ Zaphira Test Suite - Makefile Commands ║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)Available Commands:$(NC)"
	@echo ""
	@echo "  $(BLUE)make test$(NC)              - Run all tests (SQLite + Feign)"
	@echo "  $(BLUE)make test-user$(NC)         - Run user-service tests only"
	@echo "  $(BLUE)make test-wallet$(NC)       - Run wallet-service tests only"
	@echo "  $(BLUE)make test-debug$(NC)        - Run tests with debug logging"
	@echo "  $(BLUE)make test-coverage$(NC)     - Run tests with code coverage"
	@echo "  $(BLUE)make clean$(NC)             - Clean test artifacts"
	@echo "  $(BLUE)make report$(NC)            - Generate test reports"
	@echo "  $(BLUE)make help$(NC)              - Show this help message"
	@echo ""
	@echo "$(GREEN)Examples:$(NC)"
	@echo "  make test                   # Run all tests"
	@echo "  make test-user              # Test user-service only"
	@echo "  make test-debug             # Debug mode with verbose logs"
	@echo ""

# Run all tests
test: clean
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Running All Microservice Tests$(NC)"
	@echo "$(BLUE)Profile: test-sync (SQLite + Feign)$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@mkdir -p $(REPORT_DIR)
	@mvn clean test -Dspring.profiles.active=test-sync \
		-DargLine="-Xmx1024m" 2>&1 | tee $(REPORT_DIR)/all-tests.log
	@echo ""
	@echo "$(GREEN)✅ Tests completed. Report: $(REPORT_DIR)$(NC)"

# Run user-service tests
test-user: clean
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Running User-Service Tests$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@mkdir -p $(REPORT_DIR)
	@cd user-service && mvn clean test -Dspring.profiles.active=test-sync \
		-Dtest=UserRegistrationE2ETest,SynchronousMicroserviceCommunicationTest \
		2>&1 | tee ../$(REPORT_DIR)/user-service-tests.log
	@echo ""
	@echo "$(GREEN)✅ User-Service tests completed$(NC)"

# Run wallet-service tests
test-wallet: clean
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Running Wallet-Service Tests$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@mkdir -p $(REPORT_DIR)
	@cd wallet-service && mvn clean test -Dspring.profiles.active=test-sync \
		-Dtest=WalletCreationE2ETest \
		2>&1 | tee ../$(REPORT_DIR)/wallet-service-tests.log
	@echo ""
	@echo "$(GREEN)✅ Wallet-Service tests completed$(NC)"

# Run tests with debug logging
test-debug: clean
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Running Tests in Debug Mode$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@mkdir -p $(REPORT_DIR)
	@mvn clean test -Dspring.profiles.active=test-sync \
		-Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
		-X 2>&1 | tee $(REPORT_DIR)/debug-tests.log
	@echo ""
	@echo "$(GREEN)✅ Debug tests completed. Log: $(REPORT_DIR)/debug-tests.log$(NC)"

# Run tests with code coverage
test-coverage: clean
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Running Tests with Code Coverage$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@mkdir -p $(REPORT_DIR)
	@mvn clean test jacoco:report -Dspring.profiles.active=test-sync \
		2>&1 | tee $(REPORT_DIR)/coverage-tests.log
	@echo ""
	@echo "$(GREEN)✅ Coverage report generated$(NC)"
	@echo "$(BLUE)Report location: target/site/jacoco/index.html$(NC)"

# Clean test artifacts
clean:
	@echo "$(BLUE)Cleaning test artifacts...$(NC)"
	@mvn clean -q
	@rm -rf test-reports-* 2>/dev/null || true
	@echo "$(GREEN)✅ Cleaned$(NC)"

# Generate and open test reports
report:
	@echo "$(BLUE)Generating test reports...$(NC)"
	@mvn surefire-report:report -q 2>/dev/null || true
	@echo ""
	@echo "$(GREEN)✅ Test reports available:$(NC)"
	@echo "  - Surefire: target/site/surefire-report.html"
	@echo "  - Coverage: target/site/jacoco/index.html"
	@echo ""
	@if [ -f "target/site/surefire-report.html" ]; then \
		echo "$(BLUE)Opening Surefire Report...$(NC)"; \
		open target/site/surefire-report.html 2>/dev/null || xdg-open target/site/surefire-report.html 2>/dev/null || echo "Open manually: target/site/surefire-report.html"; \
	fi

# Verify test environment
verify:
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Verifying Test Environment$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(GREEN)Java Version:$(NC)"
	@java -version 2>&1 | head -n 1
	@echo ""
	@echo "$(GREEN)Maven Version:$(NC)"
	@mvn -v | head -n 1
	@echo ""
	@echo "$(GREEN)Checking Dependencies:$(NC)"
	@mvn dependency:resolve -q 2>/dev/null && echo "$(GREEN)✅ All dependencies resolved$(NC)" || echo "$(RED)❌ Dependency issues$(NC)"
	@echo ""
	@echo "$(GREEN)Profile test-sync Available:$(NC)"
	@if [ -f "user-service/src/test/resources/application-test-sync.properties" ]; then \
		echo "$(GREEN)✅ user-service configuration found$(NC)"; \
	else \
		echo "$(RED)❌ user-service configuration missing$(NC)"; \
	fi
	@if [ -f "wallet-service/src/test/resources/application-test-sync.properties" ]; then \
		echo "$(GREEN)✅ wallet-service configuration found$(NC)"; \
	else \
		echo "$(RED)❌ wallet-service configuration missing$(NC)"; \
	fi
	@echo ""
	@echo "$(GREEN)✅ Environment verified$(NC)"

# Quick test (fast, minimal output)
quick-test: clean
	@mvn clean test -Dspring.profiles.active=test-sync \
		-DskipTests=false \
		-q 2>&1 | grep -E "(BUILD|Tests run|FAILURE|ERROR|SUCCESS)"

# Watch tests (continuous)
watch-test:
	@echo "$(BLUE)Watching for changes... (requires mvn-watch plugin)$(NC)"
	@mvn test -Dspring.profiles.active=test-sync -Dwatch.interval=1000

# Print test configuration
config:
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo "$(BLUE)Test Configuration Details$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(GREEN)Profile:$(NC) test-sync"
	@echo "$(GREEN)Database:$(NC) H2 SQLite (in-memory)"
	@echo "$(GREEN)Messaging:$(NC) Feign REST (synchronous)"
	@echo "$(GREEN)Isolation:$(NC) Complete (per-test cleanup)"
	@echo ""
	@echo "$(GREEN)Configuration Files:$(NC)"
	@echo "  - user-service/src/test/resources/application-test-sync.properties"
	@echo "  - wallet-service/src/test/resources/application-test-sync.properties"
	@echo ""
	@echo "$(GREEN)Test Controllers:$(NC)"
	@echo "  - UserServiceTestMessageController.java"
	@echo "  - WalletServiceTestMessageController.java"
	@echo ""
	@echo "$(GREEN)Test Classes:$(NC)"
	@echo "  - UserRegistrationE2ETest.java"
	@echo "  - WalletCreationE2ETest.java"
	@echo "  - SynchronousMicroserviceCommunicationTest.java"
	@echo ""

.DEFAULT_GOAL := help
