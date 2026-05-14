# 🏃 Sprint 3 (Revised) — Backend APIs + FE Integration
**FE đã xong → Tập trung BE + kết nối | 6 ngày × 6h = 36h**

> **Thay đổi:** FE đã thiết kế xong → Bỏ toàn bộ task build UI.
> Sprint 3 giờ gồm: BE APIs (Day 1–3) + Kết nối FE với BE thật (Day 4–5) + Review (Day 6).

---

## Backlog Sprint 3

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Summary API: tổng thu/chi/số dư + breakdown category | 2.5h | Day 1 |
| T02 | Current balance tính động từ transactions | 1h | Day 1 |
| T03 | Unit test SummaryService | 1.5h | Day 1 |
| T04 | Seed data thực tế để test | 1h | Day 1 |
| T05 | Trend API: thu/chi 6 tháng | 2h | Day 2 |
| T06 | Migration V5: bảng budgets | 1h | Day 2 |
| T07 | Budget entity + repo + DTO | 1h | Day 2 |
| T08 | Budget CRUD API | 2h | Day 2 |
| T09 | Budget Status API (OK/WARNING/EXCEEDED) | 2.5h | Day 3 |
| T10 | Unit test BudgetService | 2h | Day 3 |
| T11 | Postman collection Sprint 3 | 1h | Day 3 |
| T12 | CORS config + axiosConfig kết nối FE-BE | 1h | Day 4 |
| T13 | Kết nối Dashboard: Summary + Trend chart | 2h | Day 4 |
| T14 | Kết nối Transaction list + filter | 2h | Day 4 |
| T15 | Kết nối Form thêm giao dịch | 1h | Day 4 |
| T16 | Kết nối Budget page + Budget Status | 2h | Day 5 |
| T17 | Kết nối Account management | 1h | Day 5 |
| T18 | Xử lý error states + loading states toàn FE | 2h | Day 5 |
| T19 | Integration test toàn bộ flow | 2.5h | Day 6 |
| T20 | Bug fix | 2h | Day 6 |
| T21 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Summary API
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 2 xong, FE đã thiết kế xong
🎯 Hôm nay: Summary API + current balance + seed data
🚧 Blocker: DB có transaction test data chưa? Nếu chưa → seed trước
```

### ⏰ 09:00–09:30 | T04: Seed Data (30 phút — làm TRƯỚC)

Trước khi code API, tạo data để test luôn. Tạo file `data.sql` hoặc chạy thẳng vào DB:

```sql
-- Tạo 1 user test (nếu chưa có)
-- Tạo 1 account: "Ví tiền mặt", initial_balance = 5,000,000

-- 10 transactions tháng hiện tại
INSERT INTO transactions (user_id, account_id, category_id, amount, type, description, transaction_date, scope)
VALUES
(1, 1, 1, 15000000, 'INCOME', 'Lương tháng 5',  '2026-05-01', 'PERSONAL'),
(1, 1, 4, 250000,   'EXPENSE','KFC bữa tối',     '2026-05-02', 'PERSONAL'),
(1, 1, 5, 45000,    'EXPENSE','Grab đi làm',      '2026-05-03', 'PERSONAL'),
(1, 1, 4, 180000,   'EXPENSE','Phở buổi sáng',   '2026-05-05', 'PERSONAL'),
(1, 1, 6, 350000,   'EXPENSE','Mua áo',           '2026-05-07', 'PERSONAL'),
(1, 1, 7, 200000,   'EXPENSE','Netflix + Spotify','2026-05-08', 'PERSONAL'),
(1, 1, 5, 80000,    'EXPENSE','Xe ôm về nhà',     '2026-05-10', 'PERSONAL'),
(1, 1, 4, 320000,   'EXPENSE','Đi ăn nhóm',       '2026-05-11', 'PERSONAL'),
(1, 1, 2, 500000,   'INCOME', 'Thưởng dự án',     '2026-05-12', 'PERSONAL'),
(1, 1, 8, 150000,   'EXPENSE','Thuốc cảm',        '2026-05-13', 'PERSONAL');
```

Tương tự thêm 5–6 transactions cho tháng trước (2026-04) để Trend API có data.

---

### ⏰ 09:30–12:00 | T01: Summary API (2.5h)

**Endpoint:** `GET /api/summary?month=2026-05`

**Response:**
```json
{
  "month": "2026-05",
  "totalIncome": 15500000,
  "totalExpense": 1575000,
  "netBalance": 13925000,
  "currentBalance": 18925000,
  "categoryBreakdown": [
    {
      "categoryId": 4,
      "categoryName": "Ăn uống",
      "type": "EXPENSE",
      "total": 750000,
      "percentage": 47.6,
      "transactionCount": 3
    }
  ]
}
```

**SummaryRepository — 2 native queries cần viết:**

Query 1 — Tổng thu/chi theo tháng:
```java
@Query(value = """
    SELECT type, COALESCE(SUM(amount), 0) as total
    FROM transactions
    WHERE user_id = :userId
      AND deleted_at IS NULL
      AND scope = 'PERSONAL'
      AND DATE_FORMAT(transaction_date, '%Y-%m') = :month
    GROUP BY type
""", nativeQuery = true)
List<Object[]> sumByTypeAndMonth(
    @Param("userId") Long userId,
    @Param("month") String month
);
```

Query 2 — Breakdown theo category:
```java
@Query(value = """
    SELECT c.id, c.name, c.type,
           COALESCE(SUM(t.amount), 0) as total,
           COUNT(t.id) as txn_count
    FROM categories c
    INNER JOIN transactions t ON t.category_id = c.id
        AND t.user_id = :userId
        AND t.deleted_at IS NULL
        AND t.scope = 'PERSONAL'
        AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
    GROUP BY c.id, c.name, c.type
    ORDER BY total DESC
""", nativeQuery = true)
List<Object[]> categoryBreakdown(
    @Param("userId") Long userId,
    @Param("month") String month
);
```

**SummaryService logic:**
1. Gọi query 1 → tách ra totalIncome, totalExpense
2. Gọi query 2 → map thành list CategoryBreakdownDTO
3. Tính `percentage` cho từng item EXPENSE: `(item.total / totalExpense) * 100`
4. Tính `currentBalance` (T02 bên dưới)
5. Nếu không truyền `month` → default `YearMonth.now().toString()`

**✅ Done khi:**
- API trả đúng tổng với data đã seed
- Tháng không có data → trả 0, không throw exception
- percentage các EXPENSE category cộng lại ≈ 100%

---

### ⏰ 12:00–13:00 | T02: Current Balance (1h)

`currentBalance` = số dư thực tế **toàn bộ thời gian** (không filter tháng):

```java
@Query(value = """
    SELECT
      COALESCE((SELECT SUM(initial_balance) FROM accounts
                WHERE user_id = :userId AND deleted_at IS NULL), 0)
      + COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0)
    FROM transactions
    WHERE user_id = :userId AND deleted_at IS NULL AND scope = 'PERSONAL'
""", nativeQuery = true)
BigDecimal calculateCurrentBalance(@Param("userId") Long userId);
```

Thêm vào `SummaryResponse.currentBalance`.

---

### ⏰ 14:00–15:30 | T03: Unit Test SummaryService (1.5h)

- `getSummary_correctTotals()` — tổng income/expense đúng
- `getSummary_emptyMonth()` — tháng trống → 0, không crash
- `getSummary_defaultCurrentMonth()` — không truyền month → tháng hiện tại
- `getSummary_percentageCalculation()` — % tính đúng
- `getSummary_isolatedByUser()` — không lộ data user khác

**Commit Day 1:**
```bash
git commit -m "feat: Summary API with category breakdown, current balance, unit tests"
```

---

## 📅 DAY 2 — Trend API + Budget CRUD
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: Summary API + tests xong
🎯 Hôm nay: Trend 6 tháng + Budget CRUD
🚧 Blocker: Đã có seed data cho tháng trước chưa? Trend cần ít nhất 2 tháng data
```

### ⏰ 09:00–11:00 | T05: Trend API (2h)

**Endpoint:** `GET /api/summary/trend?months=6`

**Response:**
```json
{
  "trend": [
    { "month": "2025-12", "income": 15000000, "expense": 9000000, "net": 6000000 },
    { "month": "2026-01", "income": 15000000, "expense": 7500000, "net": 7500000 },
    ...
  ]
}
```

**Logic:**
```java
// Tạo list 6 tháng gần nhất
List<String> months = IntStream.range(0, n)
    .mapToObj(i -> YearMonth.now().minusMonths(n - 1 - i).toString())
    .collect(Collectors.toList());

// Với mỗi tháng → gọi query tổng thu/chi (reuse query của Summary API)
// Map kết quả vào TrendItemDTO
```

**⚠️ Quan trọng:** Tháng không có data → vẫn phải có trong response với income=0, expense=0. Không được bỏ qua tháng đó (FE cần đủ 6 điểm để vẽ chart).

**✅ Done khi:** Trả đúng 6 tháng, tháng trống có giá trị 0

---

### ⏰ 11:00–12:00 | T06 + T07: Migration Budget + Entity (1h + 1h)

**`V5__create_budgets_table.sql`:**
```sql
CREATE TABLE budgets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    group_id    BIGINT NULL,
    category_id BIGINT NOT NULL,
    amount      DECIMAL(15,2) NOT NULL,
    month       DATE NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id),
    UNIQUE KEY uk_budget (user_id, category_id, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Lưu ý:** `month` lưu dạng DATE, luôn là ngày 1 của tháng (`2026-05-01`). Convert khi nhận input:
```java
LocalDate monthDate = YearMonth.parse(request.getMonth()).atDay(1);
```

**DTOs:**
- `CreateBudgetRequest`: categoryId (NotNull), amount (>0, NotNull), month (String "YYYY-MM")
- `BudgetResponse`: id, categoryId, categoryName, amount, month (String "YYYY-MM")

---

### ⏰ 13:00–15:00 | T08: Budget CRUD API (2h)

| Method | URL | Rule |
|--------|-----|------|
| GET | `/api/budgets?month=` | Trả budgets của user trong tháng |
| POST | `/api/budgets` | Tạo mới, check trùng category+tháng |
| PUT | `/api/budgets/{id}` | Chỉ sửa được amount |
| DELETE | `/api/budgets/{id}` | Chỉ owner mới xóa được |

**Business rule quan trọng — Check trùng:**
```java
boolean exists = budgetRepository
    .existsByUserIdAndCategoryIdAndMonth(userId, categoryId, monthDate);
if (exists) throw new DuplicateBudgetException(
    "Đã có ngân sách cho danh mục này trong tháng " + request.getMonth()
);
```

**Commit Day 2:**
```bash
git commit -m "feat: Trend API (6 months), Budget migration, Budget CRUD API"
```

---

## 📅 DAY 3 — Budget Status + Tests + Postman
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Trend + Budget CRUD xong
🎯 Hôm nay: Budget Status API (quan trọng nhất Sprint 3) + tests + Postman
🚧 Blocker: Seed vài budget trước khi test Budget Status
```

### ⏰ 09:00–11:30 | T09: Budget Status API (2.5h)

**Endpoint:** `GET /api/budgets/status?month=2026-05`

**Response:**
```json
{
  "month": "2026-05",
  "totalBudget": 4500000,
  "totalActual": 3775000,
  "totalRemaining": 725000,
  "items": [
    {
      "categoryId": 4,
      "categoryName": "Ăn uống",
      "budgetAmount": 3000000,
      "actualAmount": 2500000,
      "remainingAmount": 500000,
      "usedPercentage": 83.3,
      "status": "WARNING"
    },
    {
      "categoryId": 5,
      "categoryName": "Đi lại",
      "budgetAmount": 1000000,
      "actualAmount": 1200000,
      "remainingAmount": -200000,
      "usedPercentage": 120.0,
      "status": "EXCEEDED"
    }
  ]
}
```

**Status logic:**
```java
public BudgetStatus calculateStatus(double usedPercentage) {
    if (usedPercentage >= 100) return BudgetStatus.EXCEEDED;
    if (usedPercentage >= 80)  return BudgetStatus.WARNING;
    return BudgetStatus.OK;
}
```

**Query — JOIN budgets với transactions:**
```java
@Query(value = """
    SELECT b.id, b.category_id, c.name as category_name,
           b.amount as budget_amount,
           COALESCE(SUM(t.amount), 0) as actual_amount
    FROM budgets b
    JOIN categories c ON c.id = b.category_id
    LEFT JOIN transactions t ON t.category_id = b.category_id
        AND t.user_id = :userId
        AND t.type = 'EXPENSE'
        AND t.deleted_at IS NULL
        AND t.scope = 'PERSONAL'
        AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
    WHERE b.user_id = :userId
      AND DATE_FORMAT(b.month, '%Y-%m') = :month
    GROUP BY b.id, b.category_id, c.name, b.amount
""", nativeQuery = true)
List<Object[]> getBudgetStatus(
    @Param("userId") Long userId,
    @Param("month") String month
);
```

**Sau khi có data → tính trong Java:**
```java
items.forEach(item -> {
    BigDecimal remaining = item.getBudgetAmount().subtract(item.getActualAmount());
    double pct = item.getBudgetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0
        : item.getActualAmount().divide(item.getBudgetAmount(), 4, HALF_UP)
              .multiply(BigDecimal.valueOf(100)).doubleValue();
    item.setRemainingAmount(remaining);
    item.setUsedPercentage(pct);
    item.setStatus(calculateStatus(pct));
});
```

**✅ Done khi:**
- Category chi 83% → WARNING
- Category chi 120% → EXCEEDED, remaining âm
- Category chưa chi → OK, actualAmount=0

---

### ⏰ 11:30–13:30 | T10: Unit Tests BudgetService (2h)

- `createBudget_success()`
- `createBudget_duplicate()` → `DuplicateBudgetException`
- `getBudgetStatus_ok()` → 50% → OK
- `getBudgetStatus_warning()` → 85% → WARNING
- `getBudgetStatus_exceeded()` → 110% → EXCEEDED, remaining âm
- `getBudgetStatus_noSpending()` → actualAmount=0
- `getBudgetStatus_emptyMonth()` → list rỗng, không crash

---

### ⏰ 14:30–15:30 | T11: Postman Collection Sprint 3 (1h)

Thêm folder "Summary & Budget":

```
GET /api/summary?month=2026-05
GET /api/summary/trend?months=6
GET /api/budgets?month=2026-05
POST /api/budgets  → body: {categoryId, amount, month}
PUT /api/budgets/{id}
DELETE /api/budgets/{id}
GET /api/budgets/status?month=2026-05
```

Auto-test cho Budget Status:
```javascript
pm.test("Items have status field", () => {
    const items = pm.response.json().data.items;
    items.forEach(i => {
        pm.expect(i.status).to.be.oneOf(['OK','WARNING','EXCEEDED']);
    });
});
```

**Commit Day 3:**
```bash
git commit -m "feat: Budget Status API, unit tests, Postman Sprint 3 complete"
```

---

*→ Phần kết nối FE: Day 4–6 trong file sprint3_v2_p2.md*
