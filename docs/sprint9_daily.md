# 🏃 Sprint 9 — Financial Goal Planning
**Lập kế hoạch tài chính với AI | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** User tạo mục tiêu tài chính → AI phân tích thu chi thực tế → tính toán kế hoạch tiết kiệm → tự động track tiến độ mỗi tháng.

---

## Kiến Trúc Tích Hợp Với Dự Án Hiện Tại

```
Tái sử dụng từ các Sprint trước:
┌─────────────────────────────────────────────────────┐
│  Sprint 2: Account (linked_account_id)               │
│  Sprint 3: Summary API → lấy avg income/expense     │
│  Sprint 6: GeminiClient → gọi AI phân tích          │
│  Sprint 7: InsightDataCollector → pattern data       │
└─────────────────────────────────────────────────────┘
```

**Nguyên tắc thiết kế:**
- Tiến độ = tính từ **balance của tài khoản tiết kiệm liên kết** (không lưu tĩnh)
- AI plan = **cache 1 lần vào DB**, regenerate khi user yêu cầu
- Alert = **rule-based** (không tốn Gemini quota)

---

## DB Schema

### Backlog Sprint 9

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Migration V15: bảng financial_goals | 0.5h | Day 1 |
| T02 | Migration V16: bảng goal_monthly_snapshots | 0.5h | Day 1 |
| T03 | Entity + Repository: 2 bảng mới | 1.5h | Day 1 |
| T04 | DTO layer Sprint 9 | 1h | Day 1 |
| T05 | GoalProgressCalculator: tính tiến độ từ account | 2h | Day 1 |
| T06 | Goal CRUD API (POST/GET/PUT/DELETE) | 2h | Day 2 |
| T07 | GET /api/goals/{id}/progress (tiến độ + lịch sử) | 1.5h | Day 2 |
| T08 | GoalDataCollector: thu thập avg income/expense | 1.5h | Day 2 |
| T09 | Prompt engineering: AI feasibility analysis | 3h | Day 3 |
| T10 | GoalPlanService: generate + cache AI plan | 2h | Day 3 |
| T11 | GET /api/goals/{id}/plan + regenerate endpoint | 1h | Day 3 |
| T12 | GoalAlertService: cảnh báo tháng tiết kiệm ít | 1.5h | Day 4 |
| T13 | Monthly snapshot: lưu tiến độ hàng tháng | 1.5h | Day 4 |
| T14 | Goal status auto-update (COMPLETED/OVERDUE) | 1h | Day 4 |
| T15 | Unit tests GoalPlanService + ProgressCalculator | 2h | Day 4 |
| T16 | Postman collection Sprint 9 | 1h | Day 5 |
| T17 | FE: Goal list page | 1.5h | Day 5 |
| T18 | FE: Create goal form | 1h | Day 5 |
| T19 | FE: Goal detail: AI plan + progress bar | 2h | Day 5 |
| T20 | FE: Monthly progress chart + alert badge | 1h | Day 5 |
| T21 | Integration test + bug fix | 2.5h | Day 6 |
| T22 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Migrations + Entities + Progress Calculator
**6 tiếng | Thứ Hai**

### ⏰ 09:00–10:00 | T01+T02: Migrations (1h)

**`V15__create_financial_goals.sql`:**
```sql
CREATE TABLE financial_goals (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    name              VARCHAR(200) NOT NULL,
    target_amount     DECIMAL(15,2) NOT NULL,
    deadline          DATE NOT NULL,
    linked_account_id BIGINT,                     -- tài khoản tiết kiệm liên kết
    status            ENUM('ACTIVE','COMPLETED','OVERDUE','PAUSED')
                      NOT NULL DEFAULT 'ACTIVE',
    ai_plan           TEXT,                        -- AI plan cached
    ai_plan_generated_at DATETIME,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_goal_user    FOREIGN KEY (user_id)           REFERENCES users(id),
    CONSTRAINT fk_goal_account FOREIGN KEY (linked_account_id) REFERENCES accounts(id)
        ON SET NULL ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> **Lưu ý thiết kế:**
> - `initial_amount` **KHÔNG lưu** → dùng account balance tại thời điểm goal tạo suy ra. Thực ra current progress = account current balance.
> - `linked_account_id` nullable: cho phép goal không link account (track thủ công).
> - Khi account bị xóa → ON SET NULL, goal vẫn tồn tại nhưng mất link.

**`V16__create_goal_monthly_snapshots.sql`:**
```sql
CREATE TABLE goal_monthly_snapshots (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id          BIGINT NOT NULL,
    month            DATE NOT NULL,               -- ngày đầu tháng
    saved_amount     DECIMAL(15,2) NOT NULL,      -- số dư account tháng này
    planned_amount   DECIMAL(15,2),               -- cần tiết kiệm tích lũy đến tháng này
    on_track         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot (goal_id, month),
    CONSTRAINT fk_snapshot_goal FOREIGN KEY (goal_id) REFERENCES financial_goals(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### ⏰ 10:00–11:30 | T03: Entities + Repositories (1.5h)

**Entity `FinancialGoal`:**
```java
@Entity @Table(name = "financial_goals")
@Getter @Setter @Builder
public class FinancialGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private BigDecimal targetAmount;
    @Column(nullable = false) private LocalDate deadline;
    private Long linkedAccountId;

    @Enumerated(EnumType.STRING)
    private GoalStatus status; // ACTIVE, COMPLETED, OVERDUE, PAUSED

    @Column(columnDefinition = "TEXT") private String aiPlan;
    private LocalDateTime aiPlanGeneratedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Enum `GoalStatus`:** ACTIVE, COMPLETED, OVERDUE, PAUSED

**`FinancialGoalRepository`:**
```java
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {
    List<FinancialGoal> findByUserIdOrderByDeadlineAsc(Long userId);
    List<FinancialGoal> findByUserIdAndStatus(Long userId, GoalStatus status);
    Optional<FinancialGoal> findByIdAndUserId(Long id, Long userId);

    // Tìm goals linked với account này
    List<FinancialGoal> findByLinkedAccountId(Long accountId);
}
```

**`GoalMonthlySnapshotRepository`:**
```java
public interface GoalMonthlySnapshotRepository
        extends JpaRepository<GoalMonthlySnapshot, Long> {
    List<GoalMonthlySnapshot> findByGoalIdOrderByMonthAsc(Long goalId);
    Optional<GoalMonthlySnapshot> findByGoalIdAndMonth(Long goalId, LocalDate month);
}
```

---

### ⏰ 11:30–12:30 | T04: DTO Layer (1h)

**Request DTOs:**
```java
// CreateGoalRequest
@NotBlank(message = "Tên mục tiêu không được để trống")
@Size(max = 200) private String name;

@NotNull @DecimalMin("1000")
private BigDecimal targetAmount;

@NotNull @Future(message = "Deadline phải là ngày trong tương lai")
private LocalDate deadline;

private Long linkedAccountId;  // optional
```

**Response DTOs:**

`GoalResponse`:
```java
{
  "id": 1,
  "name": "Mua xe máy Honda Vision",
  "targetAmount": 30000000,
  "deadline": "2026-12-31",
  "status": "ACTIVE",
  "linkedAccountName": "Quỹ mua xe",
  "currentAmount": 7500000,          // balance tính động từ account
  "progressPercentage": 25.0,
  "remainingAmount": 22500000,
  "monthsRemaining": 7,
  "monthlyNeeded": 3214286,          // remaining / monthsRemaining
  "isOnTrack": false,
  "createdAt": "2026-06-01T09:00:00"
}
```

`GoalDetailResponse`: GoalResponse + aiPlan + monthlySnapshots list

`GoalSnapshotResponse`: month, savedAmount, plannedAmount, onTrack, difference

---

### ⏰ 13:30–15:30 | T05: GoalProgressCalculator (2h)

```java
@Service
@RequiredArgsConstructor
public class GoalProgressCalculator {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Tính current amount từ balance của linked account
     * Nếu không có linked account → return null (user track thủ công)
     */
    public BigDecimal calculateCurrentAmount(FinancialGoal goal) {
        if (goal.getLinkedAccountId() == null) return null;

        Account account = accountRepository.findById(goal.getLinkedAccountId())
            .orElse(null);
        if (account == null) return null;

        // Current balance = initial_balance + INCOME - EXPENSE của account này
        BigDecimal income = transactionRepository
            .sumByAccountAndType(account.getId(), "INCOME");
        BigDecimal expense = transactionRepository
            .sumByAccountAndType(account.getId(), "EXPENSE");

        income  = income  != null ? income  : BigDecimal.ZERO;
        expense = expense != null ? expense : BigDecimal.ZERO;

        return account.getBalance().add(income).subtract(expense);
    }

    /**
     * Tính số tiền cần tiết kiệm mỗi tháng từ bây giờ đến deadline
     */
    public BigDecimal calculateMonthlyNeeded(FinancialGoal goal,
                                              BigDecimal currentAmount) {
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;

        BigDecimal remaining = goal.getTargetAmount().subtract(currentAmount);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        long monthsLeft = ChronoUnit.MONTHS.between(
            YearMonth.now(), YearMonth.from(goal.getDeadline()));
        if (monthsLeft <= 0) return remaining; // Deadline đã qua hoặc tháng này

        return remaining.divide(BigDecimal.valueOf(monthsLeft), 0, RoundingMode.CEILING);
    }

    /**
     * Tính % tiến độ
     */
    public double calculateProgress(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0)
            return 0.0;
        if (currentAmount.compareTo(targetAmount) >= 0) return 100.0;

        return currentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    /**
     * Build đầy đủ GoalResponse
     */
    public GoalResponse buildGoalResponse(FinancialGoal goal) {
        BigDecimal current = calculateCurrentAmount(goal);
        BigDecimal monthly = calculateMonthlyNeeded(goal, current);
        double progress  = calculateProgress(current, goal.getTargetAmount());
        long monthsLeft  = Math.max(0, ChronoUnit.MONTHS.between(
            YearMonth.now(), YearMonth.from(goal.getDeadline())));

        String accountName = null;
        if (goal.getLinkedAccountId() != null) {
            accountName = accountRepository.findById(goal.getLinkedAccountId())
                .map(Account::getName).orElse(null);
        }

        return GoalResponse.builder()
            .id(goal.getId())
            .name(goal.getName())
            .targetAmount(goal.getTargetAmount())
            .deadline(goal.getDeadline())
            .status(goal.getStatus())
            .linkedAccountName(accountName)
            .currentAmount(current)
            .progressPercentage(progress)
            .remainingAmount(goal.getTargetAmount().subtract(
                current != null ? current : BigDecimal.ZERO))
            .monthsRemaining(monthsLeft)
            .monthlyNeeded(monthly)
            .isOnTrack(progress >= (100.0 - (monthsLeft * 100.0 /
                Math.max(1, ChronoUnit.MONTHS.between(
                    YearMonth.from(goal.getCreatedAt().toLocalDate()),
                    YearMonth.from(goal.getDeadline()))))))
            .build();
    }
}
```

**Commit Day 1:**
```bash
git commit -m "feat: migrations V15-V16 (financial_goals, snapshots), entities, GoalProgressCalculator"
```

---

## 📅 DAY 2 — Goal CRUD API + Progress API
**6 tiếng | Thứ Ba**

### ⏰ 09:00–11:00 | T06: Goal CRUD API (2h)

**`GoalService` — các methods chính:**

**CREATE:**
```java
@Transactional
public GoalResponse createGoal(CreateGoalRequest request, Long userId) {
    // Validate linked account thuộc sở hữu user
    if (request.getLinkedAccountId() != null) {
        boolean owned = accountRepository
            .existsByIdAndUserId(request.getLinkedAccountId(), userId);
        if (!owned) throw new ForbiddenException(
            "Tài khoản này không thuộc sở hữu của bạn");

        // Chỉ cho phép link account type SAVINGS hoặc CASH
        Account account = accountRepository
            .findById(request.getLinkedAccountId()).orElseThrow();
        if (account.getScope() != AccountScope.PERSONAL)
            throw new BusinessException("Chỉ có thể link tài khoản cá nhân vào mục tiêu");
    }

    FinancialGoal goal = FinancialGoal.builder()
        .userId(userId)
        .name(request.getName())
        .targetAmount(request.getTargetAmount())
        .deadline(request.getDeadline())
        .linkedAccountId(request.getLinkedAccountId())
        .status(GoalStatus.ACTIVE)
        .createdAt(LocalDateTime.now())
        .build();

    goal = goalRepository.save(goal);
    return progressCalculator.buildGoalResponse(goal);
}
```

**GET LIST:**
```java
public List<GoalResponse> getUserGoals(Long userId) {
    return goalRepository.findByUserIdOrderByDeadlineAsc(userId)
        .stream()
        .map(progressCalculator::buildGoalResponse)
        .toList();
}
```

**UPDATE:** Cho phép sửa: name, targetAmount, deadline, linkedAccountId, status (PAUSED/ACTIVE)

**DELETE:** Xóa cứng nếu không có snapshot, xóa mềm nếu có lịch sử

**Endpoints:**
```
POST   /api/goals              → tạo mục tiêu
GET    /api/goals              → danh sách tất cả mục tiêu
GET    /api/goals/{id}         → chi tiết 1 mục tiêu
PUT    /api/goals/{id}         → cập nhật
DELETE /api/goals/{id}        → xóa
```

---

### ⏰ 11:00–12:30 | T07: GET /api/goals/{id}/progress (1.5h)

**Response — tiến độ + lịch sử tháng:**
```json
{
  "goal": { ... },          // GoalResponse
  "monthlyHistory": [
    {
      "month": "2026-06",
      "savedAmount": 7500000,
      "plannedCumulative": 6250000,
      "onTrack": true,
      "difference": 1250000
    },
    {
      "month": "2026-07",
      "savedAmount": 9000000,
      "plannedCumulative": 9464000,
      "onTrack": false,
      "difference": -464000
    }
  ],
  "summary": {
    "totalMonths": 7,
    "onTrackMonths": 4,
    "behindMonths": 2,
    "aheadMonths": 2
  }
}
```

**Logic tính `plannedCumulative` cho tháng N:**
```java
// Từ ngày tạo goal đến deadline có M tháng
// Tháng thứ N → planned tích lũy = targetAmount * (N / M)
// VD: target 30tr, 12 tháng → tháng 3 phải có 7.5tr
BigDecimal plannedCumulative = goal.getTargetAmount()
    .multiply(BigDecimal.valueOf(monthNumber))
    .divide(BigDecimal.valueOf(totalMonths), 0, RoundingMode.HALF_UP);
```

---

### ⏰ 13:30–15:00 | T08: GoalDataCollector (1.5h)

Tái sử dụng pattern từ `InsightDataCollector` (Sprint 7):

```java
@Service
@RequiredArgsConstructor
public class GoalDataCollector {

    private final TransactionRepository txnRepo;

    /**
     * Thu thập data 3 tháng gần nhất để phân tích khả năng tiết kiệm
     */
    public GoalAnalysisData collect(Long userId) {
        // Avg income 3 tháng gần nhất
        BigDecimal avgIncome = BigDecimal.ZERO;
        BigDecimal avgExpense = BigDecimal.ZERO;
        List<CategorySpend> topExpenses = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            String month = YearMonth.now().minusMonths(i).toString();
            BigDecimal inc = txnRepo.sumByUserTypeMonth(userId, "INCOME", month);
            BigDecimal exp = txnRepo.sumByUserTypeMonth(userId, "EXPENSE", month);
            if (inc != null) avgIncome = avgIncome.add(inc);
            if (exp != null) avgExpense = avgExpense.add(exp);
        }

        avgIncome  = avgIncome.divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
        avgExpense = avgExpense.divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
        BigDecimal avgSavings = avgIncome.subtract(avgExpense);

        // Top 5 categories chi nhiều nhất (3 tháng gần nhất)
        topExpenses = txnRepo.getTopExpenseCategoriesLast3Months(userId, 5);

        return GoalAnalysisData.builder()
            .avgMonthlyIncome(avgIncome)
            .avgMonthlyExpense(avgExpense)
            .avgMonthlySavings(avgSavings)
            .topExpenseCategories(topExpenses)
            .build();
    }

    /** Format thành text compact cho Gemini prompt */
    public String formatForPrompt(GoalAnalysisData data, FinancialGoal goal,
                                   BigDecimal currentAmount, BigDecimal monthlyNeeded) {
        long monthsLeft = ChronoUnit.MONTHS.between(
            YearMonth.now(), YearMonth.from(goal.getDeadline()));

        return String.format("""
            MỤC TIÊU: %s
            Số tiền cần: %s
            Đã có: %s
            Còn thiếu: %s
            Deadline: %s (%d tháng nữa)
            Cần tiết kiệm/tháng: %s

            THU CHI TRUNG BÌNH 3 THÁNG GẦN NHẤT:
            - Thu nhập: %s/tháng
            - Chi tiêu: %s/tháng
            - Đang dư: %s/tháng

            TOP CHI TIÊU:
            %s
            """,
            goal.getName(),
            formatVND(goal.getTargetAmount()),
            formatVND(currentAmount),
            formatVND(goal.getTargetAmount().subtract(currentAmount)),
            goal.getDeadline(), monthsLeft,
            formatVND(monthlyNeeded),
            formatVND(data.getAvgMonthlyIncome()),
            formatVND(data.getAvgMonthlyExpense()),
            formatVND(data.getAvgMonthlySavings()),
            data.getTopExpenseCategories().stream()
                .map(c -> "  - " + c.getCategoryName() + ": " + formatVND(c.getAmount()))
                .collect(Collectors.joining("\n"))
        );
    }
}
```

**Commit Day 2:**
```bash
git commit -m "feat: Goal CRUD API, Progress API, GoalDataCollector"
```

---

## 📅 DAY 3 — AI Plan: Prompt Engineering + GoalPlanService
**6 tiếng | Thứ Tư**

### ⏰ 09:00–12:00 | T09: Prompt Engineering AI Feasibility (3h)

> **Timebox: 3h.** Mục tiêu: AI output ra đoạn text thực tế, rõ ràng, có con số cụ thể.

**Bước 1 — Xác định 3 scenarios cần test (30 phút):**

| Scenario | Mô tả |
|----------|-------|
| **Feasible** | Đang dư 3tr/tháng, cần 2.5tr/tháng → AI khuyến khích |
| **Tight** | Đang dư 1tr/tháng, cần 2.5tr/tháng → AI gợi ý cắt giảm |
| **Infeasible** | Đang dư 500k, cần 3tr/tháng, còn 6 tháng → AI đề xuất lùi deadline |

**Bước 2 — System Prompt:**
```
Bạn là chuyên gia tư vấn tài chính cá nhân tại Việt Nam.
Dựa vào dữ liệu thu chi thực tế và mục tiêu tài chính của user,
hãy viết kế hoạch bằng tiếng Việt tự nhiên.

Kế hoạch phải gồm:
1. Đánh giá khả năng đạt mục tiêu (có thể/khó/không thể đúng hạn)
2. Tính toán cụ thể (số tiền cần/tháng, so sánh với khả năng hiện tại)
3. Gợi ý cụ thể: nếu cần cắt giảm thì cắt danh mục nào, bao nhiêu
4. Nếu không thể đúng hạn: đề xuất deadline thực tế hơn

Yêu cầu: có con số cụ thể, thực tế, không nói chung chung.
Dài 4-6 câu. Không dùng bullet points. Thân thiện.
```

**Bước 3 — Test với data thực và chọn prompt:**

Ví dụ output mong muốn (scenario "Tight"):
```
"Để mua xe Honda Vision (30 triệu) trong 10 tháng, bạn cần để dành
2.500.000đ/tháng. Tuy nhiên, dựa trên thu chi 3 tháng gần nhất, bạn
đang dư khoảng 1.000.000đ/tháng — chỉ đạt 40% mục tiêu. Nếu cắt giảm
chi Giải trí từ 800.000đ xuống 300.000đ và Ăn uống từ 3.200.000đ xuống
2.700.000đ, bạn có thể tiết kiệm thêm 1.000.000đ/tháng, đạt 2.000.000đ/tháng.
Với tốc độ đó, bạn sẽ đạt mục tiêu trong khoảng 13 tháng thay vì 10 tháng."
```

---

### ⏰ 12:00–14:00 | T10: GoalPlanService (2h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GoalPlanService {

    private final GeminiClient geminiClient;
    private final GoalDataCollector dataCollector;
    private final GoalProgressCalculator progressCalculator;
    private final FinancialGoalRepository goalRepository;
    private final AiCallLogService logService;

    private static final String SYSTEM_PROMPT = """
        Bạn là chuyên gia tư vấn tài chính cá nhân tại Việt Nam.
        Dựa vào dữ liệu thu chi thực tế và mục tiêu tài chính của user,
        viết kế hoạch bằng tiếng Việt tự nhiên, 4-6 câu, có con số cụ thể.
        Gồm: đánh giá khả năng → tính toán cụ thể → gợi ý cắt giảm nếu cần → deadline thực tế.
        Thân thiện, không phán xét, tập trung vào giải pháp.
        """;

    /**
     * Lấy AI plan — từ DB cache hoặc generate mới
     */
    public GoalPlanResponse getOrGeneratePlan(Long goalId, Long userId) {
        FinancialGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Mục tiêu không tồn tại"));

        // Check cache: đã có plan và plan < 7 ngày tuổi
        if (goal.getAiPlan() != null && goal.getAiPlanGeneratedAt() != null) {
            boolean isFresh = goal.getAiPlanGeneratedAt()
                .isAfter(LocalDateTime.now().minusDays(7));
            if (isFresh) {
                return GoalPlanResponse.builder()
                    .plan(goal.getAiPlan())
                    .generatedAt(goal.getAiPlanGeneratedAt())
                    .isFromCache(true)
                    .build();
            }
        }

        return generateAndSavePlan(goal, userId);
    }

    @Transactional
    public GoalPlanResponse regeneratePlan(Long goalId, Long userId) {
        FinancialGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Mục tiêu không tồn tại"));
        return generateAndSavePlan(goal, userId);
    }

    private GoalPlanResponse generateAndSavePlan(FinancialGoal goal, Long userId) {
        BigDecimal currentAmount = progressCalculator.calculateCurrentAmount(goal);
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;

        BigDecimal monthlyNeeded = progressCalculator
            .calculateMonthlyNeeded(goal, currentAmount);

        GoalAnalysisData data = dataCollector.collect(userId);
        String promptData = dataCollector.formatForPrompt(data, goal, currentAmount, monthlyNeeded);

        long start = System.currentTimeMillis();
        String aiPlan = geminiClient.chat(SYSTEM_PROMPT, promptData);
        long latency = System.currentTimeMillis() - start;

        if (aiPlan == null || aiPlan.isBlank()) {
            logService.logFailure("GOAL_PLAN", "Gemini returned null");
            // Fallback: generate plan bằng rule-based
            aiPlan = generateFallbackPlan(goal, currentAmount, monthlyNeeded, data);
        } else {
            logService.logSuccess("GOAL_PLAN", "gemini-2.0-flash", 0, latency);
        }

        // Lưu cache
        goal.setAiPlan(aiPlan);
        goal.setAiPlanGeneratedAt(LocalDateTime.now());
        goalRepository.save(goal);

        return GoalPlanResponse.builder()
            .plan(aiPlan)
            .generatedAt(LocalDateTime.now())
            .isFromCache(false)
            .build();
    }

    /** Fallback khi Gemini fail — plan đơn giản từ rule */
    private String generateFallbackPlan(FinancialGoal goal, BigDecimal current,
                                         BigDecimal monthly, GoalAnalysisData data) {
        long months = ChronoUnit.MONTHS.between(YearMonth.now(),
            YearMonth.from(goal.getDeadline()));
        BigDecimal gap = monthly.subtract(data.getAvgMonthlySavings());

        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return String.format(
                "Mục tiêu %s hoàn toàn khả thi! Bạn cần tiết kiệm %s/tháng " +
                "và thu nhập hiện tại của bạn cho phép điều này.",
                goal.getName(), formatVND(monthly));
        } else {
            return String.format(
                "Để đạt mục tiêu %s trong %d tháng, bạn cần tiết kiệm %s/tháng. " +
                "Hiện tại bạn đang dư %s/tháng, cần cắt giảm thêm %s/tháng từ chi tiêu.",
                goal.getName(), months, formatVND(monthly),
                formatVND(data.getAvgMonthlySavings()), formatVND(gap));
        }
    }
}
```

---

### ⏰ 14:30–15:30 | T11: Controller Plan Endpoints (1h)

```java
// GET /api/goals/{id}/plan → lấy plan (cache 7 ngày)
@GetMapping("/{id}/plan")
public ResponseEntity<ApiResponse<GoalPlanResponse>> getPlan(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = securityUtils.getCurrentUserId(userDetails);
    return ResponseEntity.ok(ApiResponse.success(
        goalPlanService.getOrGeneratePlan(id, userId)));
}

// POST /api/goals/{id}/plan/regenerate → force regenerate
@PostMapping("/{id}/plan/regenerate")
public ResponseEntity<ApiResponse<GoalPlanResponse>> regenerate(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = securityUtils.getCurrentUserId(userDetails);
    return ResponseEntity.ok(ApiResponse.success(
        goalPlanService.regeneratePlan(id, userId)));
}
```

**Commit Day 3:**
```bash
git commit -m "feat: prompt engineering (goal plan), GoalPlanService with DB cache, plan endpoints"
```

---

## 📅 DAY 4 — Alert + Monthly Snapshot + Status Update + Tests
**6 tiếng | Thứ Năm**

### ⏰ 09:00–10:30 | T12: GoalAlertService (1.5h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GoalAlertService {

    private final FinancialGoalRepository goalRepository;
    private final GoalProgressCalculator progressCalculator;

    /**
     * Kiểm tra goal có đang bị trễ không
     * Gọi khi user GET goals hoặc vào dashboard
     */
    public GoalAlertStatus checkAlert(FinancialGoal goal) {
        BigDecimal current = progressCalculator.calculateCurrentAmount(goal);
        if (current == null || goal.getStatus() != GoalStatus.ACTIVE)
            return GoalAlertStatus.NONE;

        long totalMonths = ChronoUnit.MONTHS.between(
            YearMonth.from(goal.getCreatedAt().toLocalDate()),
            YearMonth.from(goal.getDeadline()));

        long elapsedMonths = ChronoUnit.MONTHS.between(
            YearMonth.from(goal.getCreatedAt().toLocalDate()),
            YearMonth.now());

        if (totalMonths <= 0) return GoalAlertStatus.NONE;

        double expectedProgress = (double) elapsedMonths / totalMonths * 100;
        double actualProgress   = progressCalculator.calculateProgress(
            current, goal.getTargetAmount());

        double gap = expectedProgress - actualProgress;

        if (gap >= 20) return GoalAlertStatus.SIGNIFICANTLY_BEHIND;  // Chậm ≥ 20%
        if (gap >= 10) return GoalAlertStatus.SLIGHTLY_BEHIND;        // Chậm 10-20%
        if (actualProgress >= 100) return GoalAlertStatus.COMPLETED;
        return GoalAlertStatus.ON_TRACK;
    }

    public String getAlertMessage(GoalAlertStatus status, FinancialGoal goal) {
        return switch (status) {
            case SIGNIFICANTLY_BEHIND ->
                "⚠️ Mục tiêu '" + goal.getName() + "' đang chậm đáng kể so với kế hoạch!";
            case SLIGHTLY_BEHIND ->
                "💛 Mục tiêu '" + goal.getName() + "' đang hơi chậm, cần để dành thêm.";
            case ON_TRACK ->
                "✅ Mục tiêu '" + goal.getName() + "' đang đúng tiến độ.";
            case COMPLETED ->
                "🎉 Mục tiêu '" + goal.getName() + "' đã hoàn thành!";
            default -> null;
        };
    }
}

enum GoalAlertStatus { NONE, ON_TRACK, SLIGHTLY_BEHIND, SIGNIFICANTLY_BEHIND, COMPLETED }
```

**Thêm `alertStatus` và `alertMessage` vào `GoalResponse`.**

---

### ⏰ 10:30–12:00 | T13+T14: Monthly Snapshot + Status Auto-Update (1.5h + 1h)

**Monthly Snapshot — chụp tiến độ cuối tháng:**
```java
// Gọi bằng @Scheduled cuối tháng, hoặc trigger khi user GET progress
@Transactional
public void takeMonthlySnapshot(Long goalId) {
    FinancialGoal goal = goalRepository.findById(goalId).orElseThrow();
    LocalDate monthDate = YearMonth.now().atDay(1);

    // Tránh duplicate
    if (snapshotRepository.findByGoalIdAndMonth(goalId, monthDate).isPresent()) return;

    BigDecimal currentAmount = progressCalculator.calculateCurrentAmount(goal);
    BigDecimal monthlyNeeded = progressCalculator.calculateMonthlyNeeded(goal, currentAmount);

    // Planned cumulative = target * (elapsed months / total months)
    long elapsed = ChronoUnit.MONTHS.between(
        YearMonth.from(goal.getCreatedAt().toLocalDate()), YearMonth.now());
    long total   = ChronoUnit.MONTHS.between(
        YearMonth.from(goal.getCreatedAt().toLocalDate()),
        YearMonth.from(goal.getDeadline()));

    BigDecimal planned = total > 0
        ? goal.getTargetAmount()
              .multiply(BigDecimal.valueOf(elapsed))
              .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    boolean onTrack = currentAmount != null
        && currentAmount.compareTo(planned) >= 0;

    snapshotRepository.save(GoalMonthlySnapshot.builder()
        .goalId(goalId).month(monthDate)
        .savedAmount(currentAmount != null ? currentAmount : BigDecimal.ZERO)
        .plannedAmount(planned).onTrack(onTrack).build());
}
```

**Auto-update status:**
```java
@Transactional
public void updateGoalStatus(FinancialGoal goal, BigDecimal currentAmount) {
    if (goal.getStatus() == GoalStatus.PAUSED) return; // user đang tạm dừng

    if (currentAmount != null
            && currentAmount.compareTo(goal.getTargetAmount()) >= 0) {
        goal.setStatus(GoalStatus.COMPLETED);
        goalRepository.save(goal);
        log.info("Goal completed: goalId={}, name={}", goal.getId(), goal.getName());
        return;
    }

    if (goal.getDeadline().isBefore(LocalDate.now())
            && goal.getStatus() == GoalStatus.ACTIVE) {
        goal.setStatus(GoalStatus.OVERDUE);
        goalRepository.save(goal);
    }
}
```

---

### ⏰ 13:00–15:00 | T15: Unit Tests (2h)

**GoalProgressCalculator tests:**
- `calculateProgress_zero()` → 0.0%
- `calculateProgress_half()` → 50.0%
- `calculateProgress_full()` → 100.0%
- `calculateProgress_over()` → 100.0% (không vượt 100)
- `calculateMonthlyNeeded_6months()` → `remaining / 6`
- `calculateMonthlyNeeded_deadlinePassed()` → `remaining` (toàn bộ còn lại)
- `calculateMonthlyNeeded_alreadyAchieved()` → 0

**GoalPlanService tests:**
- `getPlan_cacheHit_noGeminiCall()` — plan < 7 ngày → cache
- `getPlan_cacheStale_callGemini()` — plan > 7 ngày → regenerate
- `getPlan_geminiFail_fallback()` → fallback plan text, không crash
- `regeneratePlan_alwaysCallGemini()` — force regenerate bỏ cache

**GoalAlertService tests:**
- `checkAlert_onTrack()` → ON_TRACK
- `checkAlert_behind20percent()` → SIGNIFICANTLY_BEHIND
- `checkAlert_completed()` → COMPLETED
- `checkAlert_paused()` → NONE

**Commit Day 4:**
```bash
git commit -m "feat: GoalAlertService, monthly snapshot, auto-status update (COMPLETED/OVERDUE), unit tests GREEN"
```

---

## 📅 DAY 5 — Postman + FE Integration
**6 tiếng | Thứ Sáu**

### ⏰ 09:00–10:00 | T16: Postman Collection Sprint 9 (1h)

```
📁 Financial Goals (Sprint 9)
  ├── POST /api/goals
  │     Body: { name, targetAmount, deadline, linkedAccountId }
  │     Expected: 201 + GoalResponse với progressPercentage
  │
  ├── GET /api/goals
  │     Expected: list với alertStatus mỗi goal
  │
  ├── GET /api/goals/{id}
  │     Expected: detail + currentAmount từ account
  │
  ├── GET /api/goals/{id}/plan (lần 1 → AI)
  │     Expected: plan tiếng Việt, isFromCache: false
  │
  ├── GET /api/goals/{id}/plan (lần 2)
  │     Expected: isFromCache: true (< 7 ngày)
  │
  ├── POST /api/goals/{id}/plan/regenerate
  │     Expected: plan mới, isFromCache: false
  │
  ├── GET /api/goals/{id}/progress
  │     Expected: monthlyHistory + summary
  │
  ├── PUT /api/goals/{id}
  │     Body: { status: "PAUSED" }
  │
  └── DELETE /api/goals/{id}
```

**Test tự động:**
```javascript
// Plan not empty
pm.test("AI plan not empty", () => {
    pm.expect(pm.response.json().data.plan).to.not.be.empty;
});

// Progress in valid range
pm.test("Progress 0-100", () => {
    const p = pm.response.json().data.progressPercentage;
    pm.expect(p).to.be.at.least(0).and.at.most(100);
});
```

---

### ⏰ 10:00–11:30 | T17+T18: FE — Goal List + Create Form (1.5h + 1h)

**Goal List Page:**
```jsx
const GoalListPage = () => {
    const [goals, setGoals] = useState([]);

    useEffect(() => {
        api.get('/goals').then(res => setGoals(res.data.data));
    }, []);

    const alertColors = {
        ON_TRACK:             '#10b981',
        SLIGHTLY_BEHIND:      '#f59e0b',
        SIGNIFICANTLY_BEHIND: '#ef4444',
        COMPLETED:            '#6366f1',
        NONE:                 '#6b7280',
    };

    return (
        <div className="goals-page">
            <div className="goals-header">
                <h2>Mục tiêu tài chính</h2>
                <button onClick={() => setShowCreate(true)}>+ Tạo mục tiêu</button>
            </div>

            <div className="goals-grid">
                {goals.map(goal => (
                    <div key={goal.id} className="goal-card"
                         onClick={() => navigate(`/goals/${goal.id}`)}>
                        <h3>{goal.name}</h3>
                        <div className="goal-progress-bar">
                            <div className="progress-fill"
                                 style={{
                                     width: `${goal.progressPercentage}%`,
                                     backgroundColor:
                                         alertColors[goal.alertStatus] || '#6366f1'
                                 }} />
                        </div>
                        <div className="goal-stats">
                            <span>{formatVND(goal.currentAmount)} / {formatVND(goal.targetAmount)}</span>
                            <span>{goal.progressPercentage.toFixed(1)}%</span>
                        </div>
                        <div className="goal-footer">
                            <span>Còn {goal.monthsRemaining} tháng</span>
                            {goal.alertMessage && (
                                <span className="alert-msg">{goal.alertMessage}</span>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};
```

**Create Goal Form:**
```jsx
// Fields: name, targetAmount, deadline (date picker), linkedAccountId (dropdown)
// Chỉ hiện accounts scope=PERSONAL trong dropdown

const [personalAccounts, setPersonalAccounts] = useState([]);
useEffect(() => {
    api.get('/accounts').then(res =>
        setPersonalAccounts(res.data.data.filter(a => a.scope === 'PERSONAL'))
    );
}, []);
```

---

### ⏰ 12:00–14:00 | T19: FE — Goal Detail: AI Plan + Progress (2h)

```jsx
const GoalDetailPage = () => {
    const { id } = useParams();
    const [goal, setGoal] = useState(null);
    const [plan, setPlan] = useState(null);
    const [progress, setProgress] = useState(null);
    const [loadingPlan, setLoadingPlan] = useState(false);

    useEffect(() => {
        Promise.all([
            api.get(`/goals/${id}`),
            api.get(`/goals/${id}/plan`),
            api.get(`/goals/${id}/progress`)
        ]).then(([g, p, pr]) => {
            setGoal(g.data.data);
            setPlan(p.data.data);
            setProgress(pr.data.data);
        });
    }, [id]);

    return (
        <div className="goal-detail">
            {/* Header + Progress */}
            <div className="goal-header-card">
                <h2>{goal?.name}</h2>
                <div className="big-progress">
                    <div className="progress-circle"
                         style={{ '--p': goal?.progressPercentage }}>
                        <span>{goal?.progressPercentage?.toFixed(0)}%</span>
                    </div>
                    <div className="goal-numbers">
                        <p>{formatVND(goal?.currentAmount)} đã tiết kiệm</p>
                        <p>Cần thêm {formatVND(goal?.remainingAmount)}</p>
                        <p>Còn {goal?.monthsRemaining} tháng | Cần {formatVND(goal?.monthlyNeeded)}/tháng</p>
                    </div>
                </div>
            </div>

            {/* AI Plan Card */}
            <div className="ai-plan-card">
                <div className="plan-header">
                    <span>🤖 Kế hoạch AI</span>
                    {plan?.isFromCache && <span className="cache-tag">cached</span>}
                    <button onClick={async () => {
                        setLoadingPlan(true);
                        const res = await api.post(`/goals/${id}/plan/regenerate`);
                        setPlan(res.data.data);
                        setLoadingPlan(false);
                    }} disabled={loadingPlan}>
                        {loadingPlan ? '...' : '🔄 Tạo lại'}
                    </button>
                </div>
                <p className="plan-content">
                    {loadingPlan ? 'AI đang phân tích...' : plan?.plan}
                </p>
            </div>

            {/* Monthly Progress Chart */}
            <div className="monthly-chart">
                <h3>Tiến độ theo tháng</h3>
                <Bar data={{
                    labels: progress?.monthlyHistory?.map(m => m.month),
                    datasets: [
                        {
                            label: 'Thực tế',
                            data: progress?.monthlyHistory?.map(m => m.savedAmount),
                            backgroundColor: '#6366f1'
                        },
                        {
                            label: 'Kế hoạch',
                            data: progress?.monthlyHistory?.map(m => m.plannedCumulative),
                            backgroundColor: '#e5e7eb',
                            type: 'line'
                        }
                    ]
                }} />
            </div>
        </div>
    );
};
```

---

### ⏰ 14:30–15:30 | T20: FE — Alert Badge (1h)

**Hiện cảnh báo trong Dashboard khi có goal bị trễ:**
```jsx
{/* Trong Dashboard, phần dưới Summary cards */}
{goals.filter(g => g.alertStatus === 'SIGNIFICANTLY_BEHIND').length > 0 && (
    <div className="goal-alert-banner">
        ⚠️ {goals.filter(...).length} mục tiêu đang chậm tiến độ —
        <button onClick={() => navigate('/goals')}>Xem ngay</button>
    </div>
)}
```

**Commit Day 5:**
```bash
git commit -m "feat: Postman Sprint 9, FE goal list + create + detail with AI plan + monthly chart"
```

---

## 📅 DAY 6 — Integration Test + Bug Fix + Sprint Review
**6 tiếng | Thứ Bảy**

### ⏰ 09:00–11:00 | Integration Test

**Test flow đầy đủ:**
```
1.  Tạo account "Quỹ mua xe" 2,000,000đ initial                 ✅/❌
2.  Tạo goal "Mua xe Honda Vision" 30tr, 12/2026, link account  ✅/❌
3.  GET goal → currentAmount = 2,000,000đ (từ account balance)  ✅/❌
4.  GET goal → progressPercentage = 6.7%                         ✅/❌
5.  GET goal → monthlyNeeded = 4,000,000đ (28tr / 7 tháng)     ✅/❌
6.  GET /api/goals/{id}/plan → AI plan tiếng Việt               ✅/❌
7.  GET lại plan → isFromCache: true                             ✅/❌
8.  Thêm transaction INCOME vào "Quỹ mua xe" 3,000,000đ         ✅/❌
9.  GET goal → currentAmount = 5,000,000đ, progress = 16.7%    ✅/❌
10. POST plan/regenerate → plan mới phản ánh đúng data          ✅/❌
11. Pause goal → status PAUSED, alert NONE                      ✅/❌
12. Goal không link account → currentAmount null, form OK        ✅/❌
13. Deadline trong quá khứ → 400 validation error              ✅/❌
14. Link account của user khác → 403                          ✅/❌
```

**Common bugs:**
| Bug | Fix |
|-----|-----|
| Progress > 100% khi balance vượt target | `Math.min(100.0, progress)` |
| monthlyNeeded âm khi đã đạt | Check `remaining <= 0 → return ZERO` |
| AI plan tạo lại khi chưa đủ 7 ngày | Kiểm tra `aiPlanGeneratedAt` đúng |
| Account bị xóa → goal crash | `ON SET NULL` migration + null check |

### ⏰ 11:00–12:00 | Sprint Review (1h)

**Deliverables:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | Goal CRUD API (POST/GET/PUT/DELETE) | ⬜ |
| 2 | Progress tính động từ account balance | ⬜ |
| 3 | AI plan tiếng Việt, có con số cụ thể | ⬜ |
| 4 | AI plan cache 7 ngày, regenerate khi cần | ⬜ |
| 5 | Fallback plan khi Gemini fail | ⬜ |
| 6 | Monthly snapshot + on_track flag | ⬜ |
| 7 | Auto-update COMPLETED/OVERDUE | ⬜ |
| 8 | Alert SIGNIFICANTLY_BEHIND/SLIGHTLY_BEHIND | ⬜ |
| 9 | FE: Goal list với progress bar + alert | ⬜ |
| 10 | FE: Goal detail với AI plan + monthly chart | ⬜ |
| 11 | Unit tests GREEN | ⬜ |

**Commit cuối:**
```bash
git commit -m "chore: integration bugs fixed, Sprint 9 complete"
git tag -a sprint-9 -m "Sprint 9: Financial Goal Planning"
git push origin main --tags
```

---

## 📊 Sprint 9 Summary

### Thời Gian
```
Day 1: Migrations + Entities + Progress Calculator  → 6h
Day 2: Goal CRUD + Progress API + DataCollector     → 6h
Day 3: Prompt Engineering + GoalPlanService         → 6h  ← AI core
Day 4: Alert + Snapshot + Status + Tests            → 6h
Day 5: Postman + FE Integration                     → 6h
Day 6: Integration Test + Bug Fix + Review          → 6h
─────────────────────────────────────────────────────────
Total:                                              36h
```

### Điểm Độc Đáo So Với Các Sprint Trước

| Feature | Cách tiếp cận |
|---------|--------------|
| Progress tracking | Tính động từ account balance (không lưu tĩnh) → luôn chính xác |
| AI plan | Cache 7 ngày (khác Insight cache 1 tháng) → balance giữa freshness và quota |
| Alert | Rule-based (% gap) → 0 Gemini quota |
| Snapshot | Chụp cuối tháng → lịch sử tiến độ |

### Gemini Quota tăng thêm
```
Goal Plan generate: ~1 call / goal tạo mới hoặc 7 ngày
Ước tính: +5-10 calls/user/tháng → vẫn trong free tier
```

---

*Sprint 9 Complete → Financial Goal Planning ✅*
