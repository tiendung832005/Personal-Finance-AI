CREATE TABLE families (
                          id          BIGINT         NOT NULL AUTO_INCREMENT,
                          name        VARCHAR(100)   NOT NULL,
                          description VARCHAR(500)       NULL,
                          created_by  BIGINT         NOT NULL,
                          created_at  DATETIME       NOT NULL DEFAULT NOW(),
                          PRIMARY KEY (id),
                          INDEX idx_families_created_by (created_by)
);
