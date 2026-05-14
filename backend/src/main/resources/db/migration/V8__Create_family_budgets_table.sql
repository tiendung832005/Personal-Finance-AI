CREATE TABLE family_budgets (
     id          BIGINT         NOT NULL AUTO_INCREMENT,
     family_id   BIGINT         NOT NULL,
     category_id BIGINT         NOT NULL,
     created_by  BIGINT         NOT NULL,
     amount      DECIMAL(15,2)  NOT NULL,
     month       VARCHAR(7)     NOT NULL,
     created_at  DATETIME       NOT NULL DEFAULT NOW(),
     PRIMARY KEY (id),
     UNIQUE KEY uq_family_budget (family_id, category_id, month),
     CONSTRAINT fk_fb_family   FOREIGN KEY (family_id)   REFERENCES families(id)   ON DELETE CASCADE,
     CONSTRAINT fk_fb_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
     CONSTRAINT fk_fb_user     FOREIGN KEY (created_by)  REFERENCES users(id)      ON DELETE RESTRICT,
     INDEX idx_family_budgets_month (family_id, month)
);

