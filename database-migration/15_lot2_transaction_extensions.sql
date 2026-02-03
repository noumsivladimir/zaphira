-- ============================================================================
-- LOT 2 - Transaction Extensions Schema
-- ============================================================================
-- Created: 2026-02-03
-- Purpose: Add fees, metadata, and timeline tables for LOT 2 features
-- Dependencies: 14_create_transactions_core_table.sql

-- ============================================================================
-- SWITCH TO TRANSACTION DATABASE
-- ============================================================================
\c transaction_db;

-- ============================================================================
-- ALTER TABLE: transactions_core - Add LOT 2 fields
-- ============================================================================

ALTER TABLE transactions_core
    ADD COLUMN IF NOT EXISTS fee_amount DECIMAL(19, 4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(19, 4);

-- Update existing records to calculate total_amount
UPDATE transactions_core
SET total_amount = amount + COALESCE(fee_amount, 0)
WHERE total_amount IS NULL;

COMMENT ON COLUMN transactions_core.fee_amount IS 'Total fee amount (quick access without join)';
COMMENT ON COLUMN transactions_core.total_amount IS 'Total amount including fees (amount + fee_amount)';

-- ============================================================================
-- CREATE TABLE: transaction_fees
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_fees (
    id BIGINT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    -- Fee breakdown
    platform_fee DECIMAL(19, 4) DEFAULT 0,
    merchant_fee DECIMAL(19, 4) DEFAULT 0,
    total_fees DECIMAL(19, 4) NOT NULL DEFAULT 0,
    
    -- Fee calculation details (JSON)
    fee_details TEXT,
    
    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key
    CONSTRAINT fk_fees_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions_core(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT check_platform_fee CHECK (platform_fee >= 0),
    CONSTRAINT check_merchant_fee CHECK (merchant_fee >= 0),
    CONSTRAINT check_total_fees CHECK (total_fees >= 0)
);

-- Index
CREATE INDEX IF NOT EXISTS idx_fees_transaction ON transaction_fees(transaction_id);

-- Comments
COMMENT ON TABLE transaction_fees IS 'LOT 2 - Fee breakdown for transactions';
COMMENT ON COLUMN transaction_fees.platform_fee IS 'Platform commission (0.5% for transfers, 0.5% for merchant payments)';
COMMENT ON COLUMN transaction_fees.merchant_fee IS 'Merchant commission (2% for merchant payments)';
COMMENT ON COLUMN transaction_fees.total_fees IS 'Total fees (sum of platform_fee + merchant_fee)';
COMMENT ON COLUMN transaction_fees.fee_details IS 'JSON string with fee calculation details';

-- ============================================================================
-- CREATE TABLE: transaction_metadata
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_metadata (
    id BIGINT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    -- Failure tracking
    failure_reason VARCHAR(500),
    
    -- Retry mechanism
    retry_count INTEGER DEFAULT 0,
    max_retry_attempts INTEGER DEFAULT 3,
    
    -- Request information
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    device_info VARCHAR(255),
    
    -- Additional data
    metadata TEXT,
    notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Foreign key
    CONSTRAINT fk_metadata_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions_core(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT check_retry_count CHECK (retry_count >= 0),
    CONSTRAINT check_max_attempts CHECK (max_retry_attempts > 0)
);

-- Index
CREATE INDEX IF NOT EXISTS idx_metadata_transaction ON transaction_metadata(transaction_id);
CREATE INDEX IF NOT EXISTS idx_metadata_retry ON transaction_metadata(retry_count, max_retry_attempts);

-- Comments
COMMENT ON TABLE transaction_metadata IS 'LOT 2 - Additional transaction metadata and retry information';
COMMENT ON COLUMN transaction_metadata.failure_reason IS 'Reason for transaction failure';
COMMENT ON COLUMN transaction_metadata.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN transaction_metadata.max_retry_attempts IS 'Maximum retry attempts allowed (default 3)';
COMMENT ON COLUMN transaction_metadata.metadata IS 'Custom metadata in JSON format';

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_transaction_metadata_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_transaction_metadata_updated_at ON transaction_metadata;

CREATE TRIGGER trigger_transaction_metadata_updated_at
    BEFORE UPDATE ON transaction_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_metadata_updated_at();

-- ============================================================================
-- CREATE TABLE: transaction_timeline
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_timeline (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    -- Status change tracking
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    
    -- Change information
    change_reason VARCHAR(500),
    changed_by VARCHAR(100),
    details TEXT,
    
    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key
    CONSTRAINT fk_timeline_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions_core(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT check_new_status CHECK (new_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_timeline_transaction ON transaction_timeline(transaction_id);
CREATE INDEX IF NOT EXISTS idx_timeline_created ON transaction_timeline(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_timeline_status ON transaction_timeline(new_status);

-- Comments
COMMENT ON TABLE transaction_timeline IS 'LOT 2 - Audit trail of transaction status changes';
COMMENT ON COLUMN transaction_timeline.previous_status IS 'Status before change (NULL for initial creation)';
COMMENT ON COLUMN transaction_timeline.new_status IS 'Status after change';
COMMENT ON COLUMN transaction_timeline.change_reason IS 'Reason for status change';
COMMENT ON COLUMN transaction_timeline.changed_by IS 'User or system that triggered the change';

-- ============================================================================
-- UPDATE TRANSACTION TYPE CONSTRAINT - Add MERCHANT_PAYMENT
-- ============================================================================

ALTER TABLE transactions_core DROP CONSTRAINT IF EXISTS transactions_core_type_check;

ALTER TABLE transactions_core
    ADD CONSTRAINT transactions_core_type_check CHECK (
        type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'REVERSAL', 'MERCHANT_PAYMENT')
    );

-- ============================================================================
-- INSERT SAMPLE DATA (OPTIONAL - for testing)
-- ============================================================================

/*
-- Sample transfer with fees
INSERT INTO transactions_core (reference, sender_wallet_id, receiver_wallet_id, amount, currency, type, status, fee_amount, total_amount, description)
VALUES ('TXN-20260203-LOT2-001', 1, 2, 100.00, 'EUR', 'TRANSFER', 'COMPLETED', 0.50, 100.50, 'Test transfer with 0.5% fee');

INSERT INTO transaction_fees (id, transaction_id, platform_fee, merchant_fee, total_fees, fee_details)
SELECT id, id, 0.50, 0.00, 0.50, '{"type":"TRANSFER","platformRate":"0.5%"}'
FROM transactions_core WHERE reference = 'TXN-20260203-LOT2-001';

-- Sample merchant payment with fees
INSERT INTO transactions_core (reference, sender_wallet_id, receiver_wallet_id, amount, currency, type, status, fee_amount, total_amount, description)
VALUES ('TXN-20260203-LOT2-002', 1, 3, 200.00, 'EUR', 'MERCHANT_PAYMENT', 'COMPLETED', 5.00, 205.00, 'Merchant payment with 2.5% total fees');

INSERT INTO transaction_fees (id, transaction_id, platform_fee, merchant_fee, total_fees, fee_details)
SELECT id, id, 1.00, 4.00, 5.00, '{"type":"MERCHANT_PAYMENT","platformRate":"0.5%","merchantRate":"2%"}'
FROM transactions_core WHERE reference = 'TXN-20260203-LOT2-002';

-- Sample timeline entries
INSERT INTO transaction_timeline (transaction_id, previous_status, new_status, change_reason, changed_by)
SELECT id, NULL, 'PENDING', 'Transaction created', 'SYSTEM'
FROM transactions_core WHERE reference = 'TXN-20260203-LOT2-001';

INSERT INTO transaction_timeline (transaction_id, previous_status, new_status, change_reason, changed_by)
SELECT id, 'PENDING', 'COMPLETED', 'Transaction completed successfully', 'SYSTEM'
FROM transactions_core WHERE reference = 'TXN-20260203-LOT2-001';
*/

-- ============================================================================
-- VERIFY CREATION
-- ============================================================================

-- Check tables exist
SELECT schemaname, tablename, tableowner 
FROM pg_tables 
WHERE tablename IN ('transaction_fees', 'transaction_metadata', 'transaction_timeline')
ORDER BY tablename;

-- Check new columns in transactions_core
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'transactions_core' 
  AND column_name IN ('fee_amount', 'total_amount')
ORDER BY ordinal_position;

-- Check transaction_fees structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'transaction_fees'
ORDER BY ordinal_position;

-- Check transaction_metadata structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'transaction_metadata'
ORDER BY ordinal_position;

-- Check transaction_timeline structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'transaction_timeline'
ORDER BY ordinal_position;

-- Check foreign keys
SELECT 
    tc.table_name, 
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN ('transaction_fees', 'transaction_metadata', 'transaction_timeline')
ORDER BY tc.table_name;

-- ============================================================================
-- STATISTICS
-- ============================================================================

ANALYZE transaction_fees;
ANALYZE transaction_metadata;
ANALYZE transaction_timeline;
ANALYZE transactions_core;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

\echo '✅ LOT 2 - Transaction extensions created successfully!'
\echo ''
\echo 'Tables created:'
\echo '  1. transaction_fees (fee breakdown)'
\echo '  2. transaction_metadata (retry, failure tracking)'
\echo '  3. transaction_timeline (status change audit)'
\echo ''
\echo 'transactions_core updated:'
\echo '  - Added fee_amount column'
\echo '  - Added total_amount column'
\echo '  - Added MERCHANT_PAYMENT type'
\echo ''
\echo 'Next steps:'
\echo '1. Verify table structures above'
\echo '2. Restart transaction-service'
\echo '3. Test LOT 2 endpoints:'
\echo '   - POST /api/v1/transactions/merchant-payment'
\echo '   - POST /api/v1/transactions/{ref}/cancel'
\echo '   - POST /api/v1/transactions/{ref}/retry'
