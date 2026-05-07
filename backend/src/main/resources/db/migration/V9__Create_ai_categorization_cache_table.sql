CREATE TABLE ai_categorization_cache (
       id               BIGINT         NOT NULL AUTO_INCREMENT,
       description_hash VARCHAR(64)    NOT NULL,
       category_id      BIGINT         NOT NULL,
       confidence       DECIMAL(5,4)       NULL,
       hit_count        INT            NOT NULL DEFAULT 1,
       created_at       DATETIME       NOT NULL DEFAULT NOW(),
       expires_at       DATETIME           NULL,
       PRIMARY KEY (id),
       UNIQUE KEY uq_cache_hash (description_hash),
       CONSTRAINT fk_cache_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
       INDEX idx_cache_expires_at (expires_at)
);
