-- Sprint 5: group budgets use family_id with user_id = NULL
ALTER TABLE budgets
    MODIFY user_id BIGINT NULL;

ALTER TABLE budgets
    ADD UNIQUE KEY uq_budget_group (family_id, category_id, month);
