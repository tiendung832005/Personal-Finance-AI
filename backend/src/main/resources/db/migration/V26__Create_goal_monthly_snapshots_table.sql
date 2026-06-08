CREATE TABLE goal_monthly_snapshots (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id        BIGINT NOT NULL,
    month          DATE NOT NULL,
    saved_amount   DECIMAL(15,2) NOT NULL,
    planned_amount DECIMAL(15,2) NULL,
    on_track       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_goal_monthly_snapshots_goal_month (goal_id, month),
    CONSTRAINT fk_goal_monthly_snapshots_goal
        FOREIGN KEY (goal_id) REFERENCES financial_goals(id) ON DELETE CASCADE,
    INDEX idx_goal_monthly_snapshots_goal_month (goal_id, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
