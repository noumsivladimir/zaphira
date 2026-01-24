-- ============================================================================
-- ZAPHIRA Platform - AUTH SERVICE Schema
-- ============================================================================
-- Database: zaphira_auth_db
-- Description: Authentication tokens, JWT, refresh tokens, activity logs
-- Dependencies: References users(user_id) from zaphira_users_db
-- ============================================================================

\c zaphira_auth_db

-- ============================================================================
-- SECTION 1: TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: tokens (JWT access tokens)
-- ============================================================================
CREATE TABLE tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    
    user_id BIGINT NOT NULL,
    
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tokens_user_id ON tokens(user_id);
CREATE INDEX idx_tokens_token ON tokens(token);
CREATE INDEX idx_tokens_expired ON tokens(expired);
CREATE INDEX idx_tokens_revoked ON tokens(revoked);

COMMENT ON TABLE tokens IS 'JWT access tokens with revocation support';

-- ============================================================================
-- TABLE: refresh_token (Refresh tokens for token renewal)
-- ============================================================================
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    
    user_id BIGINT NOT NULL,
    
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_refresh_token_expiry ON refresh_token(expiry_date);
CREATE INDEX idx_refresh_token_revoked ON refresh_token(revoked);

COMMENT ON TABLE refresh_token IS 'Refresh tokens for obtaining new access tokens';

-- ============================================================================
-- TABLE: activity_log (User activity audit log)
-- ============================================================================
CREATE TABLE activity_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    user_user_id BIGINT, -- Duplicate field for compatibility
    
    action VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Additional context
    ip_address VARCHAR(50),
    user_agent TEXT,
    resource VARCHAR(255),
    method VARCHAR(10),
    status_code INTEGER,
    
    -- Request details
    request_body TEXT,
    response_body TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_log_user_id ON activity_log(user_id);
CREATE INDEX idx_activity_log_timestamp ON activity_log(timestamp);
CREATE INDEX idx_activity_log_action ON activity_log(action);

COMMENT ON TABLE activity_log IS 'Complete audit log of user activities and API calls';

-- ============================================================================
-- SECTION 2: TOKEN CLEANUP FUNCTIONS
-- ============================================================================

-- Function to clean expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM tokens 
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean expired refresh tokens
CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM refresh_token 
    WHERE expiry_date < CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to revoke all user tokens
CREATE OR REPLACE FUNCTION revoke_user_tokens(p_user_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE tokens 
    SET revoked = TRUE 
    WHERE user_id = p_user_id AND revoked = FALSE;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    UPDATE refresh_token 
    SET revoked = TRUE 
    WHERE user_id = p_user_id AND revoked = FALSE;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 3: VIEWS
-- ============================================================================

-- View: Active tokens
CREATE OR REPLACE VIEW v_active_tokens AS
SELECT 
    t.id,
    t.user_id,
    t.created_at,
    t.expires_at,
    CASE 
        WHEN t.expires_at < CURRENT_TIMESTAMP THEN TRUE 
        ELSE t.expired 
    END as is_expired,
    t.revoked
FROM tokens t
WHERE NOT t.revoked 
  AND t.expires_at > CURRENT_TIMESTAMP;

-- View: Activity summary by user
CREATE OR REPLACE VIEW v_user_activity_summary AS
SELECT 
    user_id,
    DATE(timestamp) as activity_date,
    COUNT(*) as total_actions,
    COUNT(DISTINCT action) as unique_actions,
    MIN(timestamp) as first_action,
    MAX(timestamp) as last_action
FROM activity_log
WHERE user_id IS NOT NULL
GROUP BY user_id, DATE(timestamp);

-- ============================================================================
-- SECTION 4: SCHEDULED JOBS (commented - use pg_cron or external scheduler)
-- ============================================================================

-- To enable automatic cleanup, install pg_cron extension and uncomment:
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- 
-- -- Clean expired tokens daily at 2 AM
-- SELECT cron.schedule('cleanup-expired-tokens', '0 2 * * *', 'SELECT cleanup_expired_tokens();');
-- 
-- -- Clean expired refresh tokens weekly on Sunday at 3 AM
-- SELECT cron.schedule('cleanup-refresh-tokens', '0 3 * * 0', 'SELECT cleanup_expired_refresh_tokens();');

-- ============================================================================
-- END OF AUTH SERVICE SCHEMA
-- ============================================================================
