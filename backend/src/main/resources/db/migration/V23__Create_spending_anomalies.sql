-- Sprint 7 – T03: Bảng spending_anomalies
CREATE TABLE spending_anomalies (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    transaction_id BIGINT       NOT NULL,
    anomaly_type   VARCHAR(50)  NOT NULL,   -- UNUSUAL_AMOUNT, NEW_CATEGORY, FREQUENCY_SPIKE, LARGE_SINGLE_TXN
    severity       VARCHAR(10)  NOT NULL,   -- LOW, MEDIUM, HIGH
    explanation    TEXT             NULL,   -- Gemini giải thích (điền async)
    is_dismissed   BOOLEAN      NOT NULL DEFAULT FALSE,
    detected_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_anomaly_user FOREIGN KEY (user_id)        REFERENCES users(id),
    CONSTRAINT fk_anomaly_txn  FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    INDEX idx_anomaly_user_detected (user_id, detected_at),
    INDEX idx_anomaly_txn (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
