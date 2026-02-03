-- ============================================================================
-- ZAPHIRA Platform - NOTIFICATION SERVICE Schema
-- ============================================================================
-- Database: zaphira_notifications_db
-- Description: Notifications, OTP codes, verification tokens, email/SMS logs
-- Dependencies: References users from zaphira_users_db
-- ============================================================================

\c zaphira_notifications_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE notification_type_enum AS ENUM (
    'EMAIL', 'SMS', 'PUSH', 'IN_APP', 'WEBHOOK'
);

CREATE TYPE notification_status_enum AS ENUM (
    'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED', 'OPENED', 'CLICKED'
);

CREATE TYPE otp_purpose_enum AS ENUM (
    'REGISTRATION', 'LOGIN', 'TRANSACTION', 
    'PASSWORD_RESET', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION',
    'PROFILE_UPDATE', 'SECURITY_CHANGE'
);

CREATE TYPE template_type_enum AS ENUM (
    'WELCOME', 'OTP', 'TRANSACTION_RECEIPT', 'TRANSACTION_ALERT',
    'ACCOUNT_VERIFICATION', 'PASSWORD_RESET', 'SECURITY_ALERT',
    'MONTHLY_STATEMENT', 'PROMOTIONAL', 'SYSTEM_NOTIFICATION'
);

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: notifications (Notification queue and history)
-- ============================================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    
    user_id BIGINT,
    recipient_email VARCHAR(100),
    recipient_phone VARCHAR(20),
    
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Content
    subject VARCHAR(500),
    body TEXT NOT NULL,
    
    -- Template
    template_id BIGINT,
    template_name VARCHAR(100),
    template_variables TEXT, -- JSON format
    
    -- Priority and scheduling
    priority INTEGER DEFAULT 5, -- 1 (highest) to 10 (lowest)
    scheduled_for TIMESTAMP,
    
    -- Delivery tracking
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Error handling
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    
    -- External provider tracking
    provider VARCHAR(50),
    provider_message_id VARCHAR(255),
    provider_response TEXT,
    
    -- Metadata
    metadata TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_notification_type CHECK (type IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP', 'WEBHOOK')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED', 'OPENED', 'CLICKED')),
    CONSTRAINT chk_priority CHECK (priority BETWEEN 1 AND 10)
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_scheduled_for ON notifications(scheduled_for);
CREATE INDEX idx_notifications_recipient_email ON notifications(recipient_email);
CREATE INDEX idx_notifications_recipient_phone ON notifications(recipient_phone);

COMMENT ON TABLE notifications IS 'Notification queue and delivery history';

-- ============================================================================
-- TABLE: notification_templates (Reusable message templates)
-- ============================================================================
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    
    template_name VARCHAR(100) NOT NULL UNIQUE,
    template_type VARCHAR(50) NOT NULL,
    
    -- Channel-specific templates
    email_subject VARCHAR(500),
    email_body TEXT,
    sms_body VARCHAR(500),
    push_title VARCHAR(200),
    push_body VARCHAR(500),
    
    -- Template variables (comma-separated)
    required_variables TEXT,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Localization
    language VARCHAR(10) DEFAULT 'en',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_template_type CHECK (template_type IN (
        'WELCOME', 'OTP', 'TRANSACTION_RECEIPT', 'TRANSACTION_ALERT',
        'ACCOUNT_VERIFICATION', 'PASSWORD_RESET', 'SECURITY_ALERT',
        'MONTHLY_STATEMENT', 'PROMOTIONAL', 'SYSTEM_NOTIFICATION'
    ))
);

CREATE INDEX idx_notification_templates_name ON notification_templates(template_name);
CREATE INDEX idx_notification_templates_type ON notification_templates(template_type);
CREATE INDEX idx_notification_templates_active ON notification_templates(is_active);

COMMENT ON TABLE notification_templates IS 'Reusable notification message templates';

-- ============================================================================
-- TABLE: otp_codes (One-time password codes) - Duplicate from user-service
-- ============================================================================
CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    
    code VARCHAR(10) NOT NULL,
    otp_hash VARCHAR(255),
    
    email VARCHAR(100),
    phone VARCHAR(20),
    phone_number VARCHAR(20),
    
    purpose VARCHAR(50) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    
    attempts INTEGER NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Delivery tracking
    notification_id BIGINT,
    
    CONSTRAINT chk_otp_purpose CHECK (purpose IN (
        'REGISTRATION', 'LOGIN', 'TRANSACTION', 
        'PASSWORD_RESET', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION',
        'PROFILE_UPDATE', 'SECURITY_CHANGE'
    )),
    CONSTRAINT chk_otp_attempts CHECK (attempts >= 0 AND attempts <= 5)
);

CREATE INDEX idx_otp_codes_code ON otp_codes(code);
CREATE INDEX idx_otp_codes_phone ON otp_codes(phone);
CREATE INDEX idx_otp_codes_email ON otp_codes(email);
CREATE INDEX idx_otp_codes_expires ON otp_codes(expires_at);
CREATE INDEX idx_otp_codes_user_id ON otp_codes(user_id);

COMMENT ON TABLE otp_codes IS 'One-time password codes with delivery tracking';

-- ============================================================================
-- TABLE: verification_tokens (Email/Phone verification tokens)
-- ============================================================================
CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    code VARCHAR(100) NOT NULL UNIQUE,
    
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Delivery tracking
    notification_id BIGINT
);

CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_verification_tokens_code ON verification_tokens(code);
CREATE INDEX idx_verification_tokens_expires ON verification_tokens(expires_at);
CREATE INDEX idx_verification_tokens_used ON verification_tokens(used);

COMMENT ON TABLE verification_tokens IS 'Verification tokens for email and phone confirmation';

-- ============================================================================
-- TABLE: email_logs (Email delivery logs)
-- ============================================================================
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT,
    
    recipient_email VARCHAR(100) NOT NULL,
    subject VARCHAR(500),
    
    status VARCHAR(20) NOT NULL,
    
    -- Provider details
    provider VARCHAR(50),
    provider_message_id VARCHAR(255),
    
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    bounced_at TIMESTAMP,
    
    error_message TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_logs_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE SET NULL
);

CREATE INDEX idx_email_logs_notification_id ON email_logs(notification_id);
CREATE INDEX idx_email_logs_recipient ON email_logs(recipient_email);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_created_at ON email_logs(created_at);

COMMENT ON TABLE email_logs IS 'Detailed email delivery logs';

-- ============================================================================
-- TABLE: sms_logs (SMS delivery logs)
-- ============================================================================
CREATE TABLE sms_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT,
    
    recipient_phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    
    status VARCHAR(20) NOT NULL,
    
    -- Provider details (Twilio, etc.)
    provider VARCHAR(50),
    provider_message_id VARCHAR(255),
    provider_status VARCHAR(50),
    
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    error_message TEXT,
    error_code VARCHAR(50),
    
    -- Cost tracking
    cost DECIMAL(10,4),
    currency VARCHAR(10),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sms_logs_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE SET NULL
);

CREATE INDEX idx_sms_logs_notification_id ON sms_logs(notification_id);
CREATE INDEX idx_sms_logs_recipient ON sms_logs(recipient_phone);
CREATE INDEX idx_sms_logs_status ON sms_logs(status);
CREATE INDEX idx_sms_logs_created_at ON sms_logs(created_at);

COMMENT ON TABLE sms_logs IS 'Detailed SMS delivery logs';

-- ============================================================================
-- TABLE: notification_preferences (User notification preferences)
-- ============================================================================
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- Channel preferences
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    
    -- Category preferences
    transaction_alerts BOOLEAN DEFAULT TRUE,
    security_alerts BOOLEAN DEFAULT TRUE,
    marketing_emails BOOLEAN DEFAULT FALSE,
    promotional_sms BOOLEAN DEFAULT FALSE,
    monthly_statements BOOLEAN DEFAULT TRUE,
    
    -- Quiet hours
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    
    -- Language preference
    preferred_language VARCHAR(10) DEFAULT 'en',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

COMMENT ON TABLE notification_preferences IS 'User-specific notification preferences';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_notification_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

CREATE TRIGGER update_notification_templates_updated_at BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

CREATE TRIGGER update_notification_preferences_updated_at BEFORE UPDATE ON notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_notification_updated_at();

-- Clean expired OTP codes
CREATE OR REPLACE FUNCTION cleanup_expired_otps()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM otp_codes
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '24 hours';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Clean expired verification tokens
CREATE OR REPLACE FUNCTION cleanup_expired_verification_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM verification_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 4: VIEWS
-- ============================================================================

-- Pending notifications
CREATE OR REPLACE VIEW v_pending_notifications AS
SELECT *
FROM notifications
WHERE status = 'PENDING'
  AND (scheduled_for IS NULL OR scheduled_for <= CURRENT_TIMESTAMP)
  AND retry_count < max_retries
ORDER BY priority ASC, created_at ASC;

-- Failed notifications requiring retry
CREATE OR REPLACE VIEW v_failed_notifications_retry AS
SELECT *
FROM notifications
WHERE status = 'FAILED'
  AND retry_count < max_retries
  AND created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'
ORDER BY priority ASC, created_at ASC;

-- Notification delivery statistics
CREATE OR REPLACE VIEW v_notification_stats AS
SELECT 
    DATE(created_at) as notification_date,
    type,
    status,
    COUNT(*) as notification_count,
    AVG(EXTRACT(EPOCH FROM (COALESCE(sent_at, CURRENT_TIMESTAMP) - created_at))) as avg_queue_time_seconds
FROM notifications
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at), type, status;

-- OTP usage statistics
CREATE OR REPLACE VIEW v_otp_usage_stats AS
SELECT 
    DATE(created_at) as otp_date,
    purpose,
    COUNT(*) as total_otps,
    COUNT(*) FILTER (WHERE verified_at IS NOT NULL) as verified_otps,
    COUNT(*) FILTER (WHERE expires_at < CURRENT_TIMESTAMP AND verified_at IS NULL) as expired_otps,
    AVG(attempts) as avg_attempts
FROM otp_codes
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at), purpose;

-- ============================================================================
-- SECTION 5: INITIAL DATA
-- ============================================================================

-- Insert default notification templates
INSERT INTO notification_templates (template_name, template_type, email_subject, email_body, sms_body) VALUES
('WELCOME_EMAIL', 'WELCOME', 
 'Welcome to Zaphira Platform', 
 'Welcome {{firstName}}! Your account has been created successfully.',
 'Welcome to Zaphira! Your account is ready to use.'),
 
('OTP_EMAIL', 'OTP',
 'Your verification code',
 'Your OTP code is: {{otpCode}}. Valid for {{validityMinutes}} minutes.',
 'Your Zaphira OTP: {{otpCode}}. Valid for {{validityMinutes}} min.'),
 
('TRANSACTION_RECEIPT', 'TRANSACTION_RECEIPT',
 'Transaction Receipt - {{transactionReference}}',
 'Transaction completed. Amount: {{amount}} {{currency}}. Reference: {{transactionReference}}',
 'Transaction: {{amount}} {{currency}}. Ref: {{transactionReference}}')
 
ON CONFLICT (template_name) DO NOTHING;

-- ============================================================================
-- END OF NOTIFICATION SERVICE SCHEMA
-- ============================================================================
