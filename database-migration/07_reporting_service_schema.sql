-- ============================================================================
-- ZAPHIRA Platform - REPORTING SERVICE Schema
-- ============================================================================
-- Database: zaphira_reports_db
-- Description: Daily reports, analytics, merchant metrics, user metrics
-- Dependencies: Aggregates data from transactions, users, disputes
-- ============================================================================

\c zaphira_reports_db

-- ============================================================================
-- SECTION 1: TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: daily_reports (Platform-wide daily metrics)
-- ============================================================================
CREATE TABLE daily_reports (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    
    -- Transaction metrics
    total_transactions BIGINT DEFAULT 0,
    successful_transactions BIGINT DEFAULT 0,
    failed_transactions BIGINT DEFAULT 0,
    pending_transactions BIGINT DEFAULT 0,
    
    total_transaction_volume DECIMAL(19,4) DEFAULT 0,
    
    -- Financial metrics
    total_fees_collected DECIMAL(19,4) DEFAULT 0,
    service_fees_collected DECIMAL(19,4) DEFAULT 0,
    fx_fees_collected DECIMAL(19,4) DEFAULT 0,
    average_fee_percentage DECIMAL(5,4),
    
    -- Success rates
    success_rate DECIMAL(5,4),
    settlement_success_rate DECIMAL(5,4),
    
    -- Settlement metrics
    total_settlements BIGINT DEFAULT 0,
    completed_settlements BIGINT DEFAULT 0,
    failed_settlements BIGINT DEFAULT 0,
    total_settlement_volume DECIMAL(19,4) DEFAULT 0,
    
    -- Refund metrics
    total_refunds BIGINT DEFAULT 0,
    total_refund_volume DECIMAL(19,4) DEFAULT 0,
    refund_rate DECIMAL(5,4),
    
    -- Reversal metrics
    total_reversals BIGINT DEFAULT 0,
    total_reversal_volume DECIMAL(19,4) DEFAULT 0,
    reversal_rate DECIMAL(5,4),
    
    -- Dispute metrics
    total_disputes BIGINT DEFAULT 0,
    new_disputes BIGINT DEFAULT 0,
    resolved_disputes BIGINT DEFAULT 0,
    total_disputed_amount DECIMAL(19,4) DEFAULT 0,
    dispute_rate DECIMAL(5,4),
    
    -- Currency breakdown
    top_currency VARCHAR(10),
    unique_currencies INTEGER DEFAULT 0,
    
    -- Finalization
    is_finalized BOOLEAN DEFAULT FALSE,
    finalized_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_reports_date ON daily_reports(report_date);
CREATE INDEX idx_daily_reports_finalized ON daily_reports(is_finalized);

COMMENT ON TABLE daily_reports IS 'Platform-wide daily aggregated metrics and KPIs';

-- ============================================================================
-- TABLE: merchant_analytics (Merchant-specific analytics)
-- ============================================================================
CREATE TABLE merchant_analytics (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    analytics_date DATE NOT NULL,
    
    -- Merchant classification
    merchant_tier VARCHAR(20),
    primary_currency VARCHAR(10),
    
    -- Transaction metrics
    total_transaction_count BIGINT DEFAULT 0,
    total_volume DECIMAL(19,4) DEFAULT 0,
    transaction_success_rate DECIMAL(5,4),
    average_transaction_value DECIMAL(19,4),
    
    -- Financial metrics
    gross_revenue DECIMAL(19,4) DEFAULT 0,
    net_revenue DECIMAL(19,4) DEFAULT 0,
    total_fees_paid DECIMAL(19,4) DEFAULT 0,
    
    -- Settlement metrics
    settlement_count BIGINT DEFAULT 0,
    settlement_volume DECIMAL(19,4) DEFAULT 0,
    successful_settlements BIGINT DEFAULT 0,
    failed_settlements BIGINT DEFAULT 0,
    settlement_success_rate DECIMAL(5,4),
    settlement_delay_average DECIMAL(10,2), -- in hours
    
    -- Dispute metrics
    dispute_count BIGINT DEFAULT 0,
    dispute_volume DECIMAL(19,4) DEFAULT 0,
    dispute_rate DECIMAL(5,4),
    resolved_disputes BIGINT DEFAULT 0,
    merchant_won_disputes BIGINT DEFAULT 0,
    
    -- Rate metrics
    chargeback_rate DECIMAL(5,4),
    reversal_rate DECIMAL(5,4),
    
    -- Risk and compliance
    risk_score DECIMAL(5,2),
    compliance_status VARCHAR(20),
    
    -- Performance metrics
    average_response_time_ms BIGINT,
    
    -- Geographic metrics
    country_count INTEGER DEFAULT 0,
    currency_count INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_merchant_analytics_date UNIQUE (merchant_id, analytics_date)
);

CREATE INDEX idx_merchant_analytics_merchant_id ON merchant_analytics(merchant_id);
CREATE INDEX idx_merchant_analytics_date ON merchant_analytics(analytics_date);
CREATE INDEX idx_merchant_analytics_risk_score ON merchant_analytics(risk_score);

COMMENT ON TABLE merchant_analytics IS 'Daily analytics and metrics for merchant accounts';

-- ============================================================================
-- TABLE: user_analytics (User-specific analytics)
-- ============================================================================
CREATE TABLE user_analytics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    analytics_date DATE NOT NULL,
    
    user_type VARCHAR(20),
    
    -- Transaction metrics
    transaction_count BIGINT DEFAULT 0,
    transaction_volume DECIMAL(19,4) DEFAULT 0,
    successful_transactions BIGINT DEFAULT 0,
    failed_transactions BIGINT DEFAULT 0,
    
    -- Financial metrics
    average_transaction_amount DECIMAL(19,4),
    max_transaction_amount DECIMAL(19,4),
    min_transaction_amount DECIMAL(19,4),
    total_fees_paid DECIMAL(19,4) DEFAULT 0,
    
    -- Dispute metrics
    dispute_count INTEGER DEFAULT 0,
    dispute_volume DECIMAL(19,4) DEFAULT 0,
    dispute_rate DECIMAL(5,4),
    
    -- Reversal metrics
    reversal_count INTEGER DEFAULT 0,
    reversal_volume DECIMAL(19,4) DEFAULT 0,
    reversal_rate DECIMAL(5,4),
    
    -- Currency usage
    primary_currency VARCHAR(10),
    currency_count INTEGER DEFAULT 1,
    
    -- Risk and compliance
    risk_score DECIMAL(5,2),
    compliance_status VARCHAR(20),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_analytics_date UNIQUE (user_id, analytics_date)
);

CREATE INDEX idx_user_analytics_user_id ON user_analytics(user_id);
CREATE INDEX idx_user_analytics_date ON user_analytics(analytics_date);
CREATE INDEX idx_user_analytics_user_type ON user_analytics(user_type);

COMMENT ON TABLE user_analytics IS 'Daily analytics and metrics for individual users';

-- ============================================================================
-- SECTION 2: AGGREGATE TABLES (Pre-computed summaries)
-- ============================================================================

-- Monthly summary table
CREATE TABLE monthly_reports (
    id BIGSERIAL PRIMARY KEY,
    report_month DATE NOT NULL UNIQUE, -- First day of month
    
    total_transactions BIGINT DEFAULT 0,
    total_volume DECIMAL(19,4) DEFAULT 0,
    total_fees_collected DECIMAL(19,4) DEFAULT 0,
    
    active_users_count BIGINT DEFAULT 0,
    active_merchants_count BIGINT DEFAULT 0,
    new_users_count BIGINT DEFAULT 0,
    
    total_disputes BIGINT DEFAULT 0,
    resolved_disputes BIGINT DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_monthly_reports_month ON monthly_reports(report_month);

-- Currency breakdown table
CREATE TABLE currency_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    currency VARCHAR(10) NOT NULL,
    
    transaction_count BIGINT DEFAULT 0,
    transaction_volume DECIMAL(19,4) DEFAULT 0,
    average_transaction_value DECIMAL(19,4),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_currency_stats_date UNIQUE (report_date, currency)
);

CREATE INDEX idx_currency_stats_date ON currency_daily_stats(report_date);
CREATE INDEX idx_currency_stats_currency ON currency_daily_stats(currency);

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_report_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_merchant_analytics_updated_at BEFORE UPDATE ON merchant_analytics
    FOR EACH ROW EXECUTE FUNCTION update_report_updated_at();

CREATE TRIGGER update_user_analytics_updated_at BEFORE UPDATE ON user_analytics
    FOR EACH ROW EXECUTE FUNCTION update_report_updated_at();

-- ============================================================================
-- SECTION 4: UTILITY FUNCTIONS
-- ============================================================================

-- Function to finalize daily report
CREATE OR REPLACE FUNCTION finalize_daily_report(p_report_date DATE)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE daily_reports
    SET is_finalized = TRUE,
        finalized_at = CURRENT_TIMESTAMP
    WHERE report_date = p_report_date
      AND is_finalized = FALSE;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate success rate
CREATE OR REPLACE FUNCTION calculate_success_rate(
    p_successful BIGINT,
    p_total BIGINT
)
RETURNS DECIMAL(5,4) AS $$
BEGIN
    IF p_total = 0 THEN
        RETURN 0;
    END IF;
    RETURN (p_successful::DECIMAL / p_total::DECIMAL);
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 5: VIEWS
-- ============================================================================

-- Latest daily report
CREATE OR REPLACE VIEW v_latest_daily_report AS
SELECT *
FROM daily_reports
ORDER BY report_date DESC
LIMIT 1;

-- Merchant performance ranking
CREATE OR REPLACE VIEW v_merchant_performance_ranking AS
SELECT 
    merchant_id,
    analytics_date,
    total_volume,
    total_transaction_count,
    transaction_success_rate,
    dispute_rate,
    risk_score,
    RANK() OVER (PARTITION BY analytics_date ORDER BY total_volume DESC) as volume_rank,
    RANK() OVER (PARTITION BY analytics_date ORDER BY transaction_success_rate DESC) as success_rank
FROM merchant_analytics
WHERE analytics_date >= CURRENT_DATE - INTERVAL '30 days';

-- User activity trends
CREATE OR REPLACE VIEW v_user_activity_trends AS
SELECT 
    user_id,
    DATE_TRUNC('week', analytics_date) as week_start,
    SUM(transaction_count) as weekly_transactions,
    SUM(transaction_volume) as weekly_volume,
    AVG(average_transaction_amount) as avg_transaction_amount,
    AVG(risk_score) as avg_risk_score
FROM user_analytics
WHERE analytics_date >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY user_id, DATE_TRUNC('week', analytics_date);

-- Platform health summary
CREATE OR REPLACE VIEW v_platform_health_summary AS
SELECT 
    report_date,
    total_transactions,
    success_rate,
    total_transaction_volume,
    total_fees_collected,
    dispute_rate,
    settlement_success_rate,
    CASE 
        WHEN success_rate >= 0.95 AND dispute_rate < 0.02 THEN 'EXCELLENT'
        WHEN success_rate >= 0.90 AND dispute_rate < 0.05 THEN 'GOOD'
        WHEN success_rate >= 0.85 AND dispute_rate < 0.10 THEN 'FAIR'
        ELSE 'POOR'
    END as health_status
FROM daily_reports
WHERE report_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY report_date DESC;

-- ============================================================================
-- END OF REPORTING SERVICE SCHEMA
-- ============================================================================
