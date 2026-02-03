-- ============================================================================
-- Consolidated DB updates aligned with latest models
-- ============================================================================

-- Retry counter for transactions (idempotent)
\c zaphira_transactions_db
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS retry_count INT DEFAULT 0;
ALTER TABLE transactions
    ALTER COLUMN retry_count SET DEFAULT 0;

-- No additional wallet schema changes required; merchant fields already present in base schema.
