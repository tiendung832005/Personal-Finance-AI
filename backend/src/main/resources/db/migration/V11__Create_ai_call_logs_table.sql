CREATE TABLE ai_call_logs (
     id                BIGINT        NOT NULL AUTO_INCREMENT,
     user_id           BIGINT            NULL,
     feature           VARCHAR(50)   NOT NULL,
     model             VARCHAR(50)       NULL,
     prompt_tokens     INT               NULL,
     completion_tokens INT               NULL,
     latency_ms        INT               NULL,
     success           BOOLEAN       NOT NULL,
     error_message     TEXT              NULL,
     created_at        DATETIME      NOT NULL DEFAULT NOW(),

     PRIMARY KEY (id),
     CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
     INDEX idx_ai_logs_user_id (user_id),
     INDEX idx_ai_logs_feature (feature),
     INDEX idx_ai_logs_created (created_at)
);
