-- ============================================================================
-- ZAPHIRA Platform - WALLET SERVICE Schema
-- ============================================================================
-- Database: zaphira_wallets_db
-- Description: Wallet management, sub-wallets, permissions, status history
-- Dependencies: References users(user_id) from zaphira_users_db
-- ============================================================================

\c zaphira_wallets_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE wallet_type_enum AS ENUM ('PERSONAL', 'BUSINESS', 'MERCHANT', 'SAVINGS', 'AGENT');
CREATE TYPE wallet_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'FROZEN', 'CLOSED');
CREATE TYPE sub_wallet_type_enum AS ENUM ('CHECKING', 'SAVINGS', 'BUSINESS', 'INVESTMENT', 'ESCROW');
CREATE TYPE permission_type_enum AS ENUM ('SEND', 'RECEIVE', 'WITHDRAW', 'TRANSFER', 'VIEW_BALANCE', 'VIEW_HISTORY');
CREATE TYPE currency_enum AS ENUM ('XAF', 'USD', 'EUR', 'GBP', 'NGN', 'GHS');

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: wallets (Main wallet table)
-- ============================================================================
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_number VARCHAR(8) NOT NULL UNIQUE,
    
    type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Merchant fields
    merchant_name VARCHAR(200),
    merchant_code VARCHAR(6),
    
    -- Balances
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    blocked_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    
    -- Limits
    daily_limit DECIMAL(19,4),
    monthly_limit DECIMAL(19,4),
    daily_spent DECIMAL(19,4) NOT NULL DEFAULT 0,
    monthly_spent DECIMAL(19,4) NOT NULL DEFAULT 0,
    last_limit_reset TIMESTAMP,
    
    -- Status flags
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Freeze information
    frozen_reason VARCHAR(500),
    frozen_at TIMESTAMP,
    frozen_by VARCHAR(100),
    
    -- Currency
    currency VARCHAR(10) NOT NULL DEFAULT 'XAF',
    
    -- Version for optimistic locking
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Metadata
    metadata TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    
    CONSTRAINT chk_wallet_type CHECK (type IN ('PERSONAL', 'BUSINESS', 'MERCHANT', 'SAVINGS', 'AGENT')),
    CONSTRAINT chk_wallet_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_wallet_balances CHECK (
        available_balance >= 0 AND 
        blocked_balance >= 0 AND 
        total_balance = available_balance + blocked_balance
    )
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_wallet_number ON wallets(wallet_number);
CREATE INDEX idx_wallets_status ON wallets(status);
CREATE INDEX idx_wallets_type ON wallets(type);
CREATE INDEX idx_wallets_merchant_code ON wallets(merchant_code);

COMMENT ON TABLE wallets IS 'Main wallet accounts for users';

-- ============================================================================
-- TABLE: sub_wallets (Sub-accounts within wallets)
-- ============================================================================
CREATE TABLE sub_wallets (
    id BIGSERIAL PRIMARY KEY,
    
    type VARCHAR(20) NOT NULL,
    sub_wallet_name VARCHAR(100),
    
    -- Balances
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    blocked_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    
    -- Currency and limits
    currency VARCHAR(10) NOT NULL DEFAULT 'XAF',
    daily_limit DECIMAL(19,4),
    monthly_limit DECIMAL(19,4),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Version for optimistic locking
    version BIGINT,
    
    -- Metadata
    metadata TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_sub_wallet_type CHECK (type IN ('CHECKING', 'SAVINGS', 'BUSINESS', 'INVESTMENT', 'ESCROW')),
    CONSTRAINT chk_sub_wallet_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_sub_wallet_balances CHECK (
        available_balance >= 0 AND 
        blocked_balance >= 0 AND 
        total_balance = available_balance + blocked_balance
    )
);

CREATE INDEX idx_sub_wallets_status ON sub_wallets(status);
CREATE INDEX idx_sub_wallets_type ON sub_wallets(type);

COMMENT ON TABLE sub_wallets IS 'Sub-wallets for categorizing funds';

-- ============================================================================
-- TABLE: wallet_subwallet (Many-to-Many relationship)
-- ============================================================================
CREATE TABLE wallet_subwallet (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    subwallet_id BIGINT NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wallet_subwallet_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_subwallet_subwallet FOREIGN KEY (subwallet_id) REFERENCES sub_wallets(id) ON DELETE CASCADE,
    CONSTRAINT uk_wallet_subwallet UNIQUE (wallet_id, subwallet_id)
);

CREATE INDEX idx_wallet_subwallet_wallet_id ON wallet_subwallet(wallet_id);
CREATE INDEX idx_wallet_subwallet_subwallet_id ON wallet_subwallet(subwallet_id);

COMMENT ON TABLE wallet_subwallet IS 'Junction table linking wallets to sub-wallets';

-- ============================================================================
-- TABLE: wallet_permissions (Wallet permissions and capabilities)
-- ============================================================================
CREATE TABLE wallet_permissions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    
    permission_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Transaction limits
    max_amount DECIMAL(19,4),
    daily_limit DECIMAL(19,4),
    
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wallet_permissions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT chk_permission_type CHECK (permission_type IN ('SEND', 'RECEIVE', 'WITHDRAW', 'TRANSFER', 'VIEW_BALANCE', 'VIEW_HISTORY')),
    CONSTRAINT uk_wallet_permission UNIQUE (wallet_id, permission_type)
);

CREATE INDEX idx_wallet_permissions_wallet_id ON wallet_permissions(wallet_id);
CREATE INDEX idx_wallet_permissions_type ON wallet_permissions(permission_type);

COMMENT ON TABLE wallet_permissions IS 'Granular permissions for wallet operations';

-- ============================================================================
-- TABLE: wallet_status_history (Status change audit trail)
-- ============================================================================
CREATE TABLE wallet_status_history (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    
    reason VARCHAR(500),
    notes TEXT,
    
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wallet_status_history_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

CREATE INDEX idx_wallet_status_history_wallet_id ON wallet_status_history(wallet_id);
CREATE INDEX idx_wallet_status_history_changed_at ON wallet_status_history(changed_at);

COMMENT ON TABLE wallet_status_history IS 'Audit trail of wallet status changes';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_wallet_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_wallets_updated_at BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_wallet_updated_at();

CREATE TRIGGER update_sub_wallets_updated_at BEFORE UPDATE ON sub_wallets
    FOR EACH ROW EXECUTE FUNCTION update_wallet_updated_at();

CREATE TRIGGER update_wallet_permissions_updated_at BEFORE UPDATE ON wallet_permissions
    FOR EACH ROW EXECUTE FUNCTION update_wallet_updated_at();

-- Function to calculate total balance
CREATE OR REPLACE FUNCTION update_wallet_total_balance()
RETURNS TRIGGER AS $$
BEGIN
    NEW.total_balance = NEW.available_balance + NEW.blocked_balance;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_wallet_balance BEFORE INSERT OR UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_wallet_total_balance();

CREATE TRIGGER calculate_sub_wallet_balance BEFORE INSERT OR UPDATE ON sub_wallets
    FOR EACH ROW EXECUTE FUNCTION update_wallet_total_balance();

-- Function to log wallet status changes
CREATE OR REPLACE FUNCTION log_wallet_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO wallet_status_history (wallet_id, previous_status, new_status, changed_by)
        VALUES (NEW.id, OLD.status, NEW.status, NEW.frozen_by);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_wallet_status_changes AFTER UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION log_wallet_status_change();

-- ============================================================================
-- SECTION 4: UTILITY FUNCTIONS
-- ============================================================================

-- Function to generate unique wallet number
CREATE OR REPLACE FUNCTION generate_wallet_number()
RETURNS VARCHAR(8) AS $$
DECLARE
    new_number VARCHAR(8);
    exists BOOLEAN;
BEGIN
    LOOP
        new_number := LPAD(FLOOR(RANDOM() * 100000000)::TEXT, 8, '0');
        
        SELECT EXISTS(SELECT 1 FROM wallets WHERE wallet_number = new_number) INTO exists;
        
        IF NOT exists THEN
            RETURN new_number;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to check if wallet has sufficient balance
CREATE OR REPLACE FUNCTION has_sufficient_balance(
    p_wallet_id BIGINT,
    p_amount DECIMAL(19,4)
)
RETURNS BOOLEAN AS $$
DECLARE
    v_available_balance DECIMAL(19,4);
BEGIN
    SELECT available_balance INTO v_available_balance
    FROM wallets
    WHERE id = p_wallet_id;
    
    RETURN v_available_balance >= p_amount;
END;
$$ LANGUAGE plpgsql;

-- Function to block amount in wallet
CREATE OR REPLACE FUNCTION block_amount(
    p_wallet_id BIGINT,
    p_amount DECIMAL(19,4)
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE wallets
    SET available_balance = available_balance - p_amount,
        blocked_balance = blocked_balance + p_amount,
        version = version + 1
    WHERE id = p_wallet_id
      AND available_balance >= p_amount;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to unblock amount in wallet
CREATE OR REPLACE FUNCTION unblock_amount(
    p_wallet_id BIGINT,
    p_amount DECIMAL(19,4)
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE wallets
    SET available_balance = available_balance + p_amount,
        blocked_balance = blocked_balance - p_amount,
        version = version + 1
    WHERE id = p_wallet_id
      AND blocked_balance >= p_amount;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 5: VIEWS
-- ============================================================================

-- View: Active wallets with user info
CREATE OR REPLACE VIEW v_active_wallets AS
SELECT 
    w.id,
    w.user_id,
    w.wallet_number,
    w.type,
    w.status,
    w.available_balance,
    w.blocked_balance,
    w.total_balance,
    w.currency,
    w.daily_limit,
    w.monthly_limit,
    w.daily_spent,
    w.monthly_spent,
    w.is_primary,
    w.created_at
FROM wallets w
WHERE w.status = 'ACTIVE'
  AND w.active = TRUE;

-- View: Wallet summary
CREATE OR REPLACE VIEW v_wallet_summary AS
SELECT 
    w.id as wallet_id,
    w.wallet_number,
    w.user_id,
    w.type,
    w.status,
    w.available_balance,
    w.blocked_balance,
    w.total_balance,
    w.currency,
    COUNT(ws.subwallet_id) as subwallet_count,
    COUNT(wp.id) as permission_count
FROM wallets w
LEFT JOIN wallet_subwallet ws ON w.id = ws.wallet_id
LEFT JOIN wallet_permissions wp ON w.id = wp.wallet_id AND wp.enabled = TRUE
GROUP BY w.id;

-- ============================================================================
-- END OF WALLET SERVICE SCHEMA
-- ============================================================================
