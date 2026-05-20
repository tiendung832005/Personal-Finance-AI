# 🏃 Sprint 5 — Daily Breakdown (Day 1–3)
**Shared Accounts, Transactions & Family Budget | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** Member gia đình cùng nhập giao dịch vào quỹ chung, xem được ai chi gì, ADMIN đặt budget chung. **Family MVP hoàn chỉnh sau Sprint này.**

> **Tái sử dụng từ Sprint 4:** `GroupAuthorizationService` dùng ở **mọi** API Sprint 5. Không cần viết lại logic check member/admin.

> **Nguyên tắc Privacy (phải nhớ mọi lúc):**
> - `scope = PERSONAL` → **KHÔNG BAO GIỜ** hiện trong group context
> - `scope = SHARED` + `group_id IS NOT NULL` → mọi member trong group thấy được
> - Giao dịch cá nhân của member A → member B không được biết

---

## Backlog Sprint 5

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Shared Account: POST tạo tài khoản chung | 1.5h | Day 1 |
| T02 | Shared Account: GET danh sách tài khoản chung | 1h | Day 1 |
| T03 | Shared Account: GET current balance | 1h | Day 1 |
| T04 | Unit test SharedAccountService | 1.5h | Day 1 |
| T05 | Shared Transaction: POST tạo giao dịch chung | 2h | Day 2 |
| T06 | Shared Transaction: GET list với filter | 2h | Day 2 |
| T07 | Shared Transaction: DELETE (soft delete) | 1h | Day 2 |
| T08 | Privacy check: PERSONAL không lộ ra group | 1h | Day 2 |
| T09 | Shared Budget: Migration + CRUD | 2h | Day 3 |
| T10 | Shared Budget Status API | 2h | Day 3 |
| T11 | Family Summary API (tổng thu/chi cả nhà) | 1.5h | Day 3 |
| T12 | Family Summary by-member breakdown | 2h | Day 4 |
| T13 | Family Trend API (6 tháng) | 1h | Day 4 |
| T14 | Unit test Family APIs | 1.5h | Day 4 |
| T15 | Postman collection Sprint 5 | 1h | Day 4 |
| T16 | FE: Shared Account page integration | 1.5h | Day 5 |
| T17 | FE: Shared Transaction list + form | 2h | Day 5 |
| T18 | FE: Family Dashboard (summary + chart) | 2h | Day 5 |
| T19 | FE: Shared Budget page | 1h | Day 5 |
| T20 | Security audit + isolation test | 2.5h | Day 6 |
| T21 | Bug fix | 2h | Day 6 |
| T22 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Shared Account APIs
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 4 xong — Group management hoàn chỉnh
🎯 Hôm nay: Shared Account APIs (tạo tài khoản chung, xem số dư)
🚧 Blocker: Đã có group với ít nhất 2 member để test chưa?
            Nếu chưa → seed data trước
```

---

### ⏰ 09:00–09:30 | Seed Data cho Sprint 5

Cần có sẵn để test:
```sql
-- Group "Gia đình Nguyễn" đã tạo (từ Sprint 4)
-- User A là ADMIN, User B là MEMBER

-- Tạo vài categories nếu cần thêm cho shared context
-- (Dùng lại categories system default từ Sprint 2 là đủ)
```

---

### ⏰ 09:30–11:00 | T01: POST Tạo Tài Khoản Chung (1.5h)

**Endpoint:** `POST /api/groups/{groupId}/accounts`

**Request body:**
```json
{
  "name": "Quỹ gia đình",
  "type": "CASH",
  "initialBalance": 10000000,
  "currency": "VND"
}
```

**Logic:**
```java
@Transactional
public AccountResponse createSharedAccount(Long groupId,
                                            CreateAccountRequest request,
                                            Long currentUserId) {
    // 1. Phải là ADMIN mới tạo được tài khoản chung
    groupAuthService.requireAdmin(groupId, currentUserId);

    // 2. Tạo account với group_id set, scope = SHARED
    Account account = Account.builder()
        .userId(currentUserId)      // người tạo
        .groupId(groupId)           // thuộc group này
        .name(request.getName())
        .type(request.getType())
        .balance(request.getInitialBalance()) // initial balance
        .currency(request.getCurrency())
        .scope(AccountScope.SHARED)
        .build();

    account = accountRepository.save(account);
    return mapToResponse(account);
}
```

**✅ Done khi:**
- ADMIN tạo → account có `group_id` set, `scope = SHARED`
- MEMBER tạo → 403
- User không phải member tạo → 403

---

### ⏰ 11:00–12:00 | T02: GET Danh Sách Tài Khoản Chung (1h)

**Endpoint:** `GET /api/groups/{groupId}/accounts`

```java
public List<AccountResponse> getSharedAccounts(Long groupId, Long currentUserId) {
    // Mọi member đều xem được
    groupAuthService.requireMember(groupId, currentUserId);

    // Chỉ lấy accounts thuộc group này (group_id = groupId, scope = SHARED)
    List<Account> accounts = accountRepository
        .findByGroupIdAndScope(groupId, AccountScope.SHARED);

    return accounts.stream()
        .map(a -> {
            BigDecimal currentBalance = calculateGroupAccountBalance(a);
            return mapToResponseWithBalance(a, currentBalance);
        })
        .collect(Collectors.toList());
}
```

**Repository query:**
```java
List<Account> findByGroupIdAndScope(Long groupId, AccountScope scope);
```

---

### ⏰ 12:00–13:00 | T03: Current Balance Tài Khoản Chung (1h)

Balance tài khoản chung = initial_balance + tất cả INCOME - tất cả EXPENSE trong group account đó:

```java
@Query(value = """
    SELECT
        a.balance
        + COALESCE(SUM(CASE WHEN t.type = 'INCOME'  THEN t.amount ELSE 0 END), 0)
        - COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
    FROM accounts a
    LEFT JOIN transactions t ON t.account_id = a.id
        AND t.deleted_at IS NULL
    WHERE a.id = :accountId
      AND a.group_id IS NOT NULL
""", nativeQuery = true)
BigDecimal calculateGroupAccountCurrentBalance(@Param("accountId") Long accountId);
```

Thêm `currentBalance` vào `AccountResponse` trả về.

**✅ Done khi:**
- Tạo account chung với 10tr initial, chưa có transaction → balance = 10tr
- Thêm chi 2tr → balance = 8tr

---

### ⏰ 14:00–15:30 | T04: Unit Tests SharedAccountService (1.5h)

- `createSharedAccount_adminSuccess()` — ADMIN tạo, groupId và scope đúng
- `createSharedAccount_memberForbidden()` → `ForbiddenException`
- `createSharedAccount_nonMemberForbidden()` → `ForbiddenException`
- `getSharedAccounts_memberCanView()` — MEMBER xem được
- `getSharedAccounts_nonMemberForbidden()` → `ForbiddenException`
- `getSharedAccounts_onlySharedScope()` — không trả PERSONAL accounts

**Commit Day 1:**
```bash
git commit -m "feat: shared account CRUD (create/list/balance) for group context"
```

---

## 📅 DAY 2 — Shared Transaction APIs
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: Shared Account API xong + tested
🎯 Hôm nay: Shared Transaction POST/GET/DELETE + privacy check
🚧 Blocker: Đã có shared account để test transaction chưa?
```

---

### ⏰ 09:00–11:00 | T05: POST Tạo Giao Dịch Chung (2h)

**Endpoint:** `POST /api/groups/{groupId}/transactions`

**Request:** Giống `CreateTransactionRequest` ở Sprint 2, nhưng `accountId` phải là account thuộc group này.

**Logic:**
```java
@Transactional
public TransactionResponse createSharedTransaction(Long groupId,
                                                    CreateTransactionRequest request,
                                                    Long currentUserId) {
    // 1. Phải là member của group (mọi member đều nhập được)
    groupAuthService.requireMember(groupId, currentUserId);

    // 2. accountId phải là shared account của group này
    Account account = accountRepository.findById(request.getAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

    if (!groupId.equals(account.getGroupId())) {
        throw new InvalidTransactionException(
            "Tài khoản này không thuộc nhóm. Chỉ được dùng tài khoản chung của nhóm.");
    }

    // 3. Validation tương tự Personal transaction (amount > 0, date <= today)
    validateTransactionRules(request);

    // 4. Tạo transaction với group_id set, scope = SHARED
    Transaction txn = Transaction.builder()
        .userId(currentUserId)       // ai tạo
        .accountId(request.getAccountId())
        .categoryId(request.getCategoryId())
        .groupId(groupId)            // thuộc group
        .amount(request.getAmount())
        .type(request.getType())
        .description(request.getDescription())
        .transactionDate(request.getTransactionDate())
        .scope(TransactionScope.SHARED)
        .isAutoCategrized(false)
        .build();

    txn = transactionRepository.save(txn);
    return mapToResponse(txn);
}
```

**Business rules:**
- Mọi member đều nhập được giao dịch chung (không chỉ ADMIN)
- Account phải thuộc group (có `group_id = groupId`)
- Không được dùng tài khoản cá nhân (`scope = PERSONAL`) cho giao dịch chung

**✅ Done khi:**
- MEMBER tạo shared transaction → thành công
- Dùng personal account → lỗi rõ ràng
- Account của group khác → lỗi

---

### ⏰ 11:00–13:00 | T06: GET Danh Sách Giao Dịch Chung (2h)

**Endpoint:** `GET /api/groups/{groupId}/transactions?month=&type=&categoryId=&page=`

**Key difference so với personal:** Response phải kèm thông tin **ai tạo** giao dịch đó.

**Response item:**
```json
{
  "id": 10,
  "amount": 250000,
  "type": "EXPENSE",
  "description": "Mua rau củ",
  "transactionDate": "2026-05-15",
  "categoryName": "Ăn uống",
  "accountName": "Quỹ gia đình",
  "createdByUserId": 2,
  "createdByName": "Nguyen Van B",
  "scope": "SHARED"
}
```

**Repository query:**
```java
@Query(value = """
    SELECT t.*, u.full_name as created_by_name,
           c.name as category_name, a.name as account_name
    FROM transactions t
    JOIN users u ON u.id = t.user_id
    LEFT JOIN categories c ON c.id = t.category_id
    LEFT JOIN accounts a ON a.id = t.account_id
    WHERE t.group_id = :groupId
      AND t.scope = 'SHARED'
      AND t.deleted_at IS NULL
      AND (:month IS NULL OR DATE_FORMAT(t.transaction_date, '%Y-%m') = :month)
      AND (:type IS NULL OR t.type = :type)
    ORDER BY t.transaction_date DESC, t.created_at DESC
    LIMIT :size OFFSET :offset
""", nativeQuery = true)
List<Object[]> findSharedTransactions(
    @Param("groupId") Long groupId,
    @Param("month") String month,
    @Param("type") String type,
    @Param("size") int size,
    @Param("offset") int offset
);
```

> 💡 **AI Assist:** Query này JOIN nhiều bảng + native query → nhờ AI generate, tự review lại.

**Privacy check bắt buộc:**
```java
// Chỉ lấy transactions có:
// - group_id = groupId (đúng group)
// - scope = 'SHARED'   (không lọt PERSONAL)
// - deleted_at IS NULL
```

**✅ Done khi:**
- Member xem được giao dịch chung của cả nhóm
- Thấy được "ai tạo" từng giao dịch
- PERSONAL transactions KHÔNG xuất hiện trong list này
- User ngoài group → 403

---

### ⏰ 14:00–15:00 | T07: DELETE Giao Dịch Chung (1h)

**Endpoint:** `DELETE /api/groups/{groupId}/transactions/{transactionId}`

**Ai được xóa?** Có 2 cách tiếp cận — chọn 1:

| Option | Rule | Pros | Cons |
|--------|------|------|------|
| **A** (recommended) | Chỉ người tạo hoặc ADMIN xóa được | Linh hoạt | Phức tạp hơn 1 chút |
| B | Chỉ ADMIN xóa | Đơn giản | Member không tự sửa lỗi mình nhập |

**Implement Option A:**
```java
public void deleteSharedTransaction(Long groupId, Long transactionId, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId);

    Transaction txn = transactionRepository
        .findByIdAndGroupIdAndDeletedAtIsNull(transactionId, groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại"));

    // Người tạo HOẶC ADMIN mới xóa được
    boolean isCreator = txn.getUserId().equals(currentUserId);
    boolean isAdmin = groupAuthService.isAdmin(groupId, currentUserId);

    if (!isCreator && !isAdmin) {
        throw new ForbiddenException("Chỉ người tạo giao dịch hoặc ADMIN mới có thể xóa");
    }

    txn.setDeletedAt(LocalDateTime.now());
    transactionRepository.save(txn);
}
```

---

### ⏰ 15:00–16:00 | T08: Privacy Verification Test (1h)

> **Đây là test quan trọng nhất của Sprint 5.** Phải pass trước khi move sang Day 3.

```
Setup: User A (ADMIN), User B (MEMBER), User C (không phải member)

Test 1 — PERSONAL không lộ:
- User A tạo giao dịch PERSONAL 500k "Chi tiêu riêng"
- User B gọi GET /api/groups/{id}/transactions
- Result: KHÔNG thấy giao dịch 500k đó ✅

Test 2 — SHARED thấy được:
- User B tạo shared transaction 200k "Mua rau"
- User A gọi GET /api/groups/{id}/transactions
- Result: THẤY giao dịch 200k + "created by: User B" ✅

Test 3 — User ngoài group:
- User C gọi GET /api/groups/{id}/transactions
- Result: 403 Forbidden ✅

Test 4 — Xóa giao dịch:
- User B xóa transaction do User B tạo → OK ✅
- User B xóa transaction do User A tạo → 403 ✅
- User A (ADMIN) xóa transaction của User B → OK ✅
```

**Commit Day 2:**
```bash
git commit -m "feat: shared transaction POST/GET/DELETE, privacy isolation verified"
```

---

## 📅 DAY 3 — Shared Budget + Family Summary API
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Shared Transaction APIs + privacy test xong
🎯 Hôm nay: Shared Budget + Family Summary (tổng thu/chi cả nhà)
🚧 Blocker: Privacy test có pass hết không? Đặc biệt Test 1 (PERSONAL không lộ)
```

---

### ⏰ 09:00–11:00 | T09: Shared Budget CRUD (2h)

**Bảng `budgets` đã có từ Sprint 3** (có cột `group_id`). Không cần migration mới.

**Endpoints:**

**`POST /api/groups/{groupId}/budgets`** — Chỉ ADMIN đặt budget chung:
```java
public BudgetResponse createGroupBudget(Long groupId,
                                         CreateBudgetRequest request,
                                         Long currentUserId) {
    groupAuthService.requireAdmin(groupId, currentUserId);

    LocalDate monthDate = YearMonth.parse(request.getMonth()).atDay(1);

    // Check trùng: cùng group + category + tháng
    boolean exists = budgetRepository
        .existsByGroupIdAndCategoryIdAndMonth(groupId, request.getCategoryId(), monthDate);
    if (exists) throw new DuplicateBudgetException(
        "Đã có ngân sách cho danh mục này trong tháng " + request.getMonth());

    Budget budget = Budget.builder()
        .userId(null)                    // group budget → user_id = null
        .groupId(groupId)
        .categoryId(request.getCategoryId())
        .amount(request.getAmount())
        .month(monthDate)
        .build();

    return mapToResponse(budgetRepository.save(budget));
}
```

**`GET /api/groups/{groupId}/budgets?month=`** — Mọi member xem được:
```java
groupAuthService.requireMember(groupId, currentUserId);
return budgetRepository.findByGroupIdAndMonth(groupId, monthDate);
```

**`GET /api/groups/{groupId}/budgets/status?month=`** — Budget Status chung:

Logic giống Sprint 3 nhưng query lấy **shared transactions** của group thay vì personal:
```sql
SELECT b.id, b.category_id, c.name,
       b.amount as budget_amount,
       COALESCE(SUM(t.amount), 0) as actual_amount
FROM budgets b
JOIN categories c ON c.id = b.category_id
LEFT JOIN transactions t ON t.category_id = b.category_id
    AND t.group_id = :groupId          -- chỉ giao dịch của group
    AND t.scope = 'SHARED'             -- chỉ shared
    AND t.type = 'EXPENSE'
    AND t.deleted_at IS NULL
    AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
WHERE b.group_id = :groupId
  AND DATE_FORMAT(b.month, '%Y-%m') = :month
GROUP BY b.id, b.category_id, c.name, b.amount
```

**✅ Done khi:**
- ADMIN tạo budget chung 3tr cho "Ăn uống"
- MEMBER xem được budget status
- Budget Status tính đúng từ shared transactions, không tính personal

---

### ⏰ 11:00–12:30 | T10: Family Summary API (1.5h)

**Endpoint:** `GET /api/groups/{groupId}/summary?month=2026-05`

**Response:**
```json
{
  "month": "2026-05",
  "groupId": 1,
  "groupName": "Gia đình Nguyễn",
  "totalIncome": 5000000,
  "totalExpense": 2750000,
  "netBalance": 2250000,
  "groupAccountBalance": 12250000,
  "categoryBreakdown": [
    {
      "categoryName": "Ăn uống",
      "type": "EXPENSE",
      "total": 1500000,
      "percentage": 54.5
    }
  ]
}
```

**Logic — chỉ tính SHARED transactions của group:**
```java
public FamilySummaryResponse getGroupSummary(Long groupId, String month, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId);

    // Tổng income/expense từ shared transactions
    // (Reuse query tương tự Summary API Sprint 3 nhưng filter group_id + scope=SHARED)

    // Group account balance = tổng tất cả shared accounts của group
    BigDecimal groupBalance = accountRepository
        .findByGroupIdAndScope(groupId, AccountScope.SHARED)
        .stream()
        .map(a -> calculateGroupAccountBalance(a))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return buildFamilySummaryResponse(...);
}
```

**✅ Done khi:**
- Summary chỉ tính shared transactions, không tính personal
- `groupAccountBalance` phản ánh tổng quỹ chung

---

### ⏰ 13:30–15:00 | T11: By-Member Breakdown (1.5h — tiếp theo Day 4)

> **Để sang Day 4** vì đây là query phức tạp nhất Sprint 5. Kết thúc Day 3 sớm → buffer.

**Commit Day 3:**
```bash
git commit -m "feat: shared budget CRUD + status, family summary API"
```

**📊 Check end of Day 3:**
```
T01–T03 ✅ Shared Account APIs
T04     ✅ Unit tests SharedAccount
T05     ✅ Shared Transaction POST
T06     ✅ Shared Transaction GET (với created-by info)
T07     ✅ Shared Transaction DELETE (creator hoặc ADMIN)
T08     ✅ Privacy isolation verified
T09     ✅ Shared Budget CRUD + Status
T10     ✅ Family Summary API
```

---

*→ Tiếp theo: Day 4–6 trong sprint5_daily_p2.md*
