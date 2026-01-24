-- ============================================================================
-- ZAPHIRA Platform - TRANSACTION SERVICE Schema
-- ============================================================================
-- Database: zaphira_transactions_db
-- Description: Transactions, settlements, refunds, authorizations, validations
-- Dependencies: References wallets from zaphira_wallets_db, users from zaphira_users_db
-- ============================================================================

\c zaphira_transactions_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE transaction_type_enum AS ENUM ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'REVERSAL', 'FEE', 'COMMISSION');
CREATE TYPE transaction_status_enum AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED', 'REFUNDED', 'ON_HOLD', 'UNDER_REVIEW', 'EXPIRED');
CREATE TYPE authorization_method_enum AS ENUM ('PIN', 'OTP', 'BIOMETRIC', 'TWO_FACTOR', 'NONE');
CREATE TYPE channel_enum AS ENUM ('WEB', 'MOBILE', 'API', 'USSD', 'AGENT', 'ATM');
CREATE TYPE settlement_status_enum AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'RETRYING');
CREATE TYPE refund_type_enum AS ENUM ('FULL', 'PARTIAL', 'FEE_ONLY');
CREATE TYPE authorization_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED');

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: transactions (Core transaction table)
-- ============================================================================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    
    -- Wallet references (logical FK - no actual FK constraint for microservices)
    sender_wallet_id BIGINT,
    receiver_wallet_id BIGINT,
    sender_wallet_number VARCHAR(8),
    receiver_wallet_number VARCHAR(8),
    
    -- User reference
    user_id BIGINT,
    
    -- Transaction details
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    category VARCHAR(50),
    
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'XAF',
    
    -- Fees
    fee DECIMAL(19,4) DEFAULT 0,
    fee_amount DECIMAL(19,4) DEFAULT 0,
    fee_currency VARCHAR(10),
    fee_type VARCHAR(50),
    service_fee DECIMAL(19,4) DEFAULT 0,
    
    total_amount DECIMAL(19,4) NOT NULL,
    net_amount DECIMAL(19,4),
    
    -- Foreign exchange
    exchange_rate DECIMAL(19,6),
    fx_rate DECIMAL(19,6),
    fx_fee DECIMAL(19,4) DEFAULT 0,
    fx_rate_provider VARCHAR(50),
    
    -- References and descriptions
    reference VARCHAR(100) UNIQUE,
    transaction_reference VARCHAR(100),
    description VARCHAR(500),
    
    -- Authorization
    authorization_method VARCHAR(20),
    actual_authorization_method VARCHAR(20),
    authorization_required BOOLEAN DEFAULT FALSE,
    authorization_level VARCHAR(20),
    phone_number_used_for_auth VARCHAR(20),
    
    -- Channel and device
    channel VARCHAR(20),
    payment_method VARCHAR(50),
    device_info TEXT,
    ip_address VARCHAR(50),
    
    -- Risk and compliance
    risk_score DECIMAL(5,2),
    compliance_status VARCHAR(20),
    route VARCHAR(100),
    
    -- Flags
    scheduled BOOLEAN DEFAULT FALSE,
    scheduled_for TIMESTAMP,
    is_refunded BOOLEAN DEFAULT FALSE,
    is_reversed BOOLEAN DEFAULT FALSE,
    
    -- Related transactions
    related_transaction_id BIGINT,
    
    -- Retry logic
    max_retry INTEGER DEFAULT 3,
    retry_count INTEGER DEFAULT 0,
    failure_reason TEXT,
    
    -- Metadata
    metadata TEXT,
    
    -- Version for optimistic locking
    version BIGINT DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(100),
    
    CONSTRAINT chk_transaction_type CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'REVERSAL', 'FEE', 'COMMISSION')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED', 'REFUNDED', 'ON_HOLD', 'UNDER_REVIEW', 'EXPIRED')),
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_fee CHECK (fee >= 0)
);

CREATE INDEX idx_transactions_sender_wallet ON transactions(sender_wallet_id);
CREATE INDEX idx_transactions_receiver_wallet ON transactions(receiver_wallet_id);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_reference ON transactions(reference);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_completed_at ON transactions(completed_at);
CREATE INDEX idx_transactions_sender_wallet_number ON transactions(sender_wallet_number);
CREATE INDEX idx_transactions_receiver_wallet_number ON transactions(receiver_wallet_number);

COMMENT ON TABLE transactions IS 'Core transaction records';

-- ============================================================================
-- TABLE: transaction_status_history (Status change tracking)
-- ============================================================================
CREATE TABLE transaction_status_history (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    
    reason VARCHAR(500),
    notes TEXT,
    
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_status_history_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_status_history_transaction_id ON transaction_status_history(transaction_id);
CREATE INDEX idx_transaction_status_history_changed_at ON transaction_status_history(changed_at);

COMMENT ON TABLE transaction_status_history IS 'Audit trail of transaction status changes';

-- ============================================================================
-- TABLE: transaction_states (Alternative status tracking)
-- ============================================================================
CREATE TABLE transaction_states (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_states_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_states_transaction_id ON transaction_states(transaction_id);

COMMENT ON TABLE transaction_states IS 'Alternative transaction state tracking';

-- ============================================================================
-- TABLE: transaction_authorizations (2FA and authorization)
-- ============================================================================
CREATE TABLE transaction_authorizations (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    method VARCHAR(20) NOT NULL,
    challenge_code VARCHAR(100),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requested_by VARCHAR(100),
    
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    
    rejection_reason VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_transaction_auth_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT chk_auth_method CHECK (method IN ('PIN', 'OTP', 'BIOMETRIC', 'TWO_FACTOR', 'NONE')),
    CONSTRAINT chk_auth_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_transaction_auth_transaction_id ON transaction_authorizations(transaction_id);
CREATE INDEX idx_transaction_auth_status ON transaction_authorizations(status);

COMMENT ON TABLE transaction_authorizations IS 'Transaction authorization and 2FA challenges';

-- ============================================================================
-- TABLE: transaction_settlements (Settlement processing)
-- ============================================================================
CREATE TABLE transaction_settlements (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Amounts
    original_amount DECIMAL(19,4) NOT NULL,
    original_currency VARCHAR(10) NOT NULL,
    settled_amount DECIMAL(19,4),
    settlement_currency VARCHAR(10),
    net_amount DECIMAL(19,4),
    
    -- Fees
    service_fee DECIMAL(19,4) DEFAULT 0,
    fx_fee DECIMAL(19,4) DEFAULT 0,
    total_fees DECIMAL(19,4) DEFAULT 0,
    
    -- Exchange rate
    fx_rate DECIMAL(19,6),
    fx_rate_provider VARCHAR(50),
    
    -- Retry logic
    retry_count INTEGER DEFAULT 0,
    failure_reason TEXT,
    
    -- Timestamps
    settlement_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_settlement_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT chk_settlement_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'RETRYING')),
    CONSTRAINT uk_transaction_settlement UNIQUE (transaction_id)
);

CREATE INDEX idx_transaction_settlements_transaction_id ON transaction_settlements(transaction_id);
CREATE INDEX idx_transaction_settlements_status ON transaction_settlements(status);
CREATE INDEX idx_transaction_settlements_date ON transaction_settlements(settlement_date);

COMMENT ON TABLE transaction_settlements IS 'Settlement processing for transactions';

-- ============================================================================
-- TABLE: transaction_refunds (Refund tracking)
-- ============================================================================
CREATE TABLE transaction_refunds (
    id BIGSERIAL PRIMARY KEY,
    original_transaction_id BIGINT NOT NULL,
    refund_transaction_id BIGINT,
    
    refund_reference VARCHAR(100) UNIQUE,
    
    refund_amount DECIMAL(19,4) NOT NULL,
    refund_fees DECIMAL(19,4) DEFAULT 0,
    total_refund_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    
    refund_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    
    performed_by_user_id BIGINT,
    performed_by_role VARCHAR(50),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_refund_original FOREIGN KEY (original_transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_refund_refund FOREIGN KEY (refund_transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,
    CONSTRAINT chk_refund_type CHECK (refund_type IN ('FULL', 'PARTIAL', 'FEE_ONLY')),
    CONSTRAINT chk_refund_amount CHECK (refund_amount > 0)
);

CREATE INDEX idx_transaction_refunds_original_id ON transaction_refunds(original_transaction_id);
CREATE INDEX idx_transaction_refunds_refund_id ON transaction_refunds(refund_transaction_id);
CREATE INDEX idx_transaction_refunds_reference ON transaction_refunds(refund_reference);

COMMENT ON TABLE transaction_refunds IS 'Transaction refund records';

-- ============================================================================
-- TABLE: transaction_audit_log (Detailed audit trail)
-- ============================================================================
CREATE TABLE transaction_audit_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT,
    
    action_type VARCHAR(50) NOT NULL,
    result VARCHAR(20),
    
    -- Actor information
    actor_user_id BIGINT,
    actor_email VARCHAR(100),
    actor_role VARCHAR(50),
    
    -- Financial details
    amount DECIMAL(19,4),
    currency VARCHAR(10),
    
    -- Status tracking
    status_before VARCHAR(20),
    status_after VARCHAR(20),
    
    reason VARCHAR(500),
    details TEXT,
    error_message TEXT,
    
    -- Request tracking
    request_id VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    device_id VARCHAR(100),
    
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_audit_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_audit_transaction_id ON transaction_audit_log(transaction_id);
CREATE INDEX idx_transaction_audit_timestamp ON transaction_audit_log(timestamp);
CREATE INDEX idx_transaction_audit_actor ON transaction_audit_log(actor_user_id);
CREATE INDEX idx_transaction_audit_action ON transaction_audit_log(action_type);

COMMENT ON TABLE transaction_audit_log IS 'Comprehensive audit trail for all transaction actions';

-- ============================================================================
-- TABLE: scheduled_transactions (Scheduled/recurring transactions)
-- ============================================================================
CREATE TABLE scheduled_transactions (
    id BIGSERIAL PRIMARY KEY,
    
    sender_wallet_number VARCHAR(8) NOT NULL,
    receiver_wallet_number VARCHAR(8) NOT NULL,
    
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'XAF',
    
    type VARCHAR(20) NOT NULL,
    channel VARCHAR(20),
    
    description VARCHAR(500),
    
    scheduled_for TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    executed_transaction_id BIGINT,
    last_execution_at TIMESTAMP,
    last_error TEXT,
    
    requested_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_scheduled_transaction_executed FOREIGN KEY (executed_transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,
    CONSTRAINT chk_scheduled_amount CHECK (amount > 0)
);

CREATE INDEX idx_scheduled_transactions_scheduled_for ON scheduled_transactions(scheduled_for);
CREATE INDEX idx_scheduled_transactions_status ON scheduled_transactions(status);
CREATE INDEX idx_scheduled_transactions_sender ON scheduled_transactions(sender_wallet_number);

COMMENT ON TABLE scheduled_transactions IS 'Scheduled and recurring transaction requests';

-- ============================================================================
-- TABLE: validation_requests (Transaction validation workflows)
-- ============================================================================
CREATE TABLE validation_requests (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    
    correlation_id VARCHAR(100),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_validation_request_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT uk_validation_transaction UNIQUE (transaction_id)
);

CREATE INDEX idx_validation_requests_transaction_id ON validation_requests(transaction_id);
CREATE INDEX idx_validation_requests_status ON validation_requests(status);
CREATE INDEX idx_validation_requests_correlation ON validation_requests(correlation_id);

COMMENT ON TABLE validation_requests IS 'Transaction validation and approval workflows';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_transaction_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_transactions_updated_at BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_transaction_updated_at();

CREATE TRIGGER update_transaction_settlements_updated_at BEFORE UPDATE ON transaction_settlements
    FOR EACH ROW EXECUTE FUNCTION update_transaction_updated_at();

-- Log status changes
CREATE OR REPLACE FUNCTION log_transaction_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO transaction_status_history (transaction_id, previous_status, new_status, changed_by)
        VALUES (NEW.id, OLD.status, NEW.status, NEW.last_updated_by);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_transaction_status_changes AFTER UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION log_transaction_status_change();

-- Calculate total amount
CREATE OR REPLACE FUNCTION calculate_transaction_total()
RETURNS TRIGGER AS $$
BEGIN
    NEW.total_amount = NEW.amount + COALESCE(NEW.fee, 0) + COALESCE(NEW.service_fee, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_total_amount BEFORE INSERT OR UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION calculate_transaction_total();

-- ============================================================================
-- SECTION 4: UTILITY FUNCTIONS
-- ============================================================================

-- Generate unique transaction reference
CREATE OR REPLACE FUNCTION generate_transaction_reference()
RETURNS VARCHAR(100) AS $$
DECLARE
    new_ref VARCHAR(100);
    exists BOOLEAN;
BEGIN
    LOOP
        new_ref := 'TXN-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-' || 
                   LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        SELECT EXISTS(SELECT 1 FROM transactions WHERE reference = new_ref) INTO exists;
        
        IF NOT exists THEN
            RETURN new_ref;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 5: VIEWS
-- ============================================================================

-- Completed transactions
CREATE OR REPLACE VIEW v_completed_transactions AS
SELECT * FROM transactions
WHERE status = 'COMPLETED'
ORDER BY completed_at DESC;

-- Pending transactions
CREATE OR REPLACE VIEW v_pending_transactions AS
SELECT * FROM transactions
WHERE status IN ('PENDING', 'PROCESSING')
ORDER BY created_at ASC;

-- Transaction summary by day
CREATE OR REPLACE VIEW v_daily_transaction_summary AS
SELECT 
    DATE(created_at) as transaction_date,
    type,
    status,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    SUM(fee) as total_fees,
    AVG(amount) as average_amount
FROM transactions
GROUP BY DATE(created_at), type, status;

-- ============================================================================
-- END OF TRANSACTION SERVICE SCHEMA
-- ============================================================================
