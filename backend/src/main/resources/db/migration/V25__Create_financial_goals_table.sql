CREATE TABLE financial_goals (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    target_amount        DECIMAL(15,2) NOT NULL,
    deadline             DATE NOT NULL,
    linked_account_id    BIGINT NULL,
    status               ENUM('ACTIVE','COMPLETED','OVERDUE','PAUSED') NOT NULL DEFAULT 'ACTIVE',
    ai_plan              TEXT NULL,
    ai_plan_generated_at DATETIME NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_financial_goals_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_financial_goals_account
        FOREIGN KEY (linked_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    INDEX idx_financial_goals_user_deadline (user_id, deadline),
    INDEX idx_financial_goals_status (status),
    INDEX idx_financial_goals_linked_account (linked_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
