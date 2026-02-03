-- ============================================================================
-- ZAPHIRA Platform - DISPUTE SERVICE Schema
-- ============================================================================
-- Database: zaphira_disputes_db
-- Description: Dispute management, evidence, timeline, resolutions
-- Dependencies: References transactions from zaphira_transactions_db
-- ============================================================================

\c zaphira_disputes_db

-- ============================================================================
-- SECTION 1: ENUMS AND TYPES
-- ============================================================================

CREATE TYPE dispute_status_enum AS ENUM (
    'OPEN', 'PENDING', 'UNDER_REVIEW', 'ESCALATED', 
    'RESOLVED', 'CLOSED', 'WITHDRAWN', 'REJECTED'
);

CREATE TYPE dispute_category_enum AS ENUM (
    'UNAUTHORIZED_TRANSACTION', 'SERVICE_NOT_RECEIVED', 
    'PRODUCT_NOT_AS_DESCRIBED', 'DUPLICATE_CHARGE', 
    'INCORRECT_AMOUNT', 'REFUND_NOT_RECEIVED', 'OTHER'
);

CREATE TYPE resolution_type_enum AS ENUM (
    'FULL_REFUND', 'PARTIAL_REFUND', 'NO_REFUND', 
    'CREDIT_NOTE', 'MERCHANT_FAVOR', 'CUSTOMER_FAVOR'
);

CREATE TYPE initiator_role_enum AS ENUM ('CUSTOMER', 'MERCHANT', 'ADMIN', 'SYSTEM');

CREATE TYPE evidence_type_enum AS ENUM (
    'RECEIPT', 'INVOICE', 'SCREENSHOT', 'EMAIL', 
    'CHAT_LOG', 'TRACKING_INFO', 'PHOTO', 'VIDEO', 
    'DOCUMENT', 'OTHER'
);

-- ============================================================================
-- SECTION 2: MAIN TABLES
-- ============================================================================

-- ============================================================================
-- TABLE: disputes (Main dispute records)
-- ============================================================================
CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    
    -- Transaction reference (logical FK)
    transaction_id BIGINT NOT NULL,
    
    -- Initiator information
    initiated_by BIGINT NOT NULL,
    initiated_by_wallet VARCHAR(8),
    initiator_role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    
    -- Dispute classification
    category VARCHAR(50) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    description TEXT,
    
    -- Financial details
    claimed_amount DECIMAL(19,4) NOT NULL,
    claimed_currency VARCHAR(10) NOT NULL DEFAULT 'XAF',
    
    -- Resolution details
    resolution_type VARCHAR(50),
    resolution_amount DECIMAL(19,4),
    resolution_reason TEXT,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    
    -- References
    reference VARCHAR(100) UNIQUE,
    request_id VARCHAR(100),
    
    -- Deadline and escalation
    deadline_at TIMESTAMP,
    appeal_count INTEGER DEFAULT 0,
    
    -- Evidence tracking
    evidence_submitted_count INTEGER DEFAULT 0,
    last_evidence_submitted_at TIMESTAMP,
    
    -- Resolution
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    
    -- Request tracking
    ip_address VARCHAR(50),
    user_agent TEXT,
    
    -- Metadata
    metadata TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_dispute_status CHECK (status IN (
        'OPEN', 'PENDING', 'UNDER_REVIEW', 'ESCALATED', 
        'RESOLVED', 'CLOSED', 'WITHDRAWN', 'REJECTED'
    )),
    CONSTRAINT chk_dispute_category CHECK (category IN (
        'UNAUTHORIZED_TRANSACTION', 'SERVICE_NOT_RECEIVED', 
        'PRODUCT_NOT_AS_DESCRIBED', 'DUPLICATE_CHARGE', 
        'INCORRECT_AMOUNT', 'REFUND_NOT_RECEIVED', 'OTHER'
    )),
    CONSTRAINT chk_claimed_amount CHECK (claimed_amount >= 0)
);

CREATE INDEX idx_disputes_transaction_id ON disputes(transaction_id);
CREATE INDEX idx_disputes_initiated_by ON disputes(initiated_by);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_category ON disputes(category);
CREATE INDEX idx_disputes_reference ON disputes(reference);
CREATE INDEX idx_disputes_created_at ON disputes(created_at);

COMMENT ON TABLE disputes IS 'Dispute records for contested transactions';

-- ============================================================================
-- TABLE: dispute_evidence (Evidence submissions)
-- ============================================================================
CREATE TABLE dispute_evidence (
    id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    
    evidence_type VARCHAR(50) NOT NULL,
    description TEXT,
    
    -- File information
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    
    -- Submission details
    submitted_by BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Verification
    verified BOOLEAN DEFAULT FALSE,
    verified_by VARCHAR(100),
    verified_at TIMESTAMP,
    verification_notes TEXT,
    
    -- Request tracking
    request_id VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    
    CONSTRAINT fk_dispute_evidence_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE,
    CONSTRAINT chk_evidence_type CHECK (evidence_type IN (
        'RECEIPT', 'INVOICE', 'SCREENSHOT', 'EMAIL', 
        'CHAT_LOG', 'TRACKING_INFO', 'PHOTO', 'VIDEO', 
        'DOCUMENT', 'OTHER'
    ))
);

CREATE INDEX idx_dispute_evidence_dispute_id ON dispute_evidence(dispute_id);
CREATE INDEX idx_dispute_evidence_submitted_by ON dispute_evidence(submitted_by);
CREATE INDEX idx_dispute_evidence_submitted_at ON dispute_evidence(submitted_at);

COMMENT ON TABLE dispute_evidence IS 'Evidence files and documents submitted for disputes';

-- ============================================================================
-- TABLE: dispute_timeline (Activity timeline)
-- ============================================================================
CREATE TABLE dispute_timeline (
    id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT NOT NULL,
    
    -- Status change tracking
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    
    -- Actor information
    actor BIGINT,
    actor_role VARCHAR(20),
    
    -- Request tracking
    request_id VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_dispute_timeline_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE
);

CREATE INDEX idx_dispute_timeline_dispute_id ON dispute_timeline(dispute_id);
CREATE INDEX idx_dispute_timeline_timestamp ON dispute_timeline(event_timestamp);
CREATE INDEX idx_dispute_timeline_event_type ON dispute_timeline(event_type);

COMMENT ON TABLE dispute_timeline IS 'Timeline of all events and activities for disputes';

-- ============================================================================
-- SECTION 3: TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_dispute_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_disputes_updated_at BEFORE UPDATE ON disputes
    FOR EACH ROW EXECUTE FUNCTION update_dispute_updated_at();

-- Log dispute status changes in timeline
CREATE OR REPLACE FUNCTION log_dispute_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO dispute_timeline (
            dispute_id, 
            event_type, 
            event_description,
            old_status,
            new_status,
            actor,
            actor_role
        )
        VALUES (
            NEW.id,
            'STATUS_CHANGE',
            'Dispute status changed from ' || OLD.status || ' to ' || NEW.status,
            OLD.status,
            NEW.status,
            NEW.initiated_by,
            NEW.initiator_role
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_dispute_status_changes AFTER UPDATE ON disputes
    FOR EACH ROW EXECUTE FUNCTION log_dispute_status_change();

-- Update evidence count when evidence is added
CREATE OR REPLACE FUNCTION update_dispute_evidence_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE disputes
    SET evidence_submitted_count = evidence_submitted_count + 1,
        last_evidence_submitted_at = CURRENT_TIMESTAMP
    WHERE id = NEW.dispute_id;
    
    -- Log in timeline
    INSERT INTO dispute_timeline (
        dispute_id,
        event_type,
        event_description,
        actor
    )
    VALUES (
        NEW.dispute_id,
        'EVIDENCE_SUBMITTED',
        'New evidence submitted: ' || NEW.evidence_type,
        NEW.submitted_by
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_evidence_submission AFTER INSERT ON dispute_evidence
    FOR EACH ROW EXECUTE FUNCTION update_dispute_evidence_count();

-- ============================================================================
-- SECTION 4: UTILITY FUNCTIONS
-- ============================================================================

-- Generate unique dispute reference
CREATE OR REPLACE FUNCTION generate_dispute_reference()
RETURNS VARCHAR(100) AS $$
DECLARE
    new_ref VARCHAR(100);
    exists BOOLEAN;
BEGIN
    LOOP
        new_ref := 'DSP-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-' || 
                   LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        SELECT EXISTS(SELECT 1 FROM disputes WHERE reference = new_ref) INTO exists;
        
        IF NOT exists THEN
            RETURN new_ref;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Check if dispute is within SLA deadline
CREATE OR REPLACE FUNCTION is_dispute_within_sla(p_dispute_id BIGINT)
RETURNS BOOLEAN AS $$
DECLARE
    v_deadline TIMESTAMP;
BEGIN
    SELECT deadline_at INTO v_deadline
    FROM disputes
    WHERE id = p_dispute_id;
    
    RETURN v_deadline IS NULL OR v_deadline > CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 5: VIEWS
-- ============================================================================

-- Active disputes
CREATE OR REPLACE VIEW v_active_disputes AS
SELECT 
    d.id,
    d.transaction_id,
    d.reference,
    d.initiated_by,
    d.category,
    d.status,
    d.claimed_amount,
    d.claimed_currency,
    d.created_at,
    d.deadline_at,
    d.evidence_submitted_count,
    CASE 
        WHEN d.deadline_at IS NOT NULL AND d.deadline_at < CURRENT_TIMESTAMP 
        THEN TRUE 
        ELSE FALSE 
    END as is_overdue
FROM disputes d
WHERE d.status IN ('OPEN', 'PENDING', 'UNDER_REVIEW', 'ESCALATED');

-- Dispute summary with evidence count
CREATE OR REPLACE VIEW v_dispute_summary AS
SELECT 
    d.id,
    d.transaction_id,
    d.reference,
    d.status,
    d.category,
    d.claimed_amount,
    d.claimed_currency,
    d.created_at,
    d.resolved_at,
    COUNT(de.id) as evidence_count,
    COUNT(dt.id) as timeline_event_count
FROM disputes d
LEFT JOIN dispute_evidence de ON d.id = de.dispute_id
LEFT JOIN dispute_timeline dt ON d.id = dt.dispute_id
GROUP BY d.id;

-- Resolution statistics
CREATE OR REPLACE VIEW v_dispute_resolution_stats AS
SELECT 
    DATE(resolved_at) as resolution_date,
    resolution_type,
    COUNT(*) as dispute_count,
    SUM(claimed_amount) as total_claimed_amount,
    SUM(resolution_amount) as total_resolution_amount,
    AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))/3600) as avg_resolution_hours
FROM disputes
WHERE status = 'RESOLVED'
  AND resolved_at IS NOT NULL
GROUP BY DATE(resolved_at), resolution_type;

-- ============================================================================
-- END OF DISPUTE SERVICE SCHEMA
-- ============================================================================
