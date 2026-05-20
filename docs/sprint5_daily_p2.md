# 🏃 Sprint 5 — Daily Breakdown (Day 4–6)
**Family Budget + FE Integration + Security Audit**

---

## 📅 DAY 4 — By-Member Breakdown + Tests + Postman
**6 tiếng | Thứ Năm**

### 🌅 Daily Standup
```
✅ Hôm qua: Shared Budget + Family Summary API xong
🎯 Hôm nay: By-member breakdown + Family Trend + unit tests + Postman
🚧 Blocker: Summary API tính đúng chưa? Test với 2 member nhập giao dịch
```

---

### ⏰ 09:00–11:00 | T12: Family Summary By-Member Breakdown (2h)

**Endpoint:** `GET /api/groups/{groupId}/summary/by-member?month=2026-05`

**Response — ai đóng góp nhiều nhất, ai chi nhiều nhất:**
```json
{
  "month": "2026-05",
  "members": [
    {
      "userId": 1,
      "fullName": "Nguyen Van A",
      "totalIncome": 4000000,
      "totalExpense": 1500000,
      "netContribution": 2500000,
      "transactionCount": 5
    },
    {
      "userId": 2,
      "fullName": "Nguyen Van B",
      "totalIncome": 1000000,
      "totalExpense": 1250000,
      "netContribution": -250000,
      "transactionCount": 8
    }
  ]
}
```

**Repository query — GROUP BY user_id:**
```java
@Query(value = """
    SELECT
        t.user_id,
        u.full_name,
        COALESCE(SUM(CASE WHEN t.type = 'INCOME'  THEN t.amount ELSE 0 END), 0) as total_income,
        COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) as total_expense,
        COUNT(t.id) as txn_count
    FROM transactions t
    JOIN users u ON u.id = t.user_id
    WHERE t.group_id = :groupId
      AND t.scope = 'SHARED'
      AND t.deleted_at IS NULL
      AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
    GROUP BY t.user_id, u.full_name
    ORDER BY total_expense DESC
""", nativeQuery = true)
List<Object[]> getMemberBreakdown(
    @Param("groupId") Long groupId,
    @Param("month") String month
);
```

**Map Object[] sang DTO trong service:**
```java
List<MemberSummaryDTO> members = rawData.stream().map(row -> {
    Long userId = ((Number) row[0]).longValue();
    String fullName = (String) row[1];
    BigDecimal income = (BigDecimal) row[2];
    BigDecimal expense = (BigDecimal) row[3];
    Long count = ((Number) row[4]).longValue();
    BigDecimal net = income.subtract(expense);
    return new MemberSummaryDTO(userId, fullName, income, expense, net, count);
}).collect(Collectors.toList());
```

> 💡 **AI Assist:** Native query GROUP BY phức tạp → nhờ AI generate, tự review và test kết quả.

**✅ Done khi:**
- User A nhập 3 giao dịch, User B nhập 5 giao dịch
- By-member breakdown hiện đúng số lượng + tổng tiền từng người
- Member không có giao dịch tháng đó → **không xuất hiện** trong list (khác với Trend phải trả 0)

---

### ⏰ 11:00–12:00 | T13: Family Trend API (1h)

**Endpoint:** `GET /api/groups/{groupId}/summary/trend?months=6`

Logic giống Sprint 3 Trend API nhưng filter theo shared transactions của group:

```java
public FamilyTrendResponse getGroupTrend(Long groupId, int months, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId);

    List<String> monthList = IntStream.range(0, months)
        .mapToObj(i -> YearMonth.now().minusMonths(months - 1 - i).toString())
        .collect(Collectors.toList());

    List<TrendItemDTO> trend = monthList.stream().map(month -> {
        // Reuse query tương tự nhưng thêm filter group_id + scope=SHARED
        BigDecimal income  = transactionRepository.sumByGroupAndTypeAndMonth(
            groupId, "INCOME", month, "SHARED");
        BigDecimal expense = transactionRepository.sumByGroupAndTypeAndMonth(
            groupId, "EXPENSE", month, "SHARED");
        income  = income  != null ? income  : BigDecimal.ZERO;
        expense = expense != null ? expense : BigDecimal.ZERO;
        return new TrendItemDTO(month, income, expense, income.subtract(expense));
    }).collect(Collectors.toList());

    return new FamilyTrendResponse(groupId, trend);
}
```

**✅ Done khi:** 6 tháng trả về đủ, tháng trống = 0

---

### ⏰ 13:00–14:30 | T14: Unit Tests (1.5h)

**SharedTransactionService tests:**
- `createSharedTxn_memberSuccess()` — MEMBER nhập được
- `createSharedTxn_nonMemberForbidden()` → `ForbiddenException`
- `createSharedTxn_personalAccountRejected()` → `InvalidTransactionException`
- `createSharedTxn_wrongGroupAccount()` → `InvalidTransactionException`
- `getSharedTxns_personalNotLeaked()` — PERSONAL transactions không xuất hiện
- `deleteSharedTxn_byCreator()` — creator xóa được
- `deleteSharedTxn_byAdmin()` — ADMIN xóa được
- `deleteSharedTxn_byOtherMember()` → `ForbiddenException`

**SharedBudgetService tests:**
- `createGroupBudget_adminSuccess()`
- `createGroupBudget_memberForbidden()` → `ForbiddenException`
- `createGroupBudget_duplicate()` → `DuplicateBudgetException`
- `getBudgetStatus_calculatesFromSharedTxns()` — tính từ SHARED, không tính PERSONAL

**FamilySummaryService tests:**
- `getGroupSummary_onlySharedTransactions()`
- `getByMemberBreakdown_correctGrouping()`
- `getGroupTrend_emptyMonthReturnsZero()`

---

### ⏰ 14:30–15:30 | T15: Postman Collection Sprint 5 (1h)

```
📁 Family Finance (Sprint 5)
  📁 Shared Accounts
    ├── POST /api/groups/{id}/accounts
    ├── GET  /api/groups/{id}/accounts
    └── (GET current balance — trong response của GET)

  📁 Shared Transactions
    ├── POST /api/groups/{id}/transactions
    ├── GET  /api/groups/{id}/transactions?month=
    └── DELETE /api/groups/{id}/transactions/{txnId}

  📁 Shared Budget
    ├── POST   /api/groups/{id}/budgets
    ├── GET    /api/groups/{id}/budgets?month=
    ├── GET    /api/groups/{id}/budgets/status?month=
    ├── PUT    /api/groups/{id}/budgets/{id}
    └── DELETE /api/groups/{id}/budgets/{id}

  📁 Family Summary
    ├── GET /api/groups/{id}/summary?month=
    ├── GET /api/groups/{id}/summary/by-member?month=
    └── GET /api/groups/{id}/summary/trend?months=6
```

**Privacy test trong Postman:**
```javascript
// GET shared transactions — verify PERSONAL không lộ
pm.test("No PERSONAL transactions in group list", () => {
    const items = pm.response.json().data.content;
    items.forEach(t => {
        pm.expect(t.scope).to.equal('SHARED');
    });
});
```

**Commit Day 4:**
```bash
git commit -m "feat: by-member breakdown, family trend, unit tests Sprint 5, Postman collection"
```

---

## 📅 DAY 5 — FE Integration: Family Features
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Tất cả Family APIs xong và tested
🎯 Hôm nay: Kết nối FE với Family APIs
🚧 Blocker: FE có sẵn trang Family/Group chưa? List các components cần kết nối
```

---

### ⏰ 09:00–10:30 | T16: FE Shared Account Page (1.5h)

**Shared account list trong group detail:**
```javascript
const loadSharedAccounts = async () => {
    const res = await api.get(`/groups/${groupId}/accounts`);
    setSharedAccounts(res.data.data);
};

// Hiển thị số dư tài khoản chung
{sharedAccounts.map(acc => (
    <div key={acc.id} className="account-card">
        <h4>{acc.name}</h4>
        <p className="balance">{formatVND(acc.currentBalance)}</p>
        <span className="badge shared">Tài khoản chung</span>
    </div>
))}
```

**Form tạo tài khoản chung (chỉ ADMIN thấy):**
```javascript
// Chỉ render form nếu myRole === 'ADMIN'
{group?.myRole === 'ADMIN' && (
    <button onClick={() => setShowCreateAccount(true)}>
        + Tạo tài khoản chung
    </button>
)}

const handleCreateSharedAccount = async (data) => {
    await api.post(`/groups/${groupId}/accounts`, data);
    loadSharedAccounts();
};
```

---

### ⏰ 10:30–12:30 | T17: FE Shared Transaction List + Form (2h)

**Shared Transaction List:**
```javascript
const loadSharedTransactions = async () => {
    const params = new URLSearchParams({ month: selectedMonth });
    if (filterType) params.append('type', filterType);

    const res = await api.get(`/groups/${groupId}/transactions?${params}`);
    setSharedTransactions(res.data.data.content);
};

// Hiển thị "ai nhập" giao dịch
{sharedTransactions.map(txn => (
    <div key={txn.id} className="txn-row">
        <span>{txn.description}</span>
        <span className="creator">bởi {txn.createdByName}</span>
        <span className={`amount ${txn.type}`}>
            {txn.type === 'EXPENSE' ? '-' : '+'}{formatVND(txn.amount)}
        </span>
        {/* Hiện nút xóa nếu là creator hoặc ADMIN */}
        {(txn.createdByUserId === currentUserId || group?.myRole === 'ADMIN') && (
            <button onClick={() => handleDelete(txn.id)} className="danger-sm">
                Xóa
            </button>
        )}
    </div>
))}
```

**Form thêm giao dịch chung:**
```javascript
// Dropdown tài khoản chỉ hiện SHARED accounts
const sharedAccountOptions = sharedAccounts.map(a => ({
    value: a.id, label: `${a.name} (${formatVND(a.currentBalance)})`
}));

const handleCreateSharedTxn = async (data) => {
    try {
        await api.post(`/groups/${groupId}/transactions`, {
            ...data,
            // accountId phải là shared account
        });
        loadSharedTransactions();
        onClose();
    } catch (err) {
        setError(err.response?.data?.message);
    }
};
```

---

### ⏰ 13:00–15:00 | T18: FE Family Dashboard (2h)

**Family Dashboard page:**
```javascript
const loadFamilyDashboard = async () => {
    const [summaryRes, byMemberRes, trendRes] = await Promise.all([
        api.get(`/groups/${groupId}/summary?month=${selectedMonth}`),
        api.get(`/groups/${groupId}/summary/by-member?month=${selectedMonth}`),
        api.get(`/groups/${groupId}/summary/trend?months=6`)
    ]);
    setSummary(summaryRes.data.data);
    setByMember(byMemberRes.data.data.members);
    setTrend(trendRes.data.data.trend);
};
```

**By-member chart — Bar chart ngang:**
```javascript
const memberChartData = {
    labels: byMember.map(m => m.fullName),
    datasets: [
        {
            label: 'Thu',
            data: byMember.map(m => m.totalIncome),
            backgroundColor: '#36A2EB'
        },
        {
            label: 'Chi',
            data: byMember.map(m => m.totalExpense),
            backgroundColor: '#FF6384'
        }
    ]
};
// Dùng Bar chart (indexAxis: 'y') để hiện ngang
```

**Cards tổng quan gia đình:**
```jsx
<div className="family-stats">
    <StatCard label="Quỹ chung"    value={summary?.groupAccountBalance} color="blue" />
    <StatCard label="Thu tháng này" value={summary?.totalIncome}         color="green"/>
    <StatCard label="Chi tháng này" value={summary?.totalExpense}         color="red"  />
    <StatCard label="Tiết kiệm"     value={summary?.netBalance}           color="purple"/>
</div>
```

---

### ⏰ 15:00–16:00 | T19: FE Shared Budget Page (1h)

**Shared budget page trong group context:**
```javascript
const loadGroupBudgets = async () => {
    const res = await api.get(
        `/groups/${groupId}/budgets/status?month=${selectedMonth}`
    );
    setBudgetStatus(res.data.data);
};

// Form tạo budget chỉ ADMIN thấy
{group?.myRole === 'ADMIN' && (
    <button onClick={() => setShowBudgetForm(true)}>
        + Đặt ngân sách chung
    </button>
)}
```

Layout giống Budget page cá nhân (Sprint 3) — tái sử dụng component `BudgetStatusItem`.

**Commit Day 5:**
```bash
git commit -m "feat: FE integrated - shared accounts, transactions, family dashboard, shared budget"
```

---

## 📅 DAY 6 — Security Audit + Bug Fix + Sprint Review
**6 tiếng | Thứ Bảy**

### 🌅 Daily Standup
```
✅ Hôm qua: Toàn bộ Family FE kết nối BE thật
🎯 Hôm nay: Security audit kỹ, fix bug, Sprint Review
🚧 Blocker: List tất cả bug phát hiện từ Day 4-5
```

---

### ⏰ 09:00–11:30 | T20: Security & Isolation Audit (2.5h)

**CRITICAL — Privacy Isolation Test (phải pass 100%):**

```
Test A — PERSONAL không lộ ra group:
Setup: User A nhập giao dịch PERSONAL 1tr "Tiền riêng"
Check: GET /api/groups/{id}/transactions → 1tr KHÔNG xuất hiện ✅

Test B — Cross-group isolation:
Setup: User A thuộc Group 1 và Group 2
       User A nhập shared txn vào Group 1
Check: GET /api/groups/2/transactions → txn của Group 1 KHÔNG xuất hiện ✅

Test C — Non-member access:
Setup: User C không thuộc Group 1
Check: GET /api/groups/1/transactions → 403 ✅
Check: POST /api/groups/1/transactions → 403 ✅
Check: GET /api/groups/1/summary → 403 ✅

Test D — Budget scope:
Setup: User A có personal budget + group budget cùng category
Check: GET /api/budgets?month= (personal) → chỉ thấy personal budget ✅
Check: GET /api/groups/1/budgets?month= → chỉ thấy group budget ✅

Test E — Account scope:
Check: GET /api/accounts → chỉ thấy PERSONAL accounts ✅
Check: GET /api/groups/1/accounts → chỉ thấy SHARED accounts của group ✅
```

**Authorization matrix — Kiểm tra từng endpoint:**

| Endpoint | Non-member | MEMBER | ADMIN |
|----------|-----------|--------|-------|
| POST /groups/{id}/accounts | 403 | 403 | ✅ |
| GET /groups/{id}/accounts | 403 | ✅ | ✅ |
| POST /groups/{id}/transactions | 403 | ✅ | ✅ |
| DELETE /groups/{id}/transactions/{id} | 403 | Chỉ creator | ✅ |
| POST /groups/{id}/budgets | 403 | 403 | ✅ |
| GET /groups/{id}/budgets/status | 403 | ✅ | ✅ |
| GET /groups/{id}/summary | 403 | ✅ | ✅ |
| GET /groups/{id}/summary/by-member | 403 | ✅ | ✅ |

---

### ⏰ 11:30–13:30 | T21: Bug Fix (2h)

**Common bugs Sprint 5:**

| Bug | Fix |
|-----|-----|
| Shared account hiện trong personal account list | Query personal: `WHERE group_id IS NULL AND scope = 'PERSONAL'` |
| Personal transaction hiện trong group list | Query group: `WHERE scope = 'SHARED'` — không bao giờ thiếu điều kiện này |
| Budget Status tính cả personal transactions | Thêm `AND t.group_id = :groupId AND t.scope = 'SHARED'` |
| By-member có member không nhập giao dịch → không hiện | Đây là đúng — chỉ hiện member có giao dịch |
| FE form chọn account chung nhưng list lẫn personal | Filter accounts theo `scope === 'SHARED'` trong FE |
| currentBalance tài khoản chung không cập nhật realtime | Tính lại balance mỗi lần GET accounts |

---

### ⏰ 14:00–15:00 | Sprint Review (1h)

**Deliverables Checklist:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | POST /api/groups/{id}/accounts — ADMIN tạo tài khoản chung | ⬜ |
| 2 | GET /api/groups/{id}/accounts — mọi member xem + current balance | ⬜ |
| 3 | POST shared transaction — mọi member nhập | ⬜ |
| 4 | GET shared transactions — kèm "created by" | ⬜ |
| 5 | DELETE shared transaction — creator hoặc ADMIN | ⬜ |
| 6 | Shared Budget CRUD + Status API | ⬜ |
| 7 | Family Summary (tổng thu/chi + group balance) | ⬜ |
| 8 | By-member breakdown (ai chi nhiều nhất) | ⬜ |
| 9 | Family Trend 6 tháng | ⬜ |
| 10 | **CRITICAL**: PERSONAL không lộ trong group context | ⬜ |
| 11 | FE: Shared account, transaction, budget, dashboard | ⬜ |
| 12 | Unit tests GREEN | ⬜ |

**Demo Script (10 phút):**
```
Personal (User A):
1. Dashboard cá nhân bình thường → không thấy data group   (1p)

Family Group:
2. Vào group "Gia đình Nguyễn"                               (30s)
3. Xem tài khoản chung "Quỹ gia đình" 10tr                  (30s)
4. User A nhập shared: Thu Lương 5tr                         (1p)
5. User B nhập shared: Chi Ăn uống 200k                      (1p)
6. Family dashboard: tổng thu 5tr, chi 200k                  (1p)
7. By-member: User A 5tr thu, User B 200k chi               (1p)
8. Budget: Ăn uống 500k → 200k/500k = 40% OK               (1p)
9. Privacy demo: User A nhập PERSONAL 1tr → không hiện group (1p)
10. Cross-group: User C thử vào → 403                        (30s)
```

---

### ⏰ 15:00–15:30 | Sprint Retrospective (30 phút)

```
✅ Went WELL:
   Ví dụ: Tái sử dụng GroupAuthorizationService
   giảm được rất nhiều code duplicate

⚠️ Could IMPROVE:
   Ví dụ: Native query Object[] khó debug,
   nên dùng Projections interface thay thế

🚀 Next Sprint:
   Ví dụ: Sprint 6 AI features — timebox prompt
   engineering tối đa 4h, không được kéo dài
```

**Final Commit + Tag:**
```bash
git commit -m "chore: security audit, privacy isolation verified, bugs fixed, Sprint 5 complete"
git tag -a sprint-5 -m "Sprint 5: Family Finance MVP"
git push origin main --tags
```

---

## 📊 Sprint 5 Summary

### Thời Gian Phân Bổ
```
Day 1: Shared Account APIs + tests           → 6h
Day 2: Shared Transaction APIs + privacy     → 6h  ← Privacy test quan trọng nhất
Day 3: Shared Budget + Family Summary        → 6h
Day 4: By-member + Trend + tests + Postman   → 6h  ← Query GROUP BY phức tạp nhất
Day 5: FE Integration toàn bộ family        → 6h
Day 6: Security audit + bug fix + Review     → 6h
─────────────────────────────────────────────────
Total:                                        36h
```

### Nếu Bị Trễ — Cắt Theo Thứ Tự
```
Giữ:   Shared Transaction API + Privacy isolation + Family Summary
Giảm:  By-member breakdown (bỏ chart, chỉ giữ API + hiện dạng text)
Dời:   Family Trend (bỏ hẳn, chỉ cần summary tháng hiện tại)
Bỏ:    Shared Budget (dùng personal budget để estimate)
```

### Sang Sprint 6 — AI Features
**Data đã đủ** để AI làm việc:
- `transactions` có `group_id` + `scope` → phân biệt personal vs shared
- `family_groups` + `group_members` → biết ai trong nhóm nào
- `budgets` có `group_id` → budget chung đã có

---

*Sprint 5 Complete → Family Finance MVP ✅ → Sprint 6: AI Auto-Categorization* 🚀
