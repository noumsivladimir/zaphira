-- Flyway Migration: V20251216_2__add_transaction_audit_log_table.sql
-- Créé pour Phase 2: Reversal & Refund Services
-- Table pour la traçabilité complète des opérations sensibles

CREATE TABLE IF NOT EXISTS transaction_audit_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_email VARCHAR(255),
    actor_role VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3),
    reason VARCHAR(500),
    details TEXT,
    status_before VARCHAR(50),
    status_after VARCHAR(50),
    result VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_id VARCHAR(255),
    request_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes pour recherches rapides
CREATE INDEX idx_audit_log_transaction_id ON transaction_audit_log(transaction_id);
CREATE INDEX idx_audit_log_actor_id ON transaction_audit_log(actor_user_id);
CREATE INDEX idx_audit_log_action_type ON transaction_audit_log(action_type);
CREATE INDEX idx_audit_log_result ON transaction_audit_log(result);
CREATE INDEX idx_audit_log_timestamp ON transaction_audit_log(timestamp);
CREATE INDEX idx_audit_log_actor_action_time ON transaction_audit_log(actor_user_id, action_type, timestamp);

-- Index composite pour recherches complexes
CREATE INDEX idx_audit_log_complex ON transaction_audit_log(transaction_id, actor_user_id, action_type, timestamp);

-- Comment for audit trail
COMMENT ON TABLE transaction_audit_log IS 'Complete audit trail for sensitive transaction operations (Reversal, Refund). REQUIRED for compliance and regulatory purposes.';

-- Politique de rétention (PostgreSQL)
-- Les logs d'audit ne doivent pas être supprimés, seulement archivés après X années
-- À configurer avec une tâche de scheduled maintenance

-- ============================================================
-- TABLE: transaction_reversals (optionnel - pour tracking des reversals)
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_reversals (
    id BIGSERIAL PRIMARY KEY,
    original_transaction_id BIGINT NOT NULL,
    reversal_transaction_id BIGINT NOT NULL,
    reversal_reference VARCHAR(255) UNIQUE NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    fees NUMERIC(19, 4),
    currency VARCHAR(3),
    reason VARCHAR(500),
    performed_by_user_id BIGINT NOT NULL,
    performed_by_role VARCHAR(50),
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (original_transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_reversals_original_tx ON transaction_reversals(original_transaction_id);
CREATE INDEX idx_reversals_reversal_tx ON transaction_reversals(reversal_transaction_id);
CREATE INDEX idx_reversals_actor ON transaction_reversals(performed_by_user_id);

-- ============================================================
-- TABLE: transaction_refunds (optionnel - pour tracking des refunds)
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_refunds (
    id BIGSERIAL PRIMARY KEY,
    original_transaction_id BIGINT NOT NULL,
    refund_transaction_id BIGINT NOT NULL,
    refund_reference VARCHAR(255) UNIQUE NOT NULL,
    refund_amount NUMERIC(19, 4) NOT NULL,
    refund_fees NUMERIC(19, 4),
    total_refund_amount NUMERIC(19, 4),
    currency VARCHAR(3),
    refund_type VARCHAR(20) DEFAULT 'FULL',
    reason VARCHAR(500),
    performed_by_user_id BIGINT NOT NULL,
    performed_by_role VARCHAR(50),
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (original_transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_refunds_original_tx ON transaction_refunds(original_transaction_id);
CREATE INDEX idx_refunds_refund_tx ON transaction_refunds(refund_transaction_id);
CREATE INDEX idx_refunds_actor ON transaction_refunds(performed_by_user_id);
CREATE INDEX idx_refunds_type ON transaction_refunds(refund_type);

-- Vues pour analytics (optionnel)
CREATE OR REPLACE VIEW v_audit_summary AS
SELECT 
    action_type,
    DATE(timestamp) as action_date,
    COUNT(*) as total_actions,
    COUNT(CASE WHEN result = 'SUCCESS' THEN 1 END) as success_count,
    COUNT(CASE WHEN result = 'FAILED' THEN 1 END) as failed_count,
    SUM(amount) as total_amount
FROM transaction_audit_log
GROUP BY action_type, DATE(timestamp)
ORDER BY action_date DESC, action_type;

CREATE OR REPLACE VIEW v_user_actions AS
SELECT 
    actor_user_id,
    actor_email,
    actor_role,
    action_type,
    COUNT(*) as action_count,
    COUNT(CASE WHEN result = 'SUCCESS' THEN 1 END) as success_count,
    MAX(timestamp) as last_action
FROM transaction_audit_log
GROUP BY actor_user_id, actor_email, actor_role, action_type
ORDER BY max(timestamp) DESC;
