-- Demo data for Summary / Trend manual testing (user summary-seed@pfia.local, password: password)
SET @seed_email := 'summary-seed@pfia.local';

INSERT INTO users (email, password_hash, full_name, is_active)
SELECT @seed_email,
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       'Summary Demo User',
       TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = @seed_email);

SET @seed_uid := (SELECT id FROM users WHERE email = @seed_email LIMIT 1);

INSERT INTO accounts (user_id, family_id, name, type, balance, currency, is_default, created_at, deleted_at)
SELECT @seed_uid,NULL,'Ví tiền mặt','CASH',5000000.00,
       'VND',TRUE,NOW(),NULL
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM accounts a
    WHERE a.user_id = @seed_uid
      AND a.name = 'Ví tiền mặt'
      AND a.deleted_at IS NULL
);

SET @seed_aid := (
    SELECT id
    FROM accounts
    WHERE user_id = @seed_uid
      AND name = 'Ví tiền mặt'
      AND deleted_at IS NULL
    ORDER BY id DESC
    LIMIT 1
);

DELETE FROM transactions
WHERE user_id = @seed_uid
  AND family_id IS NULL
  AND description LIKE '[SEED]%';

-- Tháng 5/2026 (đúng tổng theo sprint: income 15_500_000, expense 1_575_000)
INSERT INTO transactions (
    user_id, account_id, category_id, family_id, amount, type, description, transaction_date,
    is_auto_categorized, is_anomaly
) VALUES
(@seed_uid, @seed_aid, 13, NULL, 15000000.00, 'INCOME', '[SEED] Lương tháng 5', '2026-05-01', FALSE, FALSE),
(@seed_uid, @seed_aid, 1, NULL, 250000.00, 'EXPENSE', '[SEED] KFC bữa tối', '2026-05-02', FALSE, FALSE),
(@seed_uid, @seed_aid, 2, NULL, 45000.00, 'EXPENSE', '[SEED] Grab đi làm', '2026-05-03', FALSE, FALSE),
(@seed_uid, @seed_aid, 1, NULL, 180000.00, 'EXPENSE', '[SEED] Phở buổi sáng', '2026-05-05', FALSE, FALSE),
(@seed_uid, @seed_aid, 4, NULL, 350000.00, 'EXPENSE', '[SEED] Mua áo', '2026-05-07', FALSE, FALSE),
(@seed_uid, @seed_aid, 3, NULL, 200000.00, 'EXPENSE', '[SEED] Netflix + Spotify', '2026-05-08', FALSE, FALSE),
(@seed_uid, @seed_aid, 2, NULL, 80000.00, 'EXPENSE', '[SEED] Xe ôm về nhà', '2026-05-10', FALSE, FALSE),
(@seed_uid, @seed_aid, 1, NULL, 320000.00, 'EXPENSE', '[SEED] Đi ăn nhóm', '2026-05-11', FALSE, FALSE),
(@seed_uid, @seed_aid, 14, NULL, 500000.00, 'INCOME', '[SEED] Thưởng dự án', '2026-05-12', FALSE, FALSE),
(@seed_uid, @seed_aid, 5, NULL, 150000.00, 'EXPENSE', '[SEED] Thuốc cảm', '2026-05-13', FALSE, FALSE);

-- Tháng 4/2026 (cho Trend API sau này)
INSERT INTO transactions (
    user_id, account_id, category_id, family_id, amount, type, description, transaction_date,
    is_auto_categorized, is_anomaly
) VALUES
(@seed_uid, @seed_aid, 13, NULL, 12000000.00, 'INCOME', '[SEED] Lương tháng 4', '2026-04-01', FALSE, FALSE),
(@seed_uid, @seed_aid, 1, NULL, 400000.00, 'EXPENSE', '[SEED] Ăn trưa công ty', '2026-04-06', FALSE, FALSE),
(@seed_uid, @seed_aid, 2, NULL, 120000.00, 'EXPENSE', '[SEED] Xăng xe', '2026-04-08', FALSE, FALSE),
(@seed_uid, @seed_aid, 3, NULL, 300000.00, 'EXPENSE', '[SEED] Xem phim', '2026-04-10', FALSE, FALSE),
(@seed_uid, @seed_aid, 4, NULL, 800000.00, 'EXPENSE', '[SEED] Giày mới', '2026-04-12', FALSE, FALSE),
(@seed_uid, @seed_aid, 1, NULL, 500000.00, 'EXPENSE', '[SEED] Tiệc sinh nhật', '2026-04-15', FALSE, FALSE);

-- Khớp số dư ví với tổng giao dịch (cùng logic TransactionService)
UPDATE accounts a
SET a.balance = 5000000.00 + (
    SELECT COALESCE(SUM(
        CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END
    ), 0)
    FROM transactions t
    WHERE t.user_id = @seed_uid
      AND t.account_id = @seed_aid
      AND t.deleted_at IS NULL
      AND t.family_id IS NULL
)
WHERE a.id = @seed_aid;
