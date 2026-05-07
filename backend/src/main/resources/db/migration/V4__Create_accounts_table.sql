CREATE TABLE accounts (
     id          BIGINT         NOT NULL AUTO_INCREMENT,
     user_id     BIGINT         NOT NULL,
     family_id   BIGINT             NULL,
     name        VARCHAR(100)   NOT NULL,
     type        ENUM('CASH','BANK','CREDIT_CARD','E_WALLET') NOT NULL,
     balance     DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
     currency    VARCHAR(3)     NOT NULL DEFAULT 'VND',
     is_default  BOOLEAN        NOT NULL DEFAULT FALSE,
     created_at  DATETIME       NOT NULL DEFAULT NOW(),

        PRIMARY KEY (id),
        CONSTRAINT fk_accounts_user   FOREIGN KEY (user_id)   REFERENCES users(id)    ON DELETE CASCADE,
        CONSTRAINT fk_accounts_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE SET NULL,
        INDEX idx_accounts_user_id   (user_id),
        INDEX idx_accounts_family_id (family_id)
);
