-- ============================================================================
-- ZAPHIRA Platform - USER SERVICE Schema
-- ============================================================================
-- Database: zaphira_users_db
-- Description: User management, KYC, sessions, security questions, OTP
-- Dependencies: None (base service)
-- ============================================================================

\c zaphira_users_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE user_type_enum AS ENUM ('REGULAR', 'MERCHANT', 'ADMIN');
CREATE TYPE account_status_enum AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED', 'LOCKED');
CREATE TYPE role_type_enum AS ENUM ('ROLE_USER', 'ROLE_MERCHANT', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN');
CREATE TYPE admin_level_enum AS ENUM ('LEVEL_1', 'LEVEL_2', 'LEVEL_3', 'SUPER_ADMIN');
CREATE TYPE kyc_status_enum AS ENUM ('NOT_SUBMITTED', 'PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED');
CREATE TYPE document_type_enum AS ENUM ('NATIONAL_ID', 'PASSPORT', 'DRIVERS_LICENSE', 'RESIDENCE_PERMIT');
CREATE TYPE otp_purpose_enum AS ENUM ('REGISTRATION', 'LOGIN', 'TRANSACTION', 'PASSWORD_RESET', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION');

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: users (Single Table Inheritance for RegularUser, MerchantUser, AdminUser)
-- ============================================================================
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    user_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    
    -- Common fields
    email VARCHAR(100) UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    pin VARCHAR(255) NOT NULL,
    pin_changed_at TIMESTAMP,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    
    -- Address
    country VARCHAR(100) NOT NULL,
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    region VARCHAR(100),
    region_code VARCHAR(10),
    
    -- Account status
    account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT TRUE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(100),
    
    -- Login tracking
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    registration_ip VARCHAR(50) NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    
    -- Wallet reference
    wallet_id VARCHAR(50) UNIQUE,
    
    -- User preferences
    preferred_currency VARCHAR(10) DEFAULT 'XAF',
    preferred_language VARCHAR(10) DEFAULT 'fr',
    profile_picture VARCHAR(500),
    
    -- Notification preferences
    notifications_enabled BOOLEAN DEFAULT TRUE,
    email_notifications_enabled BOOLEAN DEFAULT TRUE,
    sms_notifications_enabled BOOLEAN DEFAULT TRUE,
    marketing_emails_enabled BOOLEAN DEFAULT FALSE,
    
    -- Activity tracking
    last_transaction_at TIMESTAMP,
    kyc_submitted_at TIMESTAMP,
    kyc_verified_at TIMESTAMP,
    
    -- ADMIN-specific fields
    admin_level VARCHAR(20),
    employee_id VARCHAR(50),
    supervisor_id BIGINT,
    can_manage_users BOOLEAN DEFAULT FALSE,
    can_manage_roles BOOLEAN DEFAULT FALSE,
    can_managekyc BOOLEAN DEFAULT FALSE,
    can_approve_transactions BOOLEAN DEFAULT FALSE,
    can_view_audit_logs BOOLEAN DEFAULT FALSE,
    can_access_reports BOOLEAN DEFAULT FALSE,
    last_admin_action_at TIMESTAMP,
    
    -- MERCHANT-specific fields
    business_name VARCHAR(200),
    business_registration_number VARCHAR(100),
    business_address TEXT,
    is_verified_merchant BOOLEAN DEFAULT FALSE,
    merchant_verified_at TIMESTAMP,
    verified_by BIGINT,
    can_accept_payments BOOLEAN DEFAULT FALSE,
    commission_rate DECIMAL(5,4),
    total_transactions BIGINT DEFAULT 0,
    total_transaction_volume DECIMAL(19,4) DEFAULT 0,
    
    -- Audit fields
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    role VARCHAR(50),
    id BIGINT GENERATED ALWAYS AS (user_id) STORED,
    
    CONSTRAINT chk_user_type CHECK (user_type IN ('REGULAR', 'MERCHANT', 'ADMIN')),
    CONSTRAINT chk_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_wallet_id ON users(wallet_id);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_account_status ON users(account_status);
CREATE INDEX idx_users_registration_date ON users(registration_date);

COMMENT ON TABLE users IS 'Main user table with Single Table Inheritance pattern';

-- ============================================================================
-- TABLE: kyc (KYC verification data)
-- ============================================================================
CREATE TABLE kyc (
    kyc_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
    
    -- Document information
    document_type VARCHAR(30),
    document_number VARCHAR(100),
    document_issue_date DATE,
    document_expiry_date DATE,
    document_issuing_country VARCHAR(100),
    
    -- Uploaded files
    document_front_image VARCHAR(500),
    document_back_image VARCHAR(500),
    selfie_image VARCHAR(500),
    proof_of_address_document VARCHAR(500),
    
    -- Data from documents
    full_name_on_document VARCHAR(200),
    date_of_birth_on_document DATE,
    address_on_document VARCHAR(500),
    
    -- Verification tracking
    submitted_at TIMESTAMP,
    verified_at TIMESTAMP,
    verified_by BIGINT,
    
    -- Rejection handling
    rejection_reason VARCHAR(1000),
    rejected_at TIMESTAMP,
    rejected_by VARCHAR(100),
    resubmission_count INTEGER NOT NULL DEFAULT 0,
    last_resubmitted_at TIMESTAMP,
    
    -- Additional notes
    notes TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_kyc_status CHECK (kyc_status IN ('NOT_SUBMITTED', 'PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_kyc_user_id ON kyc(user_id);
CREATE INDEX idx_kyc_status ON kyc(kyc_status);

COMMENT ON TABLE kyc IS 'KYC (Know Your Customer) verification data';

-- ============================================================================
-- TABLE: user_sessions (Active user sessions)
-- ============================================================================
CREATE TABLE user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    token TEXT NOT NULL,
    refresh_token TEXT,
    
    -- Session timing
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    expiry_time TIMESTAMP NOT NULL,
    refresh_expiry_time TIMESTAMP,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Device information
    ip_address VARCHAR(50),
    user_agent TEXT,
    device_type VARCHAR(50),
    device_name VARCHAR(100),
    operating_system VARCHAR(50),
    browser VARCHAR(50),
    location VARCHAR(200),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_session_user_id ON user_sessions(user_id);
CREATE INDEX idx_session_expiry ON user_sessions(expiry_time);
CREATE INDEX idx_session_active ON user_sessions(is_active);

COMMENT ON TABLE user_sessions IS 'Active user sessions with JWT tokens and device info';

-- ============================================================================
-- TABLE: otp_codes (One-time password codes)
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
    
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('REGISTRATION', 'LOGIN', 'TRANSACTION', 'PASSWORD_RESET', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION')),
    CONSTRAINT chk_otp_attempts CHECK (attempts >= 0 AND attempts <= 5)
);

CREATE INDEX idx_otp_code ON otp_codes(code);
CREATE INDEX idx_otp_phone ON otp_codes(phone);
CREATE INDEX idx_otp_email ON otp_codes(email);
CREATE INDEX idx_otp_expires ON otp_codes(expires_at);
CREATE INDEX idx_otp_user_id ON otp_codes(user_id);

COMMENT ON TABLE otp_codes IS 'One-time password codes for verification';

-- ============================================================================
-- TABLE: otp_tokens (Alternative OTP token storage)
-- ============================================================================
CREATE TABLE otp_tokens (
    id BIGSERIAL PRIMARY KEY,
    
    code VARCHAR(10) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    
    purpose VARCHAR(50) NOT NULL,
    context TEXT,
    
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    
    attempts INTEGER NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT chk_otp_token_attempts CHECK (attempts >= 0 AND attempts <= 5)
);

CREATE INDEX idx_otp_token_code ON otp_tokens(code);
CREATE INDEX idx_otp_token_phone ON otp_tokens(phone_number);
CREATE INDEX idx_otp_token_email ON otp_tokens(email);

COMMENT ON TABLE otp_tokens IS 'Alternative OTP token storage with context';

-- ============================================================================
-- TABLE: predefined_security_questions
-- ============================================================================
CREATE TABLE predefined_security_questions (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_predefined_security_active ON predefined_security_questions(active);

COMMENT ON TABLE predefined_security_questions IS 'Predefined security questions for account recovery';

-- Insert default security questions
INSERT INTO predefined_security_questions (question, display_order) VALUES
('What is your mother''s maiden name?', 1),
('What was the name of your first pet?', 2),
('What city were you born in?', 3),
('What is the name of your favorite childhood friend?', 4),
('What was your childhood nickname?', 5),
('In what city did you meet your spouse/partner?', 6),
('What is the name of your favorite teacher?', 7),
('What was the make and model of your first car?', 8);

-- ============================================================================
-- TABLE: security_question (User-defined custom questions)
-- ============================================================================
CREATE TABLE security_question (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL
);

COMMENT ON TABLE security_question IS 'User-defined custom security questions';

-- ============================================================================
-- TABLE: user_security_answers (User answers to security questions)
-- ============================================================================
CREATE TABLE user_security_answers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT,
    
    answer_hash VARCHAR(255) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_security_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_security_question FOREIGN KEY (question_id) REFERENCES predefined_security_questions(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_security_user_id ON user_security_answers(user_id);

COMMENT ON TABLE user_security_answers IS 'User answers to security questions (hashed)';

-- ============================================================================
-- TABLE: user_analytics (User analytics and metrics)
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
    
    CONSTRAINT fk_user_analytics_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_analytics_date UNIQUE (user_id, analytics_date)
);

CREATE INDEX idx_user_analytics_user_id ON user_analytics(user_id);
CREATE INDEX idx_user_analytics_date ON user_analytics(analytics_date);

COMMENT ON TABLE user_analytics IS 'Daily user analytics and metrics';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_kyc_updated_at BEFORE UPDATE ON kyc
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_security_answers_updated_at BEFORE UPDATE ON user_security_answers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_analytics_updated_at BEFORE UPDATE ON user_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SECTION 4: VIEWS
-- ============================================================================

-- View: Active users
CREATE OR REPLACE VIEW v_active_users AS
SELECT 
    user_id, 
    user_type,
    email, 
    phone_number, 
    first_name, 
    last_name,
    account_status,
    registration_date
FROM users
WHERE account_status = 'ACTIVE' 
  AND (account_locked_until IS NULL OR account_locked_until < CURRENT_TIMESTAMP);

-- View: Pending KYC
CREATE OR REPLACE VIEW v_pending_kyc AS
SELECT 
    k.kyc_id,
    k.user_id,
    u.email,
    u.phone_number,
    u.first_name,
    u.last_name,
    k.kyc_status,
    k.submitted_at,
    k.resubmission_count
FROM kyc k
JOIN users u ON k.user_id = u.user_id
WHERE k.kyc_status IN ('PENDING', 'UNDER_REVIEW');

-- ============================================================================
-- END OF USER SERVICE SCHEMA
-- ============================================================================
