-- Sprint 7 – T01: Bảng ai_insights (thiết kế lại toàn bộ, thay thế V10)
-- V10 đã khai báo ai_insights với schema cũ (type ENUM cứng, family_id).
-- V21 tạo bảng sprint7_ai_insights sát sprint plan hơn (group_id linh hoạt, UNIQUE per user+month+insight_type).
CREATE TABLE sprint7_ai_insights (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT           NULL,
    group_id     BIGINT           NULL,
    month        DATE         NOT NULL,           -- ngày đầu tháng: 2026-05-01
    insight_type VARCHAR(30)  NOT NULL,           -- PERSONAL, FAMILY
    content      TEXT         NOT NULL,           -- nội dung AI sinh ra (tiếng Việt)
    model        VARCHAR(50)      NULL,
    tokens_used  INT              NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_insight (user_id, month, insight_type),
    CONSTRAINT fk_sprint7_insight_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
