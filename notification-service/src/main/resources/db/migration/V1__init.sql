-- =============================================================================
-- Notification Service Database Schema
-- =============================================================================
-- Initial schema setup for notification service including tables for:
-- - Notifications (main notification records)
-- - Notification Templates (email/SMS templates)
-- - Notification Preferences (user preferences)
-- - Notification Logs (audit trail)
-- =============================================================================

-- Create ENUM types
CREATE TYPE notification_type AS ENUM ('EMAIL', 'SMS', 'PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'BOUNCED', 'BLOCKED');
CREATE TYPE template_type AS ENUM ('TRANSACTION_CONFIRMATION', 'WALLET_ALERT', 'USER_REGISTRATION', 'PASSWORD_RESET', 'TRANSACTION_ALERT', 'WALLET_LOW_BALANCE');

-- =============================================================================
-- Main Notifications Table
-- =============================================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    notification_type notification_type NOT NULL DEFAULT 'EMAIL',
    status notification_status NOT NULL DEFAULT 'PENDING',
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL,
    template_id UUID,
    retries INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    last_attempt_at TIMESTAMP,
    sent_at TIMESTAMP,
    failed_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes on notifications table
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_event_id ON notifications(event_id);
CREATE INDEX idx_notifications_aggregate_id ON notifications(aggregate_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_notification_type ON notifications(notification_type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_sent_at ON notifications(sent_at DESC);
CREATE INDEX idx_notifications_source_service ON notifications(source_service);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notifications_event_type ON notifications(event_type);

-- =============================================================================
-- Notification Templates Table
-- =============================================================================
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_type template_type NOT NULL UNIQUE,
    notification_type notification_type NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    version INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes on templates table
CREATE INDEX idx_templates_type ON notification_templates(template_type);
CREATE INDEX idx_templates_notification_type ON notification_templates(notification_type);
CREATE INDEX idx_templates_active ON notification_templates(is_active);
CREATE INDEX idx_templates_created_at ON notification_templates(created_at DESC);

-- =============================================================================
-- Notification Preferences Table
-- =============================================================================
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL UNIQUE,
    email_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT false,
    in_app_enabled BOOLEAN DEFAULT true,
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    preferred_channel notification_type DEFAULT 'EMAIL',
    opt_out BOOLEAN DEFAULT false,
    opt_out_reason TEXT,
    opt_out_at TIMESTAMP,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    notification_frequency VARCHAR(50) DEFAULT 'IMMEDIATE',
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Indexes on preferences table
CREATE INDEX idx_preferences_user_id ON notification_preferences(user_id);
CREATE INDEX idx_preferences_opt_out ON notification_preferences(opt_out);
CREATE INDEX idx_preferences_created_at ON notification_preferences(created_at DESC);

-- =============================================================================
-- Notification Logs Table (Audit Trail)
-- =============================================================================
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    event_id VARCHAR(255),
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    status notification_status NOT NULL,
    message TEXT,
    error_details TEXT,
    attempt_number INT,
    response_code INT,
    response_time_ms INT,
    external_message_id VARCHAR(255),
    provider VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Indexes on logs table
CREATE INDEX idx_logs_notification_id ON notification_logs(notification_id);
CREATE INDEX idx_logs_user_id ON notification_logs(user_id);
CREATE INDEX idx_logs_event_id ON notification_logs(event_id);
CREATE INDEX idx_logs_action ON notification_logs(action);
CREATE INDEX idx_logs_created_at ON notification_logs(created_at DESC);
CREATE INDEX idx_logs_user_created ON notification_logs(user_id, created_at DESC);
CREATE INDEX idx_logs_status ON notification_logs(status);

-- =============================================================================
-- Insert Default Templates
-- =============================================================================
INSERT INTO notification_templates (
    template_type, notification_type, name, description, 
    subject_template, body_template, is_active
) VALUES
(
    'TRANSACTION_CONFIRMATION',
    'EMAIL',
    'Transaction Confirmation Email',
    'Email template for transaction confirmations',
    'Your transaction of {{amount}} {{currency}} has been confirmed',
    '<h2>Transaction Confirmation</h2>
    <p>Dear {{firstName}},</p>
    <p>Your transaction has been successfully confirmed.</p>
    <ul>
        <li>Amount: {{amount}} {{currency}}</li>
        <li>Date: {{transactionDate}}</li>
        <li>Reference: {{transactionId}}</li>
    </ul>
    <p>Thank you for using Zaphira.</p>',
    true
),
(
    'WALLET_ALERT',
    'EMAIL',
    'Wallet Alert Email',
    'Email template for wallet alerts',
    'Alert: Your wallet balance has changed',
    '<h2>Wallet Alert</h2>
    <p>Dear {{firstName}},</p>
    <p>We wanted to notify you about a change in your wallet balance.</p>
    <ul>
        <li>New Balance: {{newBalance}} {{currency}}</li>
        <li>Previous Balance: {{previousBalance}} {{currency}}</li>
        <li>Change: {{change}} {{currency}}</li>
    </ul>',
    true
),
(
    'USER_REGISTRATION',
    'EMAIL',
    'User Registration Welcome Email',
    'Email template for new user registration',
    'Welcome to Zaphira!',
    '<h2>Welcome to Zaphira</h2>
    <p>Dear {{firstName}},</p>
    <p>Thank you for creating your account with Zaphira.</p>
    <p>Your account is now active and ready to use.</p>
    <p><a href="{{verificationLink}}">Click here to verify your email</a></p>',
    true
),
(
    'PASSWORD_RESET',
    'EMAIL',
    'Password Reset Email',
    'Email template for password reset requests',
    'Reset your Zaphira password',
    '<h2>Password Reset Request</h2>
    <p>Dear {{firstName}},</p>
    <p>We received a request to reset your password.</p>
    <p><a href="{{resetLink}}">Click here to reset your password</a></p>
    <p>This link will expire in 1 hour.</p>',
    true
),
(
    'TRANSACTION_ALERT',
    'SMS',
    'Transaction Alert SMS',
    'SMS template for transaction alerts',
    null,
    'Your Zaphira account: {{amount}} {{currency}} transaction confirmed. Ref: {{transactionId}}. Contact support if not authorized.',
    true
),
(
    'WALLET_LOW_BALANCE',
    'SMS',
    'Wallet Low Balance SMS',
    'SMS template for low balance alerts',
    null,
    'Low balance alert: Your wallet has {{balance}} {{currency}} remaining. Top up now to continue using Zaphira.',
    true
);

-- =============================================================================
-- Insert Default Preferences Template for New Users
-- =============================================================================
-- Note: Default preferences should be created per user via application logic

-- =============================================================================
-- Audit Trigger for updated_at timestamp
-- =============================================================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_notifications_timestamp
BEFORE UPDATE ON notifications
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_templates_timestamp
BEFORE UPDATE ON notification_templates
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_preferences_timestamp
BEFORE UPDATE ON notification_preferences
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- Sequences for ID generation (if needed for other tables)
-- =============================================================================
-- Sequences are typically not needed with UUID generation, but can be added if needed

-- =============================================================================
-- Initial Statistics/Views
-- =============================================================================
-- Create a view for notification summary statistics
CREATE VIEW notification_summary AS
SELECT
    DATE(created_at) as date,
    notification_type,
    status,
    COUNT(*) as count,
    COUNT(CASE WHEN sent_at IS NOT NULL THEN 1 END) as sent_count,
    COUNT(CASE WHEN failed_reason IS NOT NULL THEN 1 END) as failed_count
FROM notifications
GROUP BY DATE(created_at), notification_type, status;

-- Create a view for user notification activity
CREATE VIEW user_notification_activity AS
SELECT
    user_id,
    COUNT(*) as total_notifications,
    COUNT(CASE WHEN status = 'SENT' THEN 1 END) as sent,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending,
    MAX(created_at) as last_notification_at
FROM notifications
GROUP BY user_id;

-- =============================================================================
-- End of Schema
-- =============================================================================
