-- ============================================================
-- PHASE 3: DISPUTE MANAGEMENT SCHEMA
-- Version: 20251216_3
-- Description: Create dispute management tables with comprehensive
--              tracking, evidence management, and timeline support
-- ============================================================

-- ============================================================
-- DISPUTES TABLE
-- ============================================================
-- Main table for dispute management
-- Tracks dispute lifecycle, status, and resolution
-- Supports 14-day evidence submission window
-- Includes comprehensive audit fields

CREATE TABLE IF NOT EXISTS disputes (
    -- Primary Key & Identity
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    reference VARCHAR(30) NOT NULL UNIQUE,  -- DSP-XXXXXXXXX format
    
    -- Foreign Keys
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    
    -- Dispute Classification
    category VARCHAR(50) NOT NULL,  -- FRAUD, DUPLICATE_CHARGE, SERVICE_NOT_PROVIDED, etc
    reason TEXT NOT NULL,  -- Reason provided by customer
    description TEXT,  -- Detailed description
    
    -- Amounts & Currency
    claimed_amount NUMERIC(19, 4) NOT NULL,  -- Amount customer claims
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Status & Lifecycle
    status VARCHAR(50) NOT NULL,  -- INITIATED, UNDER_INVESTIGATION, AWAITING_EVIDENCE, etc
    initiator_role VARCHAR(50) NOT NULL,  -- CUSTOMER, MERCHANT, ADMIN, SYSTEM
    
    -- Deadline Management
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deadline_at TIMESTAMP NOT NULL,  -- 14 days from creation
    
    -- Initiator Information
    initiated_by VARCHAR(255) NOT NULL,  -- Email of who created dispute
    
    -- Resolution Information (Set when dispute is RESOLVED)
    resolution_type VARCHAR(50),  -- APPROVED, DENIED, PARTIAL_APPROVAL, SETTLEMENT, WITHDRAWN, EXPIRED, ESCALATED_TO_BANK
    resolution_amount NUMERIC(19, 4),  -- Amount from resolution
    resolved_by VARCHAR(255),  -- Admin who resolved
    resolved_at TIMESTAMP,  -- When resolved
    
    -- Audit Fields
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_disputes_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT ck_dispute_status CHECK (status IN ('INITIATED', 'UNDER_INVESTIGATION', 'AWAITING_EVIDENCE', 'AWAITING_RESPONSE', 'RESOLVED', 'CLOSED', 'APPEAL_REQUESTED', 'ESCALATED', 'EXPIRED')),
    CONSTRAINT ck_resolution_type CHECK (resolution_type IS NULL OR resolution_type IN ('APPROVED', 'PARTIAL_APPROVAL', 'DENIED', 'SETTLEMENT', 'WITHDRAWN', 'EXPIRED', 'ESCALATED_TO_BANK')),
    CONSTRAINT ck_initiator_role CHECK (initiator_role IN ('CUSTOMER', 'MERCHANT', 'ADMIN', 'SYSTEM')),
    CONSTRAINT ck_claimed_amount CHECK (claimed_amount > 0),
    CONSTRAINT ck_resolution_amount CHECK (resolution_amount IS NULL OR resolution_amount >= 0)
);

-- Create indexes for common queries
CREATE INDEX idx_disputes_transaction_id ON disputes(transaction_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_created_at ON disputes(created_at DESC);
CREATE INDEX idx_disputes_initiator ON disputes(initiated_by);
CREATE INDEX idx_disputes_reference ON disputes(reference);
CREATE INDEX idx_disputes_deadline ON disputes(deadline_at) WHERE status NOT IN ('RESOLVED', 'CLOSED', 'EXPIRED');

-- ============================================================
-- DISPUTE_EVIDENCE TABLE
-- ============================================================
-- Tracks evidence submitted in support of disputes
-- Supports file upload metadata
-- Includes verification tracking for admin review

CREATE TABLE IF NOT EXISTS dispute_evidence (
    -- Primary Key & Identity
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    
    -- Foreign Key
    dispute_id BIGINT NOT NULL,
    
    -- Evidence Classification
    evidence_type VARCHAR(50) NOT NULL,  -- RECEIPT, INVOICE, DELIVERY_PROOF, COMMUNICATION, etc
    
    -- File Information
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,  -- S3/Cloud Storage URL
    file_size BIGINT,  -- In bytes
    mime_type VARCHAR(100),  -- image/png, application/pdf, etc
    
    -- Evidence Details
    description TEXT,
    
    -- Submission Information
    submitted_by VARCHAR(255) NOT NULL,  -- Email of submitter
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Verification Information
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by VARCHAR(255),  -- Admin who verified
    verified_at TIMESTAMP,
    verification_notes TEXT,
    
    -- Audit Information
    ip_address VARCHAR(45),  -- IPv4 or IPv6
    user_agent VARCHAR(500),  -- Browser user agent
    request_id VARCHAR(100),  -- Distributed tracing ID
    
    -- Constraints
    CONSTRAINT fk_evidence_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE,
    CONSTRAINT ck_evidence_type CHECK (evidence_type IN ('RECEIPT', 'INVOICE', 'DELIVERY_PROOF', 'COMMUNICATION', 'REFUND_PROOF', 'PROOF_OF_IDENTITY', 'PRODUCT_PHOTO', 'CONTRACT', 'EMAIL', 'SCREENSHOT', 'OTHER')),
    CONSTRAINT ck_file_size CHECK (file_size > 0 AND file_size <= 52428800)  -- Max 50MB
);

-- Create indexes for evidence queries
CREATE INDEX idx_evidence_dispute_id ON dispute_evidence(dispute_id);
CREATE INDEX idx_evidence_type ON dispute_evidence(evidence_type);
CREATE INDEX idx_evidence_submitted_at ON dispute_evidence(submitted_at DESC);
CREATE INDEX idx_evidence_verified ON dispute_evidence(verified);

-- ============================================================
-- DISPUTE_TIMELINE TABLE
-- ============================================================
-- Audit trail for all events in dispute lifecycle
-- Tracks status transitions, evidence additions, resolutions
-- Provides complete history for accountability

CREATE TABLE IF NOT EXISTS dispute_timeline (
    -- Primary Key & Identity
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    
    -- Foreign Key
    dispute_id BIGINT NOT NULL,
    
    -- Event Information
    event_type VARCHAR(100) NOT NULL,  -- CREATED, STATUS_CHANGED, EVIDENCE_ADDED, RESOLVED, etc
    event_description TEXT,
    
    -- Actor Information
    actor VARCHAR(255) NOT NULL,  -- Email/ID of who performed action
    actor_role VARCHAR(50),  -- CUSTOMER, MERCHANT, ADMIN, SYSTEM
    
    -- State Transition (for status changes)
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    
    -- Timing
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit Information
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_id VARCHAR(100),
    
    -- Constraints
    CONSTRAINT fk_timeline_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE
);

-- Create indexes for timeline queries
CREATE INDEX idx_timeline_dispute_id ON dispute_timeline(dispute_id);
CREATE INDEX idx_timeline_event_type ON dispute_timeline(event_type);
CREATE INDEX idx_timeline_event_timestamp ON dispute_timeline(event_timestamp DESC);
CREATE INDEX idx_timeline_actor_role ON dispute_timeline(actor_role);

-- ============================================================
-- COMMENTS & DOCUMENTATION
-- ============================================================

COMMENT ON TABLE disputes IS 'Main dispute management table. Tracks customer disputes for transactions including status, evidence, and resolutions.';
COMMENT ON COLUMN disputes.reference IS 'Unique dispute reference (DSP-XXXXXXXXX). Used for customer communication and as Kafka message key.';
COMMENT ON COLUMN disputes.claimed_amount IS 'Amount customer claims is in dispute. Must be <= transaction amount.';
COMMENT ON COLUMN disputes.deadline_at IS 'Evidence submission deadline. Typically 14 days from creation. Disputes auto-expire after deadline.';
COMMENT ON COLUMN disputes.status IS 'Lifecycle status: INITIATED → UNDER_INVESTIGATION → RESOLVED or EXPIRED.';
COMMENT ON COLUMN disputes.resolution_type IS 'Final decision type: APPROVED (full refund), PARTIAL_APPROVAL (split), DENIED (no refund), SETTLEMENT, WITHDRAWN, EXPIRED, ESCALATED_TO_BANK.';

COMMENT ON TABLE dispute_evidence IS 'Evidence files submitted in support of disputes. Both customers and merchants can submit evidence. Includes verification tracking for admin review.';
COMMENT ON COLUMN dispute_evidence.file_url IS 'URL to uploaded file in S3 or cloud storage. Example: s3://bucket/disputes/DSP-ABC123/receipt.pdf';
COMMENT ON COLUMN dispute_evidence.verified IS 'Whether admin has reviewed and verified the evidence. Used for filtering unreviewed evidence.';

COMMENT ON TABLE dispute_timeline IS 'Audit trail of all dispute lifecycle events. Every action (create, status change, evidence added, resolved) creates a timeline entry. Provides complete accountability.';
COMMENT ON COLUMN dispute_timeline.event_type IS 'Type of event: DISPUTE_CREATED, STATUS_CHANGED, EVIDENCE_ADDED, DISPUTE_RESOLVED, APPEAL_REQUESTED, ESCALATED, etc.';
COMMENT ON COLUMN dispute_timeline.actor IS 'Email/ID of person/system that performed the action. Examples: customer@example.com, admin@company.com, SYSTEM';
COMMENT ON COLUMN dispute_timeline.old_status IS 'Previous status (only for STATUS_CHANGED events). Null for other event types.';
COMMENT ON COLUMN dispute_timeline.new_status IS 'New status after event. For STATUS_CHANGED: shows transition. For CREATE: shows initial status.';

-- ============================================================
-- SAMPLE DATA (for testing)
-- ============================================================
-- Uncomment to populate with sample dispute data

/*
INSERT INTO disputes (
    reference, transaction_id, category, reason, description,
    claimed_amount, currency, status, initiator_role, 
    initiated_by, deadline_at
) VALUES (
    'DSP-' || SUBSTR(HEX(RANDOM()), 1, 12),
    'TXN-' || SUBSTR(HEX(RANDOM()), 1, 12),
    'FRAUDULENT_TRANSACTION',
    'I did not authorize this transaction',
    'Card was stolen and used without my permission',
    250.00,
    'USD',
    'INITIATED',
    'CUSTOMER',
    'customer@example.com',
    CURRENT_TIMESTAMP + INTERVAL 14 DAY
);

INSERT INTO dispute_timeline (
    dispute_id, event_type, event_description, actor, actor_role, new_status
) SELECT
    id, 'DISPUTE_CREATED', 'Dispute created', initiated_by, 'CUSTOMER', status
FROM disputes
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL 1 HOUR;
*/
