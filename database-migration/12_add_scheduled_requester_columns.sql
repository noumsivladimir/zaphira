-- Delta migration: add requester info to scheduled_transactions
-- Safe for re-run (IF NOT EXISTS)

ALTER TABLE scheduled_transactions
    ADD COLUMN IF NOT EXISTS requester_user_id BIGINT;

ALTER TABLE scheduled_transactions
    ADD COLUMN IF NOT EXISTS requester_roles VARCHAR(255);
