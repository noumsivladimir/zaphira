-- Add retry_count to transactions for retry tracking
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS retry_count INT DEFAULT 0;
