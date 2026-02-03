-- ============================================================================
-- LOT 1 - Transaction Core Schema
-- ============================================================================
-- Created: 2026-02-03
-- Purpose: Create transactions_core table for lightweight transaction operations
-- Dependencies: wallet-service (wallets table must exist)

-- ============================================================================
-- SWITCH TO TRANSACTION DATABASE
-- ============================================================================
\c transaction_db;

-- ============================================================================
-- CREATE TABLE: transactions_core
-- ============================================================================
-- Minimal transaction entity for LOT 1
-- Supports: P2P Transfer, Deposit, Withdrawal
-- Philosophy: Start simple, extend later with LOT 2+

CREATE TABLE IF NOT EXISTS transactions_core (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- Reference unique
    reference VARCHAR(50) NOT NULL UNIQUE,
    
    -- Wallets impliqués
    sender_wallet_id BIGINT,
    receiver_wallet_id BIGINT,
    
    -- Montant et devise
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    
    -- Type de transaction
    type VARCHAR(20) NOT NULL CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'REVERSAL')),
    
    -- Statut
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    
    -- Description optionnelle
    description VARCHAR(255),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT check_wallets CHECK (
        (type = 'TRANSFER' AND sender_wallet_id IS NOT NULL AND receiver_wallet_id IS NOT NULL) OR
        (type = 'DEPOSIT' AND receiver_wallet_id IS NOT NULL) OR
        (type = 'WITHDRAWAL' AND sender_wallet_id IS NOT NULL) OR
        (type = 'PAYMENT' AND sender_wallet_id IS NOT NULL AND receiver_wallet_id IS NOT NULL) OR
        (type = 'REFUND' AND receiver_wallet_id IS NOT NULL) OR
        (type = 'REVERSAL' AND sender_wallet_id IS NOT NULL AND receiver_wallet_id IS NOT NULL)
    )
);

-- ============================================================================
-- CREATE INDEXES
-- ============================================================================
-- Performance optimizations for frequent queries

-- Index unique sur reference (déjà créé par UNIQUE constraint)
-- CREATE UNIQUE INDEX idx_core_reference ON transactions_core(reference);

-- Index sur sender_wallet_id pour requêtes "transactions envoyées"
CREATE INDEX IF NOT EXISTS idx_core_sender ON transactions_core(sender_wallet_id);

-- Index sur receiver_wallet_id pour requêtes "transactions reçues"
CREATE INDEX IF NOT EXISTS idx_core_receiver ON transactions_core(receiver_wallet_id);

-- Index sur status pour filtrer par statut
CREATE INDEX IF NOT EXISTS idx_core_status ON transactions_core(status);

-- Index sur created_at pour tri chronologique
CREATE INDEX IF NOT EXISTS idx_core_created ON transactions_core(created_at DESC);

-- Index composite pour requêtes wallet + status
CREATE INDEX IF NOT EXISTS idx_core_sender_status ON transactions_core(sender_wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_core_receiver_status ON transactions_core(receiver_wallet_id, status);

-- Index composite pour requêtes wallet + type
CREATE INDEX IF NOT EXISTS idx_core_sender_type ON transactions_core(sender_wallet_id, type);
CREATE INDEX IF NOT EXISTS idx_core_receiver_type ON transactions_core(receiver_wallet_id, type);

-- Index sur type pour statistiques
CREATE INDEX IF NOT EXISTS idx_core_type ON transactions_core(type);

-- ============================================================================
-- CREATE TRIGGER: updated_at auto-update
-- ============================================================================

CREATE OR REPLACE FUNCTION update_transactions_core_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_transactions_core_updated_at ON transactions_core;

CREATE TRIGGER trigger_transactions_core_updated_at
    BEFORE UPDATE ON transactions_core
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_core_updated_at();

-- ============================================================================
-- INSERT SAMPLE DATA (OPTIONAL - for testing)
-- ============================================================================
-- Uncomment to insert test data

/*
-- Sample deposit
INSERT INTO transactions_core (reference, receiver_wallet_id, amount, currency, type, status, description)
VALUES ('TXN-20260203-000001', 1, 1000.00, 'EUR', 'DEPOSIT', 'COMPLETED', 'Initial deposit');

-- Sample transfer
INSERT INTO transactions_core (reference, sender_wallet_id, receiver_wallet_id, amount, currency, type, status, description)
VALUES ('TXN-20260203-000002', 1, 2, 100.00, 'EUR', 'TRANSFER', 'COMPLETED', 'Test transfer');

-- Sample withdrawal
INSERT INTO transactions_core (reference, sender_wallet_id, amount, currency, type, status, description)
VALUES ('TXN-20260203-000003', 1, 50.00, 'EUR', 'WITHDRAWAL', 'COMPLETED', 'Cash withdrawal');
*/

-- ============================================================================
-- VERIFY CREATION
-- ============================================================================

-- Check table exists
SELECT 
    schemaname, 
    tablename, 
    tableowner 
FROM pg_tables 
WHERE tablename = 'transactions_core';

-- Check columns
SELECT 
    column_name, 
    data_type, 
    character_maximum_length,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'transactions_core'
ORDER BY ordinal_position;

-- Check indexes
SELECT 
    indexname, 
    indexdef 
FROM pg_indexes 
WHERE tablename = 'transactions_core'
ORDER BY indexname;

-- Check constraints
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(c.oid) AS constraint_definition
FROM pg_constraint c
JOIN pg_class t ON c.conrelid = t.oid
WHERE t.relname = 'transactions_core';

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================
-- Adjust according to your application user

-- GRANT ALL PRIVILEGES ON transactions_core TO transaction_service_user;
-- GRANT USAGE, SELECT ON SEQUENCE transactions_core_id_seq TO transaction_service_user;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE transactions_core IS 'LOT 1 - Minimal transaction entity for basic operations (Transfer, Deposit, Withdrawal)';
COMMENT ON COLUMN transactions_core.id IS 'Primary key - auto-incrementing ID';
COMMENT ON COLUMN transactions_core.reference IS 'Unique transaction reference (format: TXN-YYYYMMDDHHMMSS-XXXXXX)';
COMMENT ON COLUMN transactions_core.sender_wallet_id IS 'Wallet ID of sender (nullable for deposits)';
COMMENT ON COLUMN transactions_core.receiver_wallet_id IS 'Wallet ID of receiver (nullable for withdrawals)';
COMMENT ON COLUMN transactions_core.amount IS 'Transaction amount (must be positive)';
COMMENT ON COLUMN transactions_core.currency IS 'Currency code (ISO 4217, default EUR)';
COMMENT ON COLUMN transactions_core.type IS 'Transaction type: TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REFUND, REVERSAL';
COMMENT ON COLUMN transactions_core.status IS 'Transaction status: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN transactions_core.description IS 'Optional transaction description';
COMMENT ON COLUMN transactions_core.created_at IS 'Transaction creation timestamp';
COMMENT ON COLUMN transactions_core.completed_at IS 'Transaction completion timestamp (null if not completed)';
COMMENT ON COLUMN transactions_core.updated_at IS 'Last update timestamp (auto-updated by trigger)';

-- ============================================================================
-- STATISTICS
-- ============================================================================

ANALYZE transactions_core;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

\echo '✅ LOT 1 - transactions_core table created successfully!'
\echo ''
\echo 'Table: transactions_core'
\echo 'Indexes: 9 (reference, sender, receiver, status, created_at, composites)'
\echo 'Trigger: updated_at auto-update'
\echo ''
\echo 'Next steps:'
\echo '1. Verify table structure above'
\echo '2. Grant permissions to application user'
\echo '3. Start transaction-service'
\echo '4. Run test script: test_lot1_transaction_core.ps1'
