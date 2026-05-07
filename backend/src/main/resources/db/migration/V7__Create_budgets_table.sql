CREATE TABLE budgets (
      id          BIGINT         NOT NULL AUTO_INCREMENT,
      user_id     BIGINT         NOT NULL,
      family_id   BIGINT             NULL,
      category_id BIGINT         NOT NULL,
      amount      DECIMAL(15,2)  NOT NULL,
      month       VARCHAR(7)     NOT NULL,
      created_at  DATETIME       NOT NULL DEFAULT NOW(),

      PRIMARY KEY (id),
      UNIQUE KEY uq_budget_personal (user_id, category_id, month),
      CONSTRAINT fk_budget_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
      CONSTRAINT fk_budget_family   FOREIGN KEY (family_id)   REFERENCES families(id)   ON DELETE SET NULL,
      CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
      INDEX idx_budgets_user_month   (user_id, month),
      INDEX idx_budgets_family_month (family_id, month)
);
