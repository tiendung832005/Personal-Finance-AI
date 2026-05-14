ALTER TABLE accounts
    ADD COLUMN deleted_at DATETIME NULL AFTER created_at,
    ADD INDEX idx_accounts_deleted_at (deleted_at);
