#!/bin/bash
# ============================================================================
# BASH SCRIPT - Implementation de validation_requests table
# ============================================================================
# Usage: bash ./deploy_validation_table.sh
# ============================================================================

set -e  # Exit on error

# Configuration
DB_HOST="192.168.0.122"
DB_PORT="5432"
DB_NAME="wallet_db"
DB_USER="postgres"
DB_PASSWORD="1234"  # Change this to your actual password

echo "=========================================="
echo "Deploying validation_requests table..."
echo "=========================================="
echo ""

# Step 1: Verify connection to PostgreSQL
echo "[1/5] Verifying PostgreSQL connection..."
if psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1; then
    echo "✅ Connection successful"
else
    echo "❌ Connection failed. Check your credentials."
    exit 1
fi
echo ""

# Step 2: Create table
echo "[2/5] Creating validation_requests table..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME << 'EOF'
CREATE TABLE IF NOT EXISTS validation_requests (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(36) NOT NULL UNIQUE,
    transaction_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '5 minutes'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
EOF
echo "✅ Table created"
echo ""

# Step 3: Create indexes
echo "[3/5] Creating indexes..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME << 'EOF'
CREATE INDEX IF NOT EXISTS idx_validation_requests_correlation_id ON validation_requests(correlation_id);
CREATE INDEX IF NOT EXISTS idx_validation_requests_transaction_id ON validation_requests(transaction_id);
CREATE INDEX IF NOT EXISTS idx_validation_requests_status ON validation_requests(status);
CREATE INDEX IF NOT EXISTS idx_validation_requests_expires_at ON validation_requests(expires_at);
CREATE INDEX IF NOT EXISTS idx_validation_requests_pending ON validation_requests(status, expires_at) WHERE status = 'PENDING';
EOF
echo "✅ Indexes created"
echo ""

# Step 4: Add comments
echo "[4/5] Adding documentation comments..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME << 'EOF'
COMMENT ON TABLE validation_requests IS 'Tracks validation requests for Kafka idempotence. Ensures exactly-once processing of validation results.';
COMMENT ON COLUMN validation_requests.correlation_id IS 'Unique UUID for idempotence detection.';
COMMENT ON COLUMN validation_requests.transaction_id IS 'Foreign key reference to the transaction.';
COMMENT ON COLUMN validation_requests.status IS 'Status: PENDING, PROCESSED, or EXPIRED.';
EOF
echo "✅ Comments added"
echo ""

# Step 5: Verify deployment
echo "[5/5] Verifying deployment..."
echo ""
echo "Table structure:"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "\d validation_requests"
echo ""
echo "Indexes:"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT indexname FROM pg_indexes WHERE tablename='validation_requests' ORDER BY indexname;"
echo ""
echo "Row count:"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT COUNT(*) as row_count FROM validation_requests;"
echo ""

echo "=========================================="
echo "✅ DEPLOYMENT SUCCESSFUL!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Start/restart transaction-service"
echo "2. Monitor logs for Kafka consumer startup"
echo "3. Test with PHASE2_TESTING_PLAN.md"
echo ""
