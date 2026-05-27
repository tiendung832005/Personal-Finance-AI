-- Sprint 7 – T02: Bảng financial_health_scores
CREATE TABLE financial_health_scores (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id              BIGINT      NOT NULL,
    month                DATE        NOT NULL,           -- ngày đầu tháng: 2026-05-01
    overall_score        INT         NOT NULL,           -- 0-100
    savings_score        INT             NULL,           -- điểm tiết kiệm
    budget_score         INT             NULL,           -- điểm tuân thủ budget
    spending_trend_score INT             NULL,           -- điểm xu hướng chi tiêu
    debt_score           INT             NULL,           -- điểm nợ (nếu có)
    score_label          VARCHAR(20)     NULL,           -- POOR / FAIR / GOOD / EXCELLENT
    calculated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_health_score (user_id, month),
    CONSTRAINT fk_hs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
