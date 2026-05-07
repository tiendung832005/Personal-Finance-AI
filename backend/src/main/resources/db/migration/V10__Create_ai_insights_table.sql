CREATE TABLE ai_insights (
      id         BIGINT       NOT NULL AUTO_INCREMENT,
      user_id    BIGINT           NULL,
      family_id  BIGINT           NULL,
      month      VARCHAR(7)   NOT NULL,
      type       ENUM('MONTHLY_SUMMARY','ANOMALY_REPORT','SPENDING_PREDICTION','HEALTH_SCORE','FAMILY_SUMMARY') NOT NULL,
      content    TEXT         NOT NULL,
      metadata   JSON             NULL,
      created_at DATETIME     NOT NULL DEFAULT NOW(),
      PRIMARY KEY (id),
      CONSTRAINT fk_insight_user   FOREIGN KEY (user_id)   REFERENCES users(id)    ON DELETE CASCADE,
      CONSTRAINT fk_insight_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
      INDEX idx_insights_user_month   (user_id, month),
      INDEX idx_insights_family_month (family_id, month)
);
