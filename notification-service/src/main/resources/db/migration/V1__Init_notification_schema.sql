-- Initialize Notification Service Database Schema
-- PostgreSQL

-- Drop existing tables if they exist (for clean migrations)
DROP TABLE IF EXISTS notification_logs CASCADE;
DROP TABLE IF EXISTS notification_preferences CASCADE;
DROP TABLE IF EXISTS notification_templates CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;

-- Notifications table - stores all user notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ', 'ARCHIVED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH')),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_event_type ON notifications(event_type);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);

-- Notification templates table - template definitions for events
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL UNIQUE,
    title_template VARCHAR(255) NOT NULL,
    message_template TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for templates
CREATE INDEX idx_templates_event_type ON notification_templates(event_type);

-- Pre-populate common templates
INSERT INTO notification_templates (event_type, title_template, message_template, description, is_active)
VALUES
    ('TransactionCompleted', 'Transaction Successful', 'Your transaction of ${amount} ${currency} to ${recipient} has been completed successfully.', 'Transaction completion confirmation', TRUE),
    ('TransactionFailed', 'Transaction Failed', 'Your transaction of ${amount} ${currency} failed. Reason: ${reason}. Please try again or contact support.', 'Transaction failure notification', TRUE),
    ('WalletBalanceUpdated', 'Balance Updated', 'Your ${walletType} wallet balance has been updated to ${balance} ${currency}.', 'Wallet balance update notification', TRUE),
    ('WalletLowBalance', 'Low Balance Alert', 'Your ${walletType} wallet balance is running low (${balance} ${currency}). Please top up to continue using our services.', 'Low balance warning', TRUE),
    ('WalletFrozen', 'Wallet Frozen', 'Your ${walletType} wallet has been frozen due to ${reason}. Contact support for assistance.', 'Wallet freeze notification', TRUE),
    ('UserAccountCreated', 'Account Created', 'Welcome! Your account has been successfully created. Start using our services by creating a wallet.', 'New account confirmation', TRUE),
    ('UserPasswordChanged', 'Password Changed', 'Your password has been successfully changed. If you did not make this change, please contact support immediately.', 'Password change confirmation', TRUE),
    ('UserAccountSuspended', 'Account Suspended', 'Your account has been suspended. Reason: ${reason}. Please contact support for more information.', 'Account suspension notice', TRUE);

-- Notification preferences table - user preferences for notifications
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    channels TEXT,  -- JSON array: ["IN_APP", "EMAIL"]
    quiet_start TIME,
    quiet_end TIME,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, event_type)
);

-- Indexes for preferences
CREATE INDEX idx_preferences_user_id ON notification_preferences(user_id);
CREATE INDEX idx_preferences_event_type ON notification_preferences(event_type);

-- Notification logs table - audit trail for dispatch attempts
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ', 'ARCHIVED')),
    attempt_number INTEGER NOT NULL,
    error_message TEXT,
    response_code VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for logs
CREATE INDEX idx_logs_notification_id ON notification_logs(notification_id);
CREATE INDEX idx_logs_user_id ON notification_logs(user_id);
CREATE INDEX idx_logs_created_at ON notification_logs(created_at);

-- View: User notification statistics
CREATE VIEW user_notification_stats AS
SELECT 
    user_id,
    COUNT(*) as total_notifications,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as unread_count,
    COUNT(CASE WHEN status = 'READ' THEN 1 END) as read_count,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count,
    MAX(created_at) as last_notification_at
FROM notifications
GROUP BY user_id;

-- Audit function for updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for automatic timestamp updates
CREATE TRIGGER notifications_update_trigger
BEFORE UPDATE ON notifications
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER templates_update_trigger
BEFORE UPDATE ON notification_templates
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER preferences_update_trigger
BEFORE UPDATE ON notification_preferences
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();
