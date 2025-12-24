-- ============================================================
-- DATABASE MIGRATION: Add Search and Filtering Indexes
-- ============================================================
-- Date: 2025-12-16
-- Purpose: Create indexes to optimize advanced search queries
-- Impact: Improved query performance for transaction searches
-- Rollback: Drop each index if needed
-- ============================================================

-- Index pour recherche par sender wallet
CREATE INDEX IF NOT EXISTS idx_transactions_sender_wallet 
ON transactions(sender_wallet_number);

-- Index pour recherche par receiver wallet
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_wallet 
ON transactions(receiver_wallet_number);

-- Index composite pour amount et created_at (common filters)
CREATE INDEX IF NOT EXISTS idx_transactions_amount_created 
ON transactions(amount, created_at);

-- Index composite pour status et created_at (very common)
CREATE INDEX IF NOT EXISTS idx_transactions_status_created 
ON transactions(status, created_at DESC);

-- Index composite pour type et currency
CREATE INDEX IF NOT EXISTS idx_transactions_type_currency 
ON transactions(type, currency);

-- Index pour recherche par date de création (sorting)
CREATE INDEX IF NOT EXISTS idx_transactions_created_at 
ON transactions(created_at DESC);

-- Index pour recherche par reference (exact match or like)
CREATE INDEX IF NOT EXISTS idx_transactions_reference 
ON transactions(reference);

-- Index pour recherche par currency
CREATE INDEX IF NOT EXISTS idx_transactions_currency 
ON transactions(currency);

-- Index composite pour sender + status (common filter combination)
CREATE INDEX IF NOT EXISTS idx_transactions_sender_status 
ON transactions(sender_wallet_number, status);

-- Index composite pour receiver + status (common filter combination)
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_status 
ON transactions(receiver_wallet_number, status);

-- Index pour recherche par route
CREATE INDEX IF NOT EXISTS idx_transactions_route 
ON transactions(route);

-- Index pour recherche par scheduled flag
CREATE INDEX IF NOT EXISTS idx_transactions_scheduled 
ON transactions(scheduled, created_at DESC);

-- Index composite pour recherche par plage de date et status
CREATE INDEX IF NOT EXISTS idx_transactions_status_date_range 
ON transactions(status, created_at DESC, amount);

COMMIT;
