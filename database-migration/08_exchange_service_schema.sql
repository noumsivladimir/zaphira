-- ============================================================================
-- ZAPHIRA Platform - EXCHANGE SERVICE Schema
-- ============================================================================
-- Database: zaphira_exchange_db
-- Description: Exchange rates, currency conversions, forex data
-- Dependencies: None (independent service)
-- ============================================================================

\c zaphira_exchange_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE rate_provider_enum AS ENUM (
    'INTERNAL', 'CENTRAL_BANK', 'FOREX_API', 
    'AGGREGATED', 'MANUAL', 'THIRD_PARTY'
);

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: exchange_rates (Real-time exchange rates)
-- ============================================================================
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    
    source_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    
    -- Exchange rates
    rate DECIMAL(19,6) NOT NULL,
    bid DECIMAL(19,6), -- Buying rate
    ask DECIMAL(19,6), -- Selling rate
    
    -- Provider information
    provider VARCHAR(50) NOT NULL DEFAULT 'INTERNAL',
    
    -- Validity period
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Additional metadata
    source VARCHAR(100), -- API endpoint or source system
    metadata TEXT,
    
    CONSTRAINT chk_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_bid_positive CHECK (bid IS NULL OR bid > 0),
    CONSTRAINT chk_ask_positive CHECK (ask IS NULL OR ask > 0),
    CONSTRAINT chk_currency_pair CHECK (source_currency <> target_currency)
);

CREATE INDEX idx_exchange_rates_source_target ON exchange_rates(source_currency, target_currency);
CREATE INDEX idx_exchange_rates_provider ON exchange_rates(provider);
CREATE INDEX idx_exchange_rates_created_at ON exchange_rates(created_at);
CREATE INDEX idx_exchange_rates_expires_at ON exchange_rates(expires_at);
CREATE INDEX idx_exchange_rates_active ON exchange_rates(expires_at) WHERE expires_at > CURRENT_TIMESTAMP;

COMMENT ON TABLE exchange_rates IS 'Real-time and historical exchange rates';

-- ============================================================================
-- TABLE: exchange_rate_history (Historical rates archive)
-- ============================================================================
CREATE TABLE exchange_rate_history (
    id BIGSERIAL PRIMARY KEY,
    
    source_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    
    rate DECIMAL(19,6) NOT NULL,
    bid DECIMAL(19,6),
    ask DECIMAL(19,6),
    
    provider VARCHAR(50) NOT NULL,
    
    effective_from TIMESTAMP NOT NULL,
    effective_until TIMESTAMP,
    
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exchange_history_source_target ON exchange_rate_history(source_currency, target_currency);
CREATE INDEX idx_exchange_history_effective_from ON exchange_rate_history(effective_from);
CREATE INDEX idx_exchange_history_archived_at ON exchange_rate_history(archived_at);

COMMENT ON TABLE exchange_rate_history IS 'Historical archive of exchange rates';

-- ============================================================================
-- TABLE: currency_pairs (Supported currency pairs)
-- ============================================================================
CREATE TABLE currency_pairs (
    id BIGSERIAL PRIMARY KEY,
    
    source_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Trading limits
    min_amount DECIMAL(19,4),
    max_amount DECIMAL(19,4),
    
    -- Fee configuration
    base_fee_percentage DECIMAL(5,4) DEFAULT 0,
    min_fee DECIMAL(19,4) DEFAULT 0,
    max_fee DECIMAL(19,4),
    
    -- Update frequency (in minutes)
    update_frequency_minutes INTEGER DEFAULT 60,
    
    -- Provider priority order
    primary_provider VARCHAR(50),
    fallback_provider VARCHAR(50),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_currency_pair UNIQUE (source_currency, target_currency),
    CONSTRAINT chk_currency_pair_different CHECK (source_currency <> target_currency)
);

CREATE INDEX idx_currency_pairs_active ON currency_pairs(is_active);
CREATE INDEX idx_currency_pairs_source ON currency_pairs(source_currency);
CREATE INDEX idx_currency_pairs_target ON currency_pairs(target_currency);

COMMENT ON TABLE currency_pairs IS 'Configuration for supported currency pairs';

-- ============================================================================
-- TABLE: conversion_logs (Conversion transaction logs)
-- ============================================================================
CREATE TABLE conversion_logs (
    id BIGSERIAL PRIMARY KEY,
    
    -- Transaction reference
    transaction_id BIGINT,
    user_id BIGINT,
    
    source_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    
    source_amount DECIMAL(19,4) NOT NULL,
    exchange_rate DECIMAL(19,6) NOT NULL,
    target_amount DECIMAL(19,4) NOT NULL,
    
    fee_amount DECIMAL(19,4) DEFAULT 0,
    fee_currency VARCHAR(10),
    
    provider VARCHAR(50),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversion_logs_transaction_id ON conversion_logs(transaction_id);
CREATE INDEX idx_conversion_logs_user_id ON conversion_logs(user_id);
CREATE INDEX idx_conversion_logs_created_at ON conversion_logs(created_at);
CREATE INDEX idx_conversion_logs_currency_pair ON conversion_logs(source_currency, target_currency);

COMMENT ON TABLE conversion_logs IS 'Audit log of all currency conversions';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_exchange_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_exchange_rates_updated_at BEFORE UPDATE ON exchange_rates
    FOR EACH ROW EXECUTE FUNCTION update_exchange_updated_at();

CREATE TRIGGER update_currency_pairs_updated_at BEFORE UPDATE ON currency_pairs
    FOR EACH ROW EXECUTE FUNCTION update_exchange_updated_at();

-- Archive expired rates
CREATE OR REPLACE FUNCTION archive_expired_rates()
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Move expired rates to history
    INSERT INTO exchange_rate_history (
        source_currency,
        target_currency,
        rate,
        bid,
        ask,
        provider,
        effective_from,
        effective_until
    )
    SELECT 
        source_currency,
        target_currency,
        rate,
        bid,
        ask,
        provider,
        created_at,
        expires_at
    FROM exchange_rates
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete from active table
    DELETE FROM exchange_rates
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- Calculate conversion amount
CREATE OR REPLACE FUNCTION calculate_conversion(
    p_source_currency VARCHAR(10),
    p_target_currency VARCHAR(10),
    p_source_amount DECIMAL(19,4)
)
RETURNS DECIMAL(19,4) AS $$
DECLARE
    v_rate DECIMAL(19,6);
    v_target_amount DECIMAL(19,4);
BEGIN
    -- Get latest active rate
    SELECT rate INTO v_rate
    FROM exchange_rates
    WHERE source_currency = p_source_currency
      AND target_currency = p_target_currency
      AND expires_at > CURRENT_TIMESTAMP
    ORDER BY created_at DESC
    LIMIT 1;
    
    IF v_rate IS NULL THEN
        RAISE EXCEPTION 'No active exchange rate found for % to %', p_source_currency, p_target_currency;
    END IF;
    
    v_target_amount = p_source_amount * v_rate;
    
    RETURN v_target_amount;
END;
$$ LANGUAGE plpgsql;

-- Get latest rate for a currency pair
CREATE OR REPLACE FUNCTION get_latest_rate(
    p_source_currency VARCHAR(10),
    p_target_currency VARCHAR(10)
)
RETURNS DECIMAL(19,6) AS $$
DECLARE
    v_rate DECIMAL(19,6);
BEGIN
    SELECT rate INTO v_rate
    FROM exchange_rates
    WHERE source_currency = p_source_currency
      AND target_currency = p_target_currency
      AND expires_at > CURRENT_TIMESTAMP
    ORDER BY created_at DESC
    LIMIT 1;
    
    IF v_rate IS NULL THEN
        RAISE EXCEPTION 'No active exchange rate found for % to %', p_source_currency, p_target_currency;
    END IF;
    
    RETURN v_rate;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 4: VIEWS
-- ============================================================================

-- Active exchange rates
CREATE OR REPLACE VIEW v_active_exchange_rates AS
SELECT DISTINCT ON (source_currency, target_currency)
    id,
    source_currency,
    target_currency,
    rate,
    bid,
    ask,
    provider,
    created_at,
    expires_at
FROM exchange_rates
WHERE expires_at > CURRENT_TIMESTAMP
ORDER BY source_currency, target_currency, created_at DESC;

-- Exchange rate matrix (for common currencies)
CREATE OR REPLACE VIEW v_exchange_rate_matrix AS
SELECT 
    source_currency,
    target_currency,
    rate,
    CASE 
        WHEN expires_at > CURRENT_TIMESTAMP THEN 'ACTIVE'
        ELSE 'EXPIRED'
    END as status,
    created_at,
    expires_at
FROM exchange_rates
WHERE source_currency IN ('XAF', 'USD', 'EUR', 'GBP')
  AND target_currency IN ('XAF', 'USD', 'EUR', 'GBP')
  AND expires_at > CURRENT_TIMESTAMP - INTERVAL '1 hour'
ORDER BY source_currency, target_currency, created_at DESC;

-- Conversion statistics
CREATE OR REPLACE VIEW v_conversion_statistics AS
SELECT 
    source_currency,
    target_currency,
    COUNT(*) as conversion_count,
    SUM(source_amount) as total_source_amount,
    SUM(target_amount) as total_target_amount,
    AVG(exchange_rate) as average_rate,
    SUM(fee_amount) as total_fees,
    DATE(created_at) as conversion_date
FROM conversion_logs
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY source_currency, target_currency, DATE(created_at);

-- Rate volatility analysis
CREATE OR REPLACE VIEW v_rate_volatility AS
SELECT 
    source_currency,
    target_currency,
    DATE(created_at) as rate_date,
    MIN(rate) as min_rate,
    MAX(rate) as max_rate,
    AVG(rate) as avg_rate,
    STDDEV(rate) as rate_volatility,
    COUNT(*) as update_count
FROM exchange_rates
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY source_currency, target_currency, DATE(created_at);

-- ============================================================================
-- SECTION 5: INITIAL DATA
-- ============================================================================

-- Insert common currency pairs
INSERT INTO currency_pairs (source_currency, target_currency, base_fee_percentage, min_fee) VALUES
('XAF', 'USD', 0.0025, 100),
('USD', 'XAF', 0.0025, 0.10),
('XAF', 'EUR', 0.0025, 100),
('EUR', 'XAF', 0.0025, 0.10),
('USD', 'EUR', 0.0020, 0.05),
('EUR', 'USD', 0.0020, 0.05),
('GBP', 'USD', 0.0020, 0.05),
('USD', 'GBP', 0.0020, 0.05),
('GBP', 'EUR', 0.0020, 0.05),
('EUR', 'GBP', 0.0020, 0.05)
ON CONFLICT (source_currency, target_currency) DO NOTHING;

-- ============================================================================
-- END OF EXCHANGE SERVICE SCHEMA
-- ============================================================================
