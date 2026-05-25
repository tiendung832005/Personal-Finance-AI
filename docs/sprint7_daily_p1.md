# 🏃 Sprint 7 — Daily Breakdown (Day 1–3)
**AI: Insight, Anomaly Detection & Financial Health Score | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** Cuối tháng → AI sinh ra insight tiêu tiền bằng tiếng Việt tự nhiên + phát hiện chi tiêu bất thường + tính điểm sức khỏe tài chính (0–100).

> **Khác Sprint 6:** Sprint 6 classify 1 transaction ≤ 50 tokens.
> Sprint 7 **phân tích cả tháng data** → prompt dài hơn nhiều → cần tính toán token cẩn thận để không vượt free tier.

> **Chiến lược tiết kiệm quota:**
> - Insight chỉ generate **1 lần/tháng** (không phải mỗi request)
> - Cache kết quả vào DB → các request sau đọc từ DB
> - Anomaly: phát hiện bằng **rule-based thuần Java** → chỉ dùng Gemini để **giải thích** bằng ngôn ngữ tự nhiên

---

## Backlog Sprint 7

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Migration V11: bảng ai_insights | 0.5h | Day 1 |
| T02 | Migration V12: bảng financial_health_scores | 0.5h | Day 1 |
| T03 | Migration V13: bảng spending_anomalies | 0.5h | Day 1 |
| T04 | Entity + Repository: 3 bảng mới | 1.5h | Day 1 |
| T05 | DTO layer Sprint 7 | 1h | Day 1 |
| T06 | InsightDataCollector: thu thập data tháng | 2h | Day 1 |
| T07 | Prompt engineering: Monthly Insight | 3h | Day 2 |
| T08 | InsightService: generate + cache vào DB | 2h | Day 2 |
| T09 | GET /api/insights/monthly?month= | 1h | Day 2 |
| T10 | AnomalyDetector: rule-based detection | 2.5h | Day 3 |
| T11 | AnomalyExplainer: Gemini giải thích anomaly | 2h | Day 3 |
| T12 | GET /api/anomalies?month= | 1h | Day 3 |
| T13 | HealthScoreCalculator: thuật toán 0-100 | 2.5h | Day 4 |
| T14 | GET /api/health-score?month= | 1h | Day 4 |
| T15 | Family Insight API | 2h | Day 4 |
| T16 | Unit tests Sprint 7 | 2h | Day 5 |
| T17 | Postman collection Sprint 7 | 1h | Day 5 |
| T18 | FE: Monthly Insight card | 1.5h | Day 5 |
| T19 | FE: Anomaly alerts list | 1.5h | Day 5 |
| T20 | FE: Health Score gauge | 1h | Day 5 |
| T21 | Bug fix + integration test | 2.5h | Day 6 |
| T22 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Migrations + Entities + Data Collector
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 6 xong — Auto-categorize với Gemini hoạt động
🎯 Hôm nay: 3 migration mới + entities + InsightDataCollector
🚧 Blocker: DB có đủ transaction data (ít nhất 2 tháng) để test insight không?
            Nếu chưa → seed thêm data trước Day 2
```

---

### ⏰ 09:00–10:00 | T01–T03: 3 Migrations (1h)

**`V11__create_ai_insights.sql`:**
```sql
CREATE TABLE ai_insights (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT,
    group_id     BIGINT NULL,
    month        DATE NOT NULL,           -- ngày đầu tháng: 2026-05-01
    insight_type VARCHAR(30) NOT NULL,    -- PERSONAL, FAMILY
    content      TEXT NOT NULL,           -- nội dung AI sinh ra (tiếng Việt)
    model        VARCHAR(50),
    tokens_used  INT,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_insight (user_id, month, insight_type),
    CONSTRAINT fk_insight_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V12__create_financial_health_scores.sql`:**
```sql
CREATE TABLE financial_health_scores (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    month               DATE NOT NULL,
    overall_score       INT NOT NULL,       -- 0-100
    savings_score       INT,                -- điểm tiết kiệm
    budget_score        INT,                -- điểm tuân thủ budget
    spending_trend_score INT,               -- điểm xu hướng chi tiêu
    debt_score          INT,                -- điểm nợ (nếu có)
    score_label         VARCHAR(20),        -- POOR/FAIR/GOOD/EXCELLENT
    calculated_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_score (user_id, month),
    CONSTRAINT fk_hs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V13__create_spending_anomalies.sql`:**
```sql
CREATE TABLE spending_anomalies (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    transaction_id   BIGINT NOT NULL,
    anomaly_type     VARCHAR(50) NOT NULL,  -- UNUSUAL_AMOUNT, NEW_CATEGORY, FREQUENCY_SPIKE
    severity         VARCHAR(10) NOT NULL,  -- LOW, MEDIUM, HIGH
    explanation      TEXT,                  -- Gemini giải thích
    is_dismissed     BOOLEAN DEFAULT FALSE, -- user đã đọc/bỏ qua
    detected_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anomaly_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_anomaly_txn  FOREIGN KEY (transaction_id) REFERENCES transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### ⏰ 10:00–11:30 | T04: Entities + Repositories (1.5h)

**Entity `AiInsight`:** id, userId, groupId, month (LocalDate), insightType (enum), content (String TEXT), model, tokensUsed, createdAt

**Entity `FinancialHealthScore`:** id, userId, month, overallScore, savingsScore, budgetScore, spendingTrendScore, scoreLabel (enum: POOR/FAIR/GOOD/EXCELLENT), calculatedAt

**Entity `SpendingAnomaly`:** id, userId, transactionId, anomalyType (enum), severity (enum), explanation, isDismissed, detectedAt

**Enum `AnomalyType`:**
```java
public enum AnomalyType {
    UNUSUAL_AMOUNT,     // Giao dịch cao bất thường (>3x trung bình)
    NEW_CATEGORY,       // Danh mục chưa từng chi trước đây
    FREQUENCY_SPIKE,    // Chi quá nhiều lần trong ngắn
    LARGE_SINGLE_TXN    // Giao dịch đơn lẻ rất lớn (>30% thu nhập)
}
```

**Repositories cần:**
```java
// AiInsight
Optional<AiInsight> findByUserIdAndMonthAndInsightType(Long userId, LocalDate month, String type);

// FinancialHealthScore
Optional<FinancialHealthScore> findByUserIdAndMonth(Long userId, LocalDate month);

// SpendingAnomaly
List<SpendingAnomaly> findByUserIdAndDetectedAtBetweenAndIsDismissedFalse(
    Long userId, LocalDateTime from, LocalDateTime to);
boolean existsByUserIdAndTransactionId(Long userId, Long transactionId);
```

---

### ⏰ 11:30–12:00 | T05: DTO Layer (30 phút)

**Response DTOs:**
- `InsightResponse`: month, content, createdAt, isFromCache (bool)
- `HealthScoreResponse`: month, overallScore, savingsScore, budgetScore, spendingTrendScore, scoreLabel, breakdown (list)
- `AnomalyResponse`: id, transactionId, description, amount, anomalyType, severity, explanation, detectedAt

---

### ⏰ 13:00–15:30 | T06: InsightDataCollector (2h)

**Mục đích:** Thu thập và format data tháng thành text compact để đưa vào Gemini prompt. Token count phải nhỏ.

```java
@Service
@RequiredArgsConstructor
public class InsightDataCollector {

    private final TransactionRepository txnRepo;
    private final BudgetRepository budgetRepo;

    /**
     * Thu thập data tháng, format thành text ngắn gọn cho AI prompt
     */
    public InsightData collectForMonth(Long userId, String month) {
        LocalDate monthDate = YearMonth.parse(month).atDay(1);
        LocalDate nextMonth = monthDate.plusMonths(1);

        // 1. Tổng thu/chi
        BigDecimal totalIncome  = txnRepo.sumByUserTypeMonth(userId, "INCOME", month);
        BigDecimal totalExpense = txnRepo.sumByUserTypeMonth(userId, "EXPENSE", month);
        BigDecimal savingsRate  = calculateSavingsRate(totalIncome, totalExpense);

        // 2. Top 5 category chi nhiều nhất
        List<CategorySpending> topExpenses = txnRepo
            .getTopExpenseCategories(userId, month, 5);

        // 3. So sánh với tháng trước
        String lastMonth = YearMonth.parse(month).minusMonths(1).toString();
        BigDecimal lastExpense = txnRepo.sumByUserTypeMonth(userId, "EXPENSE", lastMonth);
        BigDecimal expenseChange = totalExpense.subtract(lastExpense);

        // 4. Budget compliance (bao nhiêu % category vượt budget)
        List<BudgetStatus> budgetStatuses = getBudgetStatuses(userId, month, monthDate);
        long exceededCount = budgetStatuses.stream()
            .filter(b -> b.getUsedPercentage() >= 100).count();

        return InsightData.builder()
            .month(month)
            .totalIncome(totalIncome)
            .totalExpense(totalExpense)
            .savingsRate(savingsRate)
            .topExpenses(topExpenses)
            .expenseChangeFromLastMonth(expenseChange)
            .budgetExceededCount((int) exceededCount)
            .totalBudgetCount(budgetStatuses.size())
            .build();
    }

    /**
     * Format thành text compact để đưa vào prompt
     * Giữ ngắn → tiết kiệm token
     */
    public String formatForPrompt(InsightData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tháng ").append(data.getMonth()).append(":\n");
        sb.append("- Thu: ").append(formatVND(data.getTotalIncome())).append("\n");
        sb.append("- Chi: ").append(formatVND(data.getTotalExpense())).append("\n");
        sb.append("- Tiết kiệm: ").append(data.getSavingsRate()).append("%\n");

        if (data.getExpenseChangeFromLastMonth() != null) {
            String trend = data.getExpenseChangeFromLastMonth().compareTo(BigDecimal.ZERO) > 0
                ? "tăng " : "giảm ";
            sb.append("- So tháng trước: chi ").append(trend)
              .append(formatVND(data.getExpenseChangeFromLastMonth().abs())).append("\n");
        }

        sb.append("- Top chi tiêu:\n");
        data.getTopExpenses().forEach(c ->
            sb.append("  + ").append(c.getCategoryName()).append(": ")
              .append(formatVND(c.getAmount())).append("\n")
        );

        if (data.getTotalBudgetCount() > 0) {
            sb.append("- Budget: ").append(data.getBudgetExceededCount())
              .append("/").append(data.getTotalBudgetCount())
              .append(" danh mục vượt ngân sách\n");
        }

        return sb.toString();
    }

    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal expense) {
        if (income == null || income.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        return income.subtract(expense)
            .divide(income, 2, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private String formatVND(BigDecimal amount) {
        if (amount == null) return "0đ";
        return NumberFormat.getNumberInstance(new Locale("vi","VN"))
            .format(amount) + "đ";
    }
}
```

**Commit Day 1:**
```bash
git commit -m "feat: migrations V11-V13 (insights/health_score/anomalies), entities, InsightDataCollector"
```

---

## 📅 DAY 2 — Prompt Engineering + InsightService
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: Migrations, entities, InsightDataCollector xong
🎯 Hôm nay: Thiết kế prompt insight + InsightService với cache DB
🚧 Blocker: InsightDataCollector format ra text đúng chưa?
            Test với data thật ngay đầu ngày
```

> ⚠️ **Timebox: 3h cho prompt engineering** — Chấp nhận kết quả "đọc được và có ích", không cần hoàn hảo.

---

### ⏰ 09:00–12:00 | T07: Prompt Engineering Monthly Insight (3h)

**Bước 1 — Kiểm tra format data (30 phút):**

Gọi `insightDataCollector.formatForPrompt()` với data tháng hiện tại, print ra console:
```
Tháng 2026-05:
- Thu: 15.500.000đ
- Chi: 8.750.000đ
- Tiết kiệm: 43.5%
- So tháng trước: chi tăng 1.250.000đ
- Top chi tiêu:
  + Ăn uống: 3.200.000đ
  + Đi lại: 1.500.000đ
  + Mua sắm: 1.200.000đ
  + Giải trí: 800.000đ
  + Hóa đơn: 700.000đ
- Budget: 2/5 danh mục vượt ngân sách
```

Đếm token: ~100-150 tokens. Rất OK.

**Bước 2 — Test prompt versions (2h):**

**Prompt V1:**
```
System:
"""
Bạn là chuyên gia tư vấn tài chính cá nhân tại Việt Nam.
Dựa trên dữ liệu chi tiêu của người dùng, hãy viết một đoạn nhận xét
bằng tiếng Việt tự nhiên, thân thiện, ngắn gọn (3-5 câu).
Tập trung vào: điểm tốt, điểm cần cải thiện, và 1 lời khuyên cụ thể.
KHÔNG dùng bullet points, viết thành đoạn văn.
"""

User: "{formatted_data}"
```

**Prompt V2 — Có persona cụ thể:**
```
System:
"""
Bạn là trợ lý tài chính AI của ứng dụng Personal Finance,
nói chuyện thân thiện như một người bạn, không phán xét.
Phân tích dữ liệu chi tiêu tháng và viết nhận xét 3-5 câu bằng tiếng Việt.
Cấu trúc: [Tình hình chung] → [Điểm nổi bật] → [Gợi ý cụ thể].
Không dùng từ "tôi" hay "bạn", dùng ngôi thứ hai.
"""

User:
"""
Dữ liệu tháng {month}:
{formatted_data}

Hãy viết nhận xét tháng này.
"""
```

**Bước 3 — Test và chọn (30 phút):**

Test với 3 bộ data khác nhau (tháng tốt, tháng xấu, tháng trung bình) → Chọn prompt cho ra text tự nhiên nhất.

**Ví dụ output mong muốn:**
```
"Tháng 5 là một tháng khá ổn! Tỷ lệ tiết kiệm đạt 43.5%, cao hơn
mức khuyến nghị 20%. Tuy nhiên, chi tiêu ăn uống chiếm tới 36% tổng
chi — có thể thử nấu ăn tại nhà vài bữa để giảm khoản này. Đặc biệt,
2 danh mục đã vượt ngân sách: nên điều chỉnh lại kế hoạch tháng tới."
```

---

### ⏰ 12:00–14:00 | T08: InsightService — Generate + Cache (2h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final GeminiClient geminiClient;
    private final InsightDataCollector dataCollector;
    private final AiInsightRepository insightRepository;
    private final AiCallLogService logService;

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý tài chính AI, nói chuyện thân thiện như người bạn.
        Dựa vào dữ liệu chi tiêu, viết nhận xét 3-5 câu bằng tiếng Việt tự nhiên.
        Cấu trúc: tình hình chung → điểm nổi bật → gợi ý cụ thể.
        Không dùng bullet points. Không phán xét. Luôn tích cực và có ích.
        """;

    /**
     * Lấy insight tháng — từ DB cache hoặc generate mới từ Gemini
     */
    public InsightResponse getMonthlyInsight(Long userId, String month) {
        LocalDate monthDate = YearMonth.parse(month).atDay(1);

        // 1. Check DB cache — đã generate tháng này chưa?
        Optional<AiInsight> cached = insightRepository
            .findByUserIdAndMonthAndInsightType(userId, monthDate, "PERSONAL");

        if (cached.isPresent()) {
            log.debug("Insight cache hit: userId={}, month={}", userId, month);
            return InsightResponse.builder()
                .month(month)
                .content(cached.get().getContent())
                .createdAt(cached.get().getCreatedAt())
                .isFromCache(true)
                .build();
        }

        // 2. Thu thập data tháng
        InsightData data = dataCollector.collectForMonth(userId, month);

        // 3. Kiểm tra có đủ data không
        if (data.getTotalExpense().compareTo(BigDecimal.ZERO) == 0) {
            return InsightResponse.builder()
                .month(month)
                .content("Tháng này chưa có giao dịch nào được ghi lại.")
                .isFromCache(false)
                .build();
        }

        // 4. Gọi Gemini
        String formattedData = dataCollector.formatForPrompt(data);
        long start = System.currentTimeMillis();
        String aiContent = geminiClient.chat(
            SYSTEM_PROMPT,
            "Dữ liệu tháng " + month + ":\n" + formattedData
        );
        long latency = System.currentTimeMillis() - start;

        if (aiContent == null || aiContent.isBlank()) {
            logService.logFailure("MONTHLY_INSIGHT", "Gemini returned null");
            return InsightResponse.builder()
                .month(month)
                .content("Không thể tạo nhận xét lúc này. Vui lòng thử lại sau.")
                .isFromCache(false)
                .build();
        }

        // 5. Lưu vào DB (cache cho tháng này)
        AiInsight insight = AiInsight.builder()
            .userId(userId)
            .month(monthDate)
            .insightType("PERSONAL")
            .content(aiContent)
            .model("gemini-2.0-flash")
            .build();
        insightRepository.save(insight);

        logService.logSuccess("MONTHLY_INSIGHT", "gemini-2.0-flash", 0, latency);
        log.info("Generated new insight: userId={}, month={}", userId, month);

        return InsightResponse.builder()
            .month(month)
            .content(aiContent)
            .createdAt(LocalDateTime.now())
            .isFromCache(false)
            .build();
    }

    /**
     * Force regenerate — xóa cache cũ và generate lại
     */
    @Transactional
    public InsightResponse regenerateInsight(Long userId, String month) {
        LocalDate monthDate = YearMonth.parse(month).atDay(1);
        insightRepository.deleteByUserIdAndMonthAndInsightType(userId, monthDate, "PERSONAL");
        return getMonthlyInsight(userId, month);
    }
}
```

---

### ⏰ 14:30–15:30 | T09: Controller Insight API (1h)

```java
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    /**
     * GET /api/insights/monthly?month=2026-05
     * Nếu đã có trong DB → trả luôn. Nếu chưa → gọi Gemini.
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<InsightResponse>> getMonthlyInsight(
            @RequestParam(defaultValue = "") String month,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = securityUtils.getCurrentUserId(userDetails);
        if (month.isBlank()) {
            month = YearMonth.now().toString();
        }

        InsightResponse insight = insightService.getMonthlyInsight(userId, month);
        return ResponseEntity.ok(ApiResponse.success(insight));
    }

    /**
     * POST /api/insights/monthly/regenerate?month=2026-05
     * Xóa cache cũ, generate lại
     */
    @PostMapping("/monthly/regenerate")
    public ResponseEntity<ApiResponse<InsightResponse>> regenerate(
            @RequestParam(defaultValue = "") String month,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = securityUtils.getCurrentUserId(userDetails);
        if (month.isBlank()) month = YearMonth.now().toString();

        InsightResponse insight = insightService.regenerateInsight(userId, month);
        return ResponseEntity.ok(ApiResponse.success(insight));
    }
}
```

**Commit Day 2:**
```bash
git commit -m "feat: prompt engineering (monthly insight), InsightService with DB cache, Insight API"
```

---

## 📅 DAY 3 — Anomaly Detection
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Insight API xong + tested (check content có tự nhiên không)
🎯 Hôm nay: Rule-based anomaly detection + Gemini explain
🚧 Blocker: Content Gemini generate có đọc được cảm giác tự nhiên không?
            Nếu quá cứng nhắc → điều chỉnh prompt
```

---

### ⏰ 09:00–11:30 | T10: AnomalyDetector — Rule-Based (2.5h)

> **Triết lý:** Rule-based detect (100% tự làm, không tốn quota) → AI chỉ dùng để **giải thích bằng ngôn ngữ tự nhiên** cho từng anomaly được tìm ra.

**`AnomalyDetector` — 4 loại anomaly:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetector {

    private final TransactionRepository txnRepo;
    private final SpendingAnomalyRepository anomalyRepo;

    /**
     * Detect anomalies cho transaction mới vừa tạo
     * Gọi async sau khi transaction được save
     */
    @Async
    public void detectForTransaction(Transaction txn) {
        if (txn.getType() != TransactionType.EXPENSE
                || txn.getScope() != TransactionScope.PERSONAL) {
            return; // Chỉ detect EXPENSE PERSONAL
        }

        List<AnomalyDetectionResult> results = new ArrayList<>();

        // Rule 1: UNUSUAL_AMOUNT — Giao dịch > 3x trung bình category đó (3 tháng gần nhất)
        detectUnusualAmount(txn).ifPresent(results::add);

        // Rule 2: LARGE_SINGLE_TXN — Giao dịch đơn lẻ > 30% thu nhập tháng
        detectLargeTransaction(txn).ifPresent(results::add);

        // Rule 3: NEW_CATEGORY — Category chưa từng chi trong 3 tháng trước
        detectNewCategory(txn).ifPresent(results::add);

        // Lưu các anomaly tìm được (chưa có explanation)
        results.forEach(r -> saveAnomaly(txn, r));
    }

    private Optional<AnomalyDetectionResult> detectUnusualAmount(Transaction txn) {
        if (txn.getCategoryId() == null) return Optional.empty();

        // Trung bình amount của category này trong 3 tháng trước
        String threeMonthsAgo = YearMonth.now().minusMonths(3).toString();
        BigDecimal avg = txnRepo.avgAmountByUserCategoryAfterMonth(
            txn.getUserId(), txn.getCategoryId(), threeMonthsAgo);

        if (avg == null || avg.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        // Nếu giao dịch này > 3x trung bình → anomaly
        BigDecimal threshold = avg.multiply(BigDecimal.valueOf(3));
        if (txn.getAmount().compareTo(threshold) > 0) {
            BigDecimal ratio = txn.getAmount().divide(avg, 1, RoundingMode.HALF_UP);
            return Optional.of(new AnomalyDetectionResult(
                AnomalyType.UNUSUAL_AMOUNT,
                txn.getAmount().compareTo(avg.multiply(BigDecimal.valueOf(5))) > 0
                    ? Severity.HIGH : Severity.MEDIUM,
                "Giao dịch cao gấp " + ratio + "x trung bình của danh mục này"
            ));
        }
        return Optional.empty();
    }

    private Optional<AnomalyDetectionResult> detectLargeTransaction(Transaction txn) {
        // Thu nhập tháng này
        String currentMonth = txn.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal monthlyIncome = txnRepo.sumByUserTypeMonth(
            txn.getUserId(), "INCOME", currentMonth);

        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0)
            return Optional.empty();

        BigDecimal thirtyPercent = monthlyIncome.multiply(BigDecimal.valueOf(0.30));
        if (txn.getAmount().compareTo(thirtyPercent) > 0) {
            return Optional.of(new AnomalyDetectionResult(
                AnomalyType.LARGE_SINGLE_TXN,
                Severity.HIGH,
                "Giao dịch chiếm " +
                    txn.getAmount().divide(monthlyIncome, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).intValue()
                    + "% thu nhập tháng này"
            ));
        }
        return Optional.empty();
    }

    private Optional<AnomalyDetectionResult> detectNewCategory(Transaction txn) {
        if (txn.getCategoryId() == null) return Optional.empty();

        // Có từng chi category này trong 3 tháng qua không?
        String threeMonthsAgo = YearMonth.now().minusMonths(3).toString();
        boolean hasHistory = txnRepo.existsByUserCategoryAfterMonth(
            txn.getUserId(), txn.getCategoryId(), threeMonthsAgo);

        if (!hasHistory) {
            return Optional.of(new AnomalyDetectionResult(
                AnomalyType.NEW_CATEGORY,
                Severity.LOW,
                "Đây là lần đầu tiên chi tiêu danh mục này trong 3 tháng gần đây"
            ));
        }
        return Optional.empty();
    }

    private void saveAnomaly(Transaction txn, AnomalyDetectionResult result) {
        // Chưa có anomaly này cho transaction này chưa?
        if (anomalyRepo.existsByUserIdAndTransactionId(txn.getUserId(), txn.getId())) return;

        SpendingAnomaly anomaly = SpendingAnomaly.builder()
            .userId(txn.getUserId())
            .transactionId(txn.getId())
            .anomalyType(result.getType())
            .severity(result.getSeverity())
            .explanation(null) // Sẽ được điền bởi AnomalyExplainer async
            .build();
        anomalyRepo.save(anomaly);
        log.info("Anomaly detected: txnId={}, type={}, severity={}",
            txn.getId(), result.getType(), result.getSeverity());
    }
}
```

**Repositories cần thêm:**
```java
@Query("SELECT AVG(t.amount) FROM Transaction t " +
       "WHERE t.userId = :userId AND t.categoryId = :categoryId " +
       "AND t.type = 'EXPENSE' AND t.deletedAt IS NULL " +
       "AND DATE_FORMAT(t.transactionDate, '%Y-%m') >= :fromMonth")
BigDecimal avgAmountByUserCategoryAfterMonth(
    @Param("userId") Long userId,
    @Param("categoryId") Long categoryId,
    @Param("fromMonth") String fromMonth);

@Query("SELECT COUNT(t) > 0 FROM Transaction t " +
       "WHERE t.userId = :userId AND t.categoryId = :categoryId " +
       "AND t.type = 'EXPENSE' AND t.deletedAt IS NULL " +
       "AND DATE_FORMAT(t.transactionDate, '%Y-%m') >= :fromMonth")
boolean existsByUserCategoryAfterMonth(...);
```

**Gọi detect sau khi transaction tạo:**
```java
// Trong TransactionService.create() — sau save transaction
@Async
private void triggerAnomalyDetection(Transaction savedTxn) {
    try {
        anomalyDetector.detectForTransaction(savedTxn);
    } catch (Exception e) {
        log.warn("Anomaly detection failed for txnId={}: {}", savedTxn.getId(), e.getMessage());
        // Không crash transaction creation
    }
}
```

**✅ Done khi:**
- Tạo transaction chi 5tr (trong khi avg 500k) → anomaly UNUSUAL_AMOUNT xuất hiện trong DB
- Tạo transaction chi category chưa từng dùng → anomaly NEW_CATEGORY
- Detection fail → transaction vẫn OK

---

### ⏰ 11:30–13:00 | T11: AnomalyExplainer — Gemini Giải Thích (2h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyExplainer {

    private final GeminiClient geminiClient;
    private final SpendingAnomalyRepository anomalyRepo;
    private final TransactionRepository txnRepo;

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý tài chính AI. Dựa vào thông tin giao dịch bất thường,
        viết 1-2 câu giải thích thân thiện bằng tiếng Việt.
        Đừng phán xét. Có thể gợi ý nhẹ nhàng nếu phù hợp.
        """;

    /**
     * Điền explanation bằng Gemini cho anomaly chưa có giải thích
     * Gọi async sau khi detect
     */
    @Async
    public void explainAnomaly(SpendingAnomaly anomaly) {
        if (anomaly.getExplanation() != null) return;

        Transaction txn = txnRepo.findById(anomaly.getTransactionId()).orElse(null);
        if (txn == null) return;

        String userMessage = buildExplanationPrompt(anomaly, txn);
        String explanation = geminiClient.chat(SYSTEM_PROMPT, userMessage);

        if (explanation != null && !explanation.isBlank()) {
            anomaly.setExplanation(explanation);
            anomalyRepo.save(anomaly);
        }
    }

    private String buildExplanationPrompt(SpendingAnomaly anomaly, Transaction txn) {
        return switch (anomaly.getAnomalyType()) {
            case UNUSUAL_AMOUNT -> String.format(
                "Giao dịch '%s' số tiền %sđ cao bất thường so với lịch sử. %s.",
                txn.getDescription(), txn.getAmount().toPlainString(),
                anomaly.getExplanation() // technical detail từ rule
            );
            case LARGE_SINGLE_TXN -> String.format(
                "Giao dịch đơn lẻ '%s' số tiền %sđ. %s.",
                txn.getDescription(), txn.getAmount().toPlainString(),
                anomaly.getExplanation()
            );
            case NEW_CATEGORY -> String.format(
                "Chi tiêu danh mục mới '%s' lần đầu tiên. Số tiền: %sđ.",
                txn.getCategoryId(), txn.getAmount().toPlainString()
            );
            default -> "Phát hiện chi tiêu bất thường: " + anomaly.getExplanation();
        };
    }
}
```

**Gọi explainer sau khi detect:**
```java
// Trong AnomalyDetector.saveAnomaly() — sau save, gọi async explain
anomalyExplainer.explainAnomaly(savedAnomaly);
```

---

### ⏰ 14:00–14:30 | T12: GET /api/anomalies (30 phút)

```java
GET /api/anomalies?month=2026-05

Response:
{
  "data": [
    {
      "id": 1,
      "transactionId": 25,
      "description": "Mua iPhone 16 Pro",
      "amount": 30000000,
      "anomalyType": "LARGE_SINGLE_TXN",
      "severity": "HIGH",
      "explanation": "Khoản chi này chiếm đến 75% thu nhập tháng 5. Nếu đây là mua sắm kế hoạch, bạn có thể cân nhắc chia nhỏ các khoản lớn trong những tháng tiếp theo.",
      "detectedAt": "2026-05-15T10:30:00"
    }
  ]
}
```

**Thêm:** `POST /api/anomalies/{id}/dismiss` — User đã đọc, bỏ qua:
```java
anomaly.setIsDismissed(true);
anomalyRepo.save(anomaly);
// Trả 204 No Content
```

**Commit Day 3:**
```bash
git commit -m "feat: AnomalyDetector (4 rules), AnomalyExplainer (Gemini), Anomaly API with dismiss"
```

**📊 Check end of Day 3:**
```
T01–T03 ✅ Migrations V11-V13
T04     ✅ Entities + repositories
T05     ✅ DTOs
T06     ✅ InsightDataCollector
T07     ✅ Prompt engineering (monthly insight)
T08     ✅ InsightService + DB cache
T09     ✅ Insight API
T10     ✅ AnomalyDetector (rule-based, 4 loại)
T11     ✅ AnomalyExplainer (Gemini)
T12     ✅ Anomaly API + dismiss
```

---

*→ Tiếp theo: Day 4–6 trong sprint7_daily_p2.md*
