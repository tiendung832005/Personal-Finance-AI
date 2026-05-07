CREATE TABLE users (
      id            BIGINT         NOT NULL AUTO_INCREMENT,
      email         VARCHAR(255)   NOT NULL,
      password_hash VARCHAR(255)   NOT NULL,
      full_name     VARCHAR(100)   NOT NULL,
      avatar_url    VARCHAR(500)       NULL,
      is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
      family_id     BIGINT             NULL,
      created_at    DATETIME       NOT NULL DEFAULT NOW(),
      updated_at    DATETIME       NOT NULL DEFAULT NOW() ON UPDATE NOW(),

           PRIMARY KEY (id),
           UNIQUE KEY uq_users_email (email),
           CONSTRAINT fk_users_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE SET NULL,
           INDEX idx_users_family_id (family_id)
);

ALTER TABLE families
    ADD CONSTRAINT fk_families_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT;
