-- ============================================================================
-- ZAPHIRA Platform - Transaction Schema Refactoring
-- ============================================================================
-- Script: 13_refactor_transaction_tables.sql
-- Description: Refactor monolithic transactions table into normalized tables
-- Date: February 3, 2026
-- Purpose: Separate concerns and improve maintainability
-- ============================================================================

\c zaphira_transactions_db

-- ============================================================================
-- SECTION 1: CREATE NEW NORMALIZED TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: transaction_fees (Fee breakdown - 1:1 with transactions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_fees (
    transaction_id BIGINT PRIMARY KEY,
    
    -- Fee details
    fee DECIMAL(19,4) DEFAULT 0,
    fee_amount DECIMAL(19,4) DEFAULT 0,
    fee_currency VARCHAR(10),
    fee_type VARCHAR(50),
    service_fee DECIMAL(19,4) DEFAULT 0,
    
    -- Calculated amounts
    total_amount DECIMAL(19,4) NOT NULL,
    net_amount DECIMAL(19,4),
    
    -- Foreign exchange fees
    fx_fee DECIMAL(19,4) DEFAULT 0,
    exchange_rate DECIMAL(19,6),
    fx_rate DECIMAL(19,6),
    fx_rate_provider VARCHAR(50),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_fees_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_fees_transaction_id ON transaction_fees(transaction_id);

COMMENT ON TABLE transaction_fees IS 'Fee breakdown and calculations for transactions';

-- ============================================================================
-- TABLE: transaction_timeline (All timestamp events - 1:1 with transactions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_timeline (
    transaction_id BIGINT PRIMARY KEY,
    
    -- Lifecycle timestamps
    initiated_at TIMESTAMP,
    initiated_by VARCHAR(100),
    pending_at TIMESTAMP,
    processing_at TIMESTAMP,
    authorized_at TIMESTAMP,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    reversed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    under_review_at TIMESTAMP,
    on_hold_at TIMESTAMP,
    expired_at TIMESTAMP,
    
    -- Audit
    last_updated_by VARCHAR(100),
    
    CONSTRAINT fk_transaction_timeline_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_timeline_transaction_id ON transaction_timeline(transaction_id);
CREATE INDEX idx_transaction_timeline_completed_at ON transaction_timeline(completed_at);
CREATE INDEX idx_transaction_timeline_failed_at ON transaction_timeline(failed_at);

COMMENT ON TABLE transaction_timeline IS 'Complete timeline of transaction state transitions';

-- ============================================================================
-- TABLE: transaction_metadata (Device, IP, metadata - already exists, ensure structure)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_metadata (
    transaction_id BIGINT PRIMARY KEY,
    
    -- Device and channel info
    device_info TEXT,
    ip_address VARCHAR(50),
    payment_method VARCHAR(50),
    
    -- Additional metadata (JSON)
    metadata TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_metadata_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_metadata_transaction_id ON transaction_metadata(transaction_id);

COMMENT ON TABLE transaction_metadata IS 'Device, IP, and metadata for transactions';

-- ============================================================================
-- TABLE: transaction_risk (Risk scoring and compliance - already exists, ensure structure)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_risk (
    transaction_id BIGINT PRIMARY KEY,
    
    -- Risk assessment
    risk_score DECIMAL(5,2),
    compliance_status VARCHAR(20),
    route VARCHAR(100),
    
    -- Device context (duplicated for risk analysis)
    ip_address VARCHAR(50),
    device_info TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_risk_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_risk_transaction_id ON transaction_risk(transaction_id);
CREATE INDEX idx_transaction_risk_score ON transaction_risk(risk_score);
CREATE INDEX idx_transaction_risk_compliance ON transaction_risk(compliance_status);

COMMENT ON TABLE transaction_risk IS 'Risk scoring and compliance status for transactions';

-- ============================================================================
-- TABLE: transaction_retry (Retry logic - already exists, ensure structure)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_retry (
    transaction_id BIGINT PRIMARY KEY,
    
    retry_count INTEGER DEFAULT 0,
    max_retry INTEGER DEFAULT 3,
    last_failure_reason TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_retry_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_retry_transaction_id ON transaction_retry(transaction_id);

COMMENT ON TABLE transaction_retry IS 'Retry logic and failure tracking for transactions';

-- ============================================================================
-- TABLE: transaction_authorization_info (Auth details from main table)
-- ============================================================================
CREATE TABLE IF NOT EXISTS transaction_authorization_info (
    transaction_id BIGINT PRIMARY KEY,
    
    authorization_method VARCHAR(20),
    actual_authorization_method VARCHAR(20),
    authorization_required BOOLEAN DEFAULT FALSE,
    authorization_level VARCHAR(20),
    phone_number_used_for_auth VARCHAR(20),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_auth_info_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_auth_info_transaction_id ON transaction_authorization_info(transaction_id);

COMMENT ON TABLE transaction_authorization_info IS 'Authorization method details from transaction';

-- ============================================================================
-- SECTION 2: DATA MIGRATION FROM EXISTING transactions TABLE
-- ============================================================================

-- Migrate fee data
INSERT INTO transaction_fees (
    transaction_id, fee, fee_amount, fee_currency, fee_type, service_fee,
    total_amount, net_amount, fx_fee, exchange_rate, fx_rate, fx_rate_provider
)
SELECT 
    id, 
    COALESCE(fee, 0),
    COALESCE(fee_amount, 0),
    fee_currency,
    fee_type,
    COALESCE(service_fee, 0),
    total_amount,
    net_amount,
    COALESCE(fx_fee, 0),
    exchange_rate,
    fx_rate,
    fx_rate_provider
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_fees);

-- Migrate timeline data
INSERT INTO transaction_timeline (
    transaction_id, initiated_at, initiated_by, pending_at, processing_at,
    authorized_at, processed_at, completed_at, failed_at, cancelled_at,
    reversed_at, refunded_at, under_review_at, on_hold_at, expired_at,
    last_updated_by
)
SELECT 
    id,
    initiated_at,
    initiated_by,
    pending_at,
    processing_at,
    authorized_at,
    processed_at,
    completed_at,
    failed_at,
    cancelled_at,
    reversed_at,
    refunded_at,
    under_review_at,
    on_hold_at,
    expired_at,
    last_updated_by
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_timeline);

-- Migrate metadata
INSERT INTO transaction_metadata (
    transaction_id, device_info, ip_address, payment_method, metadata
)
SELECT 
    id,
    device_info,
    ip_address,
    payment_method,
    metadata
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_metadata);

-- Migrate risk data
INSERT INTO transaction_risk (
    transaction_id, risk_score, compliance_status, route, ip_address, device_info
)
SELECT 
    id,
    risk_score,
    compliance_status,
    route,
    ip_address,
    device_info
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_risk);

-- Migrate retry data
INSERT INTO transaction_retry (
    transaction_id, retry_count, max_retry, last_failure_reason
)
SELECT 
    id,
    COALESCE(retry_count, 0),
    COALESCE(max_retry, 3),
    failure_reason
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_retry);

-- Migrate authorization info
INSERT INTO transaction_authorization_info (
    transaction_id, authorization_method, actual_authorization_method,
    authorization_required, authorization_level, phone_number_used_for_auth
)
SELECT 
    id,
    authorization_method,
    actual_authorization_method,
    authorization_required,
    authorization_level,
    phone_number_used_for_auth
FROM transactions
WHERE id NOT IN (SELECT transaction_id FROM transaction_authorization_info);

-- ============================================================================
-- SECTION 3: DROP COLUMNS FROM transactions TABLE (Optional - for clean separation)
-- ============================================================================
-- NOTE: Comment out if you want to keep backward compatibility temporarily

-- Drop fee columns (now in transaction_fees)
ALTER TABLE transactions DROP COLUMN IF EXISTS fee;
ALTER TABLE transactions DROP COLUMN IF EXISTS fee_amount;
ALTER TABLE transactions DROP COLUMN IF EXISTS fee_currency;
ALTER TABLE transactions DROP COLUMN IF EXISTS fee_type;
ALTER TABLE transactions DROP COLUMN IF EXISTS service_fee;
ALTER TABLE transactions DROP COLUMN IF EXISTS total_amount;
ALTER TABLE transactions DROP COLUMN IF EXISTS net_amount;
ALTER TABLE transactions DROP COLUMN IF EXISTS fx_fee;
ALTER TABLE transactions DROP COLUMN IF EXISTS exchange_rate;
ALTER TABLE transactions DROP COLUMN IF EXISTS fx_rate;
ALTER TABLE transactions DROP COLUMN IF EXISTS fx_rate_provider;

-- Drop timeline columns (now in transaction_timeline)
ALTER TABLE transactions DROP COLUMN IF EXISTS initiated_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS initiated_by;
ALTER TABLE transactions DROP COLUMN IF EXISTS pending_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS processing_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS authorized_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS processed_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS completed_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS failed_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS cancelled_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS reversed_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS refunded_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS under_review_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS on_hold_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS expired_at;
ALTER TABLE transactions DROP COLUMN IF EXISTS last_updated_by;

-- Drop metadata columns (now in transaction_metadata)
ALTER TABLE transactions DROP COLUMN IF EXISTS device_info;
ALTER TABLE transactions DROP COLUMN IF EXISTS ip_address;
ALTER TABLE transactions DROP COLUMN IF EXISTS payment_method;
ALTER TABLE transactions DROP COLUMN IF EXISTS metadata;

-- Drop risk columns (now in transaction_risk)
ALTER TABLE transactions DROP COLUMN IF EXISTS risk_score;
ALTER TABLE transactions DROP COLUMN IF EXISTS compliance_status;
ALTER TABLE transactions DROP COLUMN IF EXISTS route;

-- Drop retry columns (now in transaction_retry)
ALTER TABLE transactions DROP COLUMN IF EXISTS retry_count;
ALTER TABLE transactions DROP COLUMN IF EXISTS max_retry;
ALTER TABLE transactions DROP COLUMN IF EXISTS failure_reason;

-- Drop authorization columns (now in transaction_authorization_info)
ALTER TABLE transactions DROP COLUMN IF EXISTS authorization_method;
ALTER TABLE transactions DROP COLUMN IF EXISTS actual_authorization_method;
ALTER TABLE transactions DROP COLUMN IF EXISTS authorization_required;
ALTER TABLE transactions DROP COLUMN IF EXISTS authorization_level;
ALTER TABLE transactions DROP COLUMN IF EXISTS phone_number_used_for_auth;

-- ============================================================================
-- SECTION 4: UPDATE TRIGGERS
-- ============================================================================

-- Update trigger for transaction_fees
CREATE OR REPLACE FUNCTION update_transaction_fees_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_transaction_fees_updated_at 
BEFORE UPDATE ON transaction_fees
FOR EACH ROW EXECUTE FUNCTION update_transaction_fees_timestamp();

-- Update trigger for transaction_risk
CREATE TRIGGER update_transaction_risk_updated_at 
BEFORE UPDATE ON transaction_risk
FOR EACH ROW EXECUTE FUNCTION update_transaction_fees_timestamp();

-- Update trigger for transaction_retry
CREATE TRIGGER update_transaction_retry_updated_at 
BEFORE UPDATE ON transaction_retry
FOR EACH ROW EXECUTE FUNCTION update_transaction_fees_timestamp();

-- ============================================================================
-- SECTION 5: UPDATED VIEWS (Maintain compatibility)
-- ============================================================================

-- Drop old views
DROP VIEW IF EXISTS v_completed_transactions CASCADE;
DROP VIEW IF EXISTS v_pending_transactions CASCADE;
DROP VIEW IF EXISTS v_daily_transaction_summary CASCADE;

-- Recreate views with JOIN to new tables
CREATE OR REPLACE VIEW v_completed_transactions AS
SELECT 
    t.*,
    tl.completed_at,
    tf.total_amount,
    tf.net_amount
FROM transactions t
LEFT JOIN transaction_timeline tl ON t.id = tl.transaction_id
LEFT JOIN transaction_fees tf ON t.id = tf.transaction_id
WHERE t.status = 'COMPLETED'
ORDER BY tl.completed_at DESC;

CREATE OR REPLACE VIEW v_pending_transactions AS
SELECT 
    t.*,
    tm.device_info,
    tm.ip_address
FROM transactions t
LEFT JOIN transaction_metadata tm ON t.id = tm.transaction_id
WHERE t.status IN ('PENDING', 'PROCESSING')
ORDER BY t.created_at ASC;

CREATE OR REPLACE VIEW v_daily_transaction_summary AS
SELECT 
    DATE(t.created_at) as transaction_date,
    t.type,
    t.status,
    COUNT(*) as transaction_count,
    SUM(t.amount) as total_amount,
    SUM(COALESCE(tf.fee, 0)) as total_fees,
    AVG(t.amount) as average_amount
FROM transactions t
LEFT JOIN transaction_fees tf ON t.id = tf.transaction_id
GROUP BY DATE(t.created_at), t.type, t.status;

-- ============================================================================
-- SECTION 6: HELPER FUNCTION FOR COMPLETE TRANSACTION DATA
-- ============================================================================

-- Function to get complete transaction with all related data
CREATE OR REPLACE FUNCTION get_complete_transaction(tx_id BIGINT)
RETURNS TABLE (
    -- Core transaction
    id BIGINT,
    reference VARCHAR(100),
    type VARCHAR(20),
    status VARCHAR(20),
    amount DECIMAL(19,4),
    currency VARCHAR(10),
    -- Fee data
    total_amount DECIMAL(19,4),
    net_amount DECIMAL(19,4),
    fee DECIMAL(19,4),
    -- Timeline
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    -- Risk
    risk_score DECIMAL(5,2),
    -- Metadata
    device_info TEXT,
    ip_address VARCHAR(50)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.id,
        t.reference,
        t.type,
        t.status,
        t.amount,
        t.currency,
        tf.total_amount,
        tf.net_amount,
        tf.fee,
        t.created_at,
        tl.completed_at,
        tr.risk_score,
        tm.device_info,
        tm.ip_address
    FROM transactions t
    LEFT JOIN transaction_fees tf ON t.id = tf.transaction_id
    LEFT JOIN transaction_timeline tl ON t.id = tl.transaction_id
    LEFT JOIN transaction_risk tr ON t.id = tr.transaction_id
    LEFT JOIN transaction_metadata tm ON t.id = tm.transaction_id
    WHERE t.id = tx_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- END OF REFACTORING MIGRATION
-- ============================================================================
