-- Sprint 5: distinguish personal vs shared context
ALTER TABLE accounts
    ADD COLUMN scope ENUM('PERSONAL', 'SHARED') NOT NULL DEFAULT 'PERSONAL' AFTER family_id;

ALTER TABLE transactions
    ADD COLUMN scope ENUM('PERSONAL', 'SHARED') NOT NULL DEFAULT 'PERSONAL' AFTER family_id;

UPDATE accounts
SET scope = 'SHARED'
WHERE family_id IS NOT NULL;

UPDATE transactions
SET scope = 'SHARED'
WHERE family_id IS NOT NULL;

CREATE INDEX idx_accounts_family_scope ON accounts (family_id, scope);
