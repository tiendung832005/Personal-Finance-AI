# 🏃 Sprint 7 — Daily Breakdown (Day 4–6)
**Health Score + Family Insight + FE Integration**

---

## 📅 DAY 4 — Financial Health Score + Family Insight
**6 tiếng | Thứ Năm**

### 🌅 Daily Standup
```
✅ Hôm qua: Anomaly Detection (rule-based + Gemini explain) xong
🎯 Hôm nay: Health Score calculator + Family Insight API
🚧 Blocker: Thử tạo 1-2 transaction lớn → anomaly có detect không?
```

---

### ⏰ 09:00–11:30 | T13: FinancialHealthScoreCalculator (2.5h)

> **Triết lý:** Health Score = **thuần toán học, không dùng AI** → nhanh, deterministic, tái tính được. AI chỉ dùng trong Insight.

**Điểm tổng (0–100) = trọng số của 3 thành phần:**

| Thành phần | Trọng số | Ý nghĩa |
|-----------|---------|---------|
| Savings Score | 40% | Tỷ lệ tiết kiệm so với thu nhập |
| Budget Score | 35% | Tuân thủ budget đặt ra |
| Spending Trend | 25% | Chi tiêu tháng này so với trung bình 3 tháng |

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialHealthScoreCalculator {

    private final TransactionRepository txnRepo;
    private final BudgetRepository budgetRepo;
    private final FinancialHealthScoreRepository scoreRepo;

    public HealthScoreResponse calculateAndSave(Long userId, String month) {
        LocalDate monthDate = YearMonth.parse(month).atDay(1);

        // Check cache trong DB
        Optional<FinancialHealthScore> cached = scoreRepo.findByUserIdAndMonth(userId, monthDate);
        if (cached.isPresent()) {
            return mapToResponse(cached.get());
        }

        // Thu thập data
        BigDecimal totalIncome  = txnRepo.sumByUserTypeMonth(userId, "INCOME", month);
        BigDecimal totalExpense = txnRepo.sumByUserTypeMonth(userId, "EXPENSE", month);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        // Nếu không có income → không tính được score đáng tin cậy
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return HealthScoreResponse.builder()
                .month(month)
                .overallScore(0)
                .scoreLabel("INSUFFICIENT_DATA")
                .message("Chưa có dữ liệu thu nhập tháng này để tính điểm")
                .build();
        }

        // === Tính từng thành phần ===

        // 1. Savings Score (0-100)
        int savingsScore = calculateSavingsScore(totalIncome, totalExpense);

        // 2. Budget Score (0-100)
        int budgetScore = calculateBudgetScore(userId, month, monthDate);

        // 3. Spending Trend Score (0-100)
        int trendScore = calculateSpendingTrendScore(userId, month, totalExpense);

        // 4. Overall Score
        int overall = (int) Math.round(
            savingsScore * 0.40 +
            budgetScore  * 0.35 +
            trendScore   * 0.25
        );
        overall = Math.min(100, Math.max(0, overall));

        String label = getScoreLabel(overall);

        // Lưu vào DB
        FinancialHealthScore score = FinancialHealthScore.builder()
            .userId(userId)
            .month(monthDate)
            .overallScore(overall)
            .savingsScore(savingsScore)
            .budgetScore(budgetScore)
            .spendingTrendScore(trendScore)
            .scoreLabel(label)
            .build();
        scoreRepo.save(score);

        return mapToResponse(score);
    }

    /** Savings Score: dựa vào tỷ lệ tiết kiệm */
    private int calculateSavingsScore(BigDecimal income, BigDecimal expense) {
        BigDecimal savings = income.subtract(expense);
        double savingsRate = savings.divide(income, 4, RoundingMode.HALF_UP)
                                   .doubleValue();

        // Thang điểm:
        // ≥ 30% → 100 điểm (xuất sắc)
        // 20-30% → 75-99 điểm (tốt)
        // 10-20% → 50-74 điểm (trung bình)
        // 0-10%  → 25-49 điểm (yếu)
        // < 0%   → 0-24 điểm (âm = vay nợ)
        if (savingsRate >= 0.30) return 100;
        if (savingsRate >= 0.20) return (int)(75 + (savingsRate - 0.20) / 0.10 * 25);
        if (savingsRate >= 0.10) return (int)(50 + (savingsRate - 0.10) / 0.10 * 25);
        if (savingsRate >= 0.00) return (int)(savingsRate / 0.10 * 25);
        return Math.max(0, (int)(50 + savingsRate * 100)); // âm → 0-24
    }

    /** Budget Score: dựa vào % category không vượt budget */
    private int calculateBudgetScore(Long userId, String month, LocalDate monthDate) {
        List<Budget> budgets = budgetRepo.findByUserIdAndMonth(userId, monthDate);
        if (budgets.isEmpty()) return 70; // Chưa set budget → điểm trung bình

        long exceededCount = budgets.stream()
            .filter(b -> {
                BigDecimal actual = txnRepo.sumByUserCategoryTypeMonth(
                    userId, b.getCategoryId(), "EXPENSE", month);
                return actual != null && actual.compareTo(b.getAmount()) > 0;
            }).count();

        double complianceRate = 1.0 - (double) exceededCount / budgets.size();
        return (int)(complianceRate * 100);
    }

    /** Spending Trend Score: so sánh tháng này vs trung bình 3 tháng trước */
    private int calculateSpendingTrendScore(Long userId, String month,
                                             BigDecimal currentExpense) {
        // Trung bình chi 3 tháng trước
        List<BigDecimal> lastThreeMonths = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String m = YearMonth.parse(month).minusMonths(i).toString();
            BigDecimal e = txnRepo.sumByUserTypeMonth(userId, "EXPENSE", m);
            if (e != null && e.compareTo(BigDecimal.ZERO) > 0) lastThreeMonths.add(e);
        }

        if (lastThreeMonths.isEmpty()) return 75; // Không đủ lịch sử → điểm trung bình

        BigDecimal avg = lastThreeMonths.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(lastThreeMonths.size()), 2, RoundingMode.HALF_UP);

        double changeRate = currentExpense.subtract(avg)
            .divide(avg, 4, RoundingMode.HALF_UP).doubleValue();

        // Chi giảm → điểm cao. Chi tăng → điểm thấp.
        // -20% hoặc tốt hơn → 100 điểm
        // +50% hoặc tệ hơn → 0 điểm
        if (changeRate <= -0.20) return 100;
        if (changeRate >= 0.50)  return 0;
        // Linear interpolation giữa -20% và +50%
        return (int)((0.50 - changeRate) / 0.70 * 100);
    }

    private String getScoreLabel(int score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "FAIR";
        return "POOR";
    }
}
```

**Label + màu sắc + mô tả:**

| Label | Score | Màu | Mô tả hiển thị |
|-------|-------|-----|---------------|
| EXCELLENT | 80-100 | Xanh lá ✅ | "Tài chính rất lành mạnh!" |
| GOOD | 60-79 | Xanh dương 🔵 | "Tài chính ổn định" |
| FAIR | 40-59 | Vàng ⚠️ | "Cần chú ý hơn" |
| POOR | 0-39 | Đỏ 🔴 | "Cần cải thiện ngay" |

---

### ⏰ 11:30–12:30 | T14: GET /api/health-score?month= (1h)

```java
@GetMapping("/health-score")
public ResponseEntity<ApiResponse<HealthScoreResponse>> getHealthScore(
        @RequestParam(defaultValue = "") String month,
        @AuthenticationPrincipal UserDetails userDetails) {

    Long userId = securityUtils.getCurrentUserId(userDetails);
    if (month.isBlank()) month = YearMonth.now().toString();

    HealthScoreResponse score = healthScoreCalculator.calculateAndSave(userId, month);
    return ResponseEntity.ok(ApiResponse.success(score));
}
```

**Response mẫu:**
```json
{
  "month": "2026-05",
  "overallScore": 76,
  "scoreLabel": "GOOD",
  "savingsScore": 85,
  "budgetScore": 71,
  "spendingTrendScore": 68,
  "breakdown": [
    { "component": "Tiết kiệm",       "score": 85, "weight": "40%" },
    { "component": "Tuân thủ budget",  "score": 71, "weight": "35%" },
    { "component": "Xu hướng chi tiêu","score": 68, "weight": "25%" }
  ]
}
```

---

### ⏰ 13:30–15:30 | T15: Family Insight API (2h)

**Endpoint:** `GET /api/groups/{groupId}/insights/monthly?month=`

**Khác Personal Insight:** Data là shared transactions của group, prompt đề cập "cả nhóm".

```java
public InsightResponse getGroupInsight(Long groupId, String month, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId);
    LocalDate monthDate = YearMonth.parse(month).atDay(1);

    // Check cache
    Optional<AiInsight> cached = insightRepository
        .findByGroupIdAndMonthAndInsightType(groupId, monthDate, "FAMILY");
    if (cached.isPresent()) {
        return InsightResponse.fromCache(cached.get());
    }

    // Thu thập data group (shared transactions)
    InsightData data = dataCollector.collectGroupForMonth(groupId, month);

    // Prompt khác cho Family context
    String systemPrompt = """
        Bạn là trợ lý tài chính AI cho nhóm gia đình.
        Dựa vào dữ liệu chi tiêu chung của cả nhóm, viết nhận xét 3-5 câu bằng tiếng Việt.
        Đề cập đến đóng góp của các thành viên nếu có dữ liệu.
        Thân thiện, không phán xét ai, tích cực và có ích.
        """;

    String aiContent = geminiClient.chat(systemPrompt,
        "Dữ liệu nhóm tháng " + month + ":\n" + dataCollector.formatForPrompt(data));

    // Lưu cache
    if (aiContent != null) {
        AiInsight insight = AiInsight.builder()
            .groupId(groupId)
            .month(monthDate)
            .insightType("FAMILY")
            .content(aiContent)
            .model("gemini-2.0-flash")
            .build();
        insightRepository.save(insight);
    }

    return InsightResponse.builder()
        .month(month)
        .content(aiContent != null ? aiContent : "Không thể tạo nhận xét lúc này.")
        .isFromCache(false)
        .build();
}
```

**Commit Day 4:**
```bash
git commit -m "feat: HealthScoreCalculator (savings/budget/trend), Health Score API, Family Insight API"
```

---

## 📅 DAY 5 — Tests + Postman + FE Integration
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Health Score + Family Insight xong
🎯 Hôm nay: Unit tests, Postman, kết nối FE
🚧 Blocker: Health Score tính đúng không? Test thủ công với data biết trước
```

---

### ⏰ 09:00–11:00 | T16: Unit Tests (2h)

**InsightService tests:**
- `getInsight_cacheHit()` — đã có trong DB → không gọi Gemini
- `getInsight_cacheMiss_geminiSuccess()` → sinh nội dung, lưu DB
- `getInsight_noData()` → trả "Chưa có giao dịch" không crash
- `getInsight_geminiFail()` → trả fallback message, không crash

**AnomalyDetector tests:**
- `detectUnusualAmount_triggered()` → amount > 3x avg → anomaly UNUSUAL_AMOUNT
- `detectUnusualAmount_notTriggered()` → amount bình thường → không detect
- `detectLargeTransaction_triggered()` → >30% income → LARGE_SINGLE_TXN
- `detectNewCategory_triggered()` → category chưa dùng → NEW_CATEGORY
- `detectIncomeTxn_skipped()` → transaction INCOME → không detect

**HealthScoreCalculator tests:**
- `calculateScore_highSavings()` → savings 40% → savingsScore 100
- `calculateScore_negativeSavings()` → chi > thu → savingsScore thấp
- `calculateScore_noBudgets()` → budgetScore = 70 (default)
- `calculateScore_allBudgetsExceeded()` → budgetScore = 0
- `calculateScore_trendImproving()` → chi giảm 25% so với avg → trendScore cao
- `calculateScore_noIncome()` → trả INSUFFICIENT_DATA

---

### ⏰ 11:00–12:00 | T17: Postman Collection Sprint 7 (1h)

```
📁 AI Insights & Health (Sprint 7)
  │
  ├── GET /api/insights/monthly?month=2026-05
  │     Lần 1: source AI (mới generate)
  │     Lần 2: isFromCache: true
  │
  ├── POST /api/insights/monthly/regenerate?month=2026-05
  │     Expected: nội dung mới (khác lần trước)
  │
  ├── GET /api/health-score?month=2026-05
  │     Expected: overallScore 0-100, breakdown 3 thành phần
  │
  ├── GET /api/anomalies?month=2026-05
  │     Expected: list anomaly với explanation
  │
  ├── POST /api/anomalies/{id}/dismiss
  │     Expected: 204, anomaly biến mất khỏi list
  │
  └── GET /api/groups/{id}/insights/monthly?month=2026-05
        Expected: Family insight nội dung đề cập "nhóm"
```

**Postman tests:**
```javascript
// Health Score: verify range 0-100
pm.test("Score in valid range", () => {
    const score = pm.response.json().data.overallScore;
    pm.expect(score).to.be.at.least(0);
    pm.expect(score).to.be.at.most(100);
});

// Insight: verify content tiếng Việt
pm.test("Insight content not empty", () => {
    pm.expect(pm.response.json().data.content).to.not.be.empty;
});
```

---

### ⏰ 13:00–14:30 | T18: FE — Monthly Insight Card (1.5h)

**Component `InsightCard`:**
```jsx
const InsightCard = ({ userId, selectedMonth }) => {
    const [insight, setInsight] = useState(null);
    const [loading, setLoading] = useState(false);

    const loadInsight = async () => {
        setLoading(true);
        try {
            const res = await api.get(`/insights/monthly?month=${selectedMonth}`);
            setInsight(res.data.data);
        } catch {
            setInsight({ content: 'Không thể tải nhận xét lúc này.' });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadInsight(); }, [selectedMonth]);

    return (
        <div className="insight-card">
            <div className="insight-header">
                <span className="insight-icon">🤖</span>
                <h3>Nhận xét tháng {selectedMonth}</h3>
                {insight?.isFromCache && (
                    <span className="cache-badge">cached</span>
                )}
            </div>

            {loading ? (
                <div className="insight-loading">
                    <div className="spinner" />
                    <p>AI đang phân tích chi tiêu của bạn...</p>
                </div>
            ) : (
                <p className="insight-content">
                    {insight?.content}
                </p>
            )}

            <button
                className="regenerate-btn"
                onClick={async () => {
                    setLoading(true);
                    const res = await api.post(
                        `/insights/monthly/regenerate?month=${selectedMonth}`);
                    setInsight(res.data.data);
                    setLoading(false);
                }}
            >
                🔄 Tạo lại nhận xét
            </button>
        </div>
    );
};
```

**CSS:**
```css
.insight-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white; border-radius: 12px; padding: 20px;
}
.insight-content {
    font-size: 0.95rem; line-height: 1.7;
    font-style: italic; margin: 12px 0;
}
.regenerate-btn {
    background: rgba(255,255,255,0.2);
    border: 1px solid rgba(255,255,255,0.4);
    color: white; border-radius: 8px;
    padding: 6px 14px; cursor: pointer; font-size: 0.85rem;
}
```

---

### ⏰ 14:30–15:30 | T19: FE — Anomaly Alerts (1h)

```jsx
const AnomalyAlerts = ({ selectedMonth }) => {
    const [anomalies, setAnomalies] = useState([]);

    useEffect(() => {
        api.get(`/anomalies?month=${selectedMonth}`)
           .then(res => setAnomalies(res.data.data));
    }, [selectedMonth]);

    const dismiss = async (id) => {
        await api.post(`/anomalies/${id}/dismiss`);
        setAnomalies(prev => prev.filter(a => a.id !== id));
    };

    const severityConfig = {
        HIGH:   { icon: '🔴', label: 'Cao',    bg: '#fef2f2' },
        MEDIUM: { icon: '🟡', label: 'Trung',  bg: '#fffbeb' },
        LOW:    { icon: '🔵', label: 'Thấp',   bg: '#eff6ff' },
    };

    if (anomalies.length === 0) return null;

    return (
        <div className="anomaly-section">
            <h3>⚠️ Chi tiêu bất thường ({anomalies.length})</h3>
            {anomalies.map(a => {
                const config = severityConfig[a.severity];
                return (
                    <div key={a.id}
                         className="anomaly-item"
                         style={{ background: config.bg }}>
                        <div className="anomaly-header">
                            <span>{config.icon} {a.description}</span>
                            <span className="amount">{formatVND(a.amount)}</span>
                        </div>
                        <p className="anomaly-explain">{a.explanation}</p>
                        <button onClick={() => dismiss(a.id)}
                                className="dismiss-btn">
                            Đã hiểu ✓
                        </button>
                    </div>
                );
            })}
        </div>
    );
};
```

---

### ⏰ 15:30–16:00 | T20: FE — Health Score Gauge (30 phút)

```jsx
const HealthScoreGauge = ({ score, label }) => {
    const labelConfig = {
        EXCELLENT: { color: '#10b981', text: 'Xuất sắc' },
        GOOD:      { color: '#3b82f6', text: 'Tốt' },
        FAIR:      { color: '#f59e0b', text: 'Trung bình' },
        POOR:      { color: '#ef4444', text: 'Cần cải thiện' },
    };
    const config = labelConfig[label] || labelConfig.FAIR;

    return (
        <div className="health-score-card">
            <h3>Sức khỏe tài chính</h3>
            {/* Circular gauge đơn giản bằng CSS */}
            <div className="score-circle"
                 style={{ '--score': score, '--color': config.color }}>
                <span className="score-number">{score}</span>
                <span className="score-max">/100</span>
            </div>
            <span className="score-label" style={{ color: config.color }}>
                {config.text}
            </span>
        </div>
    );
};
```

```css
.score-circle {
    width: 120px; height: 120px;
    border-radius: 50%;
    background: conic-gradient(
        var(--color) calc(var(--score) * 1%),
        #e5e7eb calc(var(--score) * 1%)
    );
    display: flex; flex-direction: column;
    align-items: center; justify-content: center;
    margin: 16px auto;
}
.score-number { font-size: 2rem; font-weight: bold; }
```

**Commit Day 5:**
```bash
git commit -m "feat: unit tests GREEN, Postman Sprint 7, FE insight card + anomaly alerts + health gauge"
```

---

## 📅 DAY 6 — Bug Fix + Sprint Review
**6 tiếng | Thứ Bảy**

### 🌅 Daily Standup
```
✅ Hôm qua: Unit tests GREEN, FE kết nối xong
🎯 Hôm nay: Bug fix, integration test toàn flow, Sprint Review
🚧 Blocker: List tất cả bug từ Day 3-5
```

---

### ⏰ 09:00–11:30 | T21: Bug Fix + Integration Test (2.5h)

**Checklist toàn bộ flow:**
```
Insight:
1. GET /api/insights/monthly → nội dung tiếng Việt tự nhiên  ✅/❌
2. Call lần 2 → isFromCache: true, không gọi Gemini          ✅/❌
3. POST regenerate → nội dung mới được tạo                   ✅/❌
4. Tháng không có data → "Chưa có giao dịch", không crash    ✅/❌

Anomaly:
5. Tạo transaction 5tr (avg 500k) → UNUSUAL_AMOUNT detect   ✅/❌
6. Explanation được fill bởi Gemini (async, chờ 2-3s)        ✅/❌
7. GET /api/anomalies → hiện đúng anomaly                   ✅/❌
8. POST dismiss → anomaly biến mất khỏi list                ✅/❌

Health Score:
9. GET /api/health-score → điểm trong range 0-100            ✅/❌
10. Thay đổi data → score thay đổi sau khi xóa cache         ✅/❌
11. No income → INSUFFICIENT_DATA, không crash               ✅/❌

Family:
12. GET /api/groups/{id}/insights/monthly → Family insight   ✅/❌

FE:
13. InsightCard load và hiện content đúng                    ✅/❌
14. AnomalyAlerts hiện khi có anomaly, ẩn khi dismiss        ✅/❌
15. HealthScore gauge màu đúng theo label                    ✅/❌
```

**Common bugs:**

| Bug | Fix |
|-----|-----|
| Anomaly explanation null mãi | `@Async` cần `@EnableAsync` trong main class |
| Insight tạo lại mỗi request | Check DB cache trước khi gọi Gemini |
| Health Score < 0 hoặc > 100 | Thêm `Math.min(100, Math.max(0, score))` |
| Gauge CSS không render đúng | `calc(var(--score) * 1%)` phải đúng unit |
| Anomaly detect cho INCOME txn | Check `txn.getType() == EXPENSE` ngay đầu |

---

### ⏰ 11:30–12:30 | Sprint Review (1h)

**Deliverables Checklist:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | Monthly Insight API (Gemini + DB cache) | ⬜ |
| 2 | Insight content tiếng Việt tự nhiên | ⬜ |
| 3 | Anomaly Detection: UNUSUAL_AMOUNT | ⬜ |
| 4 | Anomaly Detection: LARGE_SINGLE_TXN | ⬜ |
| 5 | Anomaly Detection: NEW_CATEGORY | ⬜ |
| 6 | Anomaly Explanation bằng Gemini | ⬜ |
| 7 | Dismiss anomaly | ⬜ |
| 8 | Health Score 0-100 với 3 thành phần | ⬜ |
| 9 | Family Insight API | ⬜ |
| 10 | FE: Insight card + regenerate button | ⬜ |
| 11 | FE: Anomaly alerts + dismiss | ⬜ |
| 12 | FE: Health Score gauge + label | ⬜ |
| 13 | Unit tests GREEN | ⬜ |

**Demo Script (10 phút):**
```
Personal:
1. Dashboard → InsightCard: "Tháng 5 là một tháng..."     (2p)
2. Click Tạo lại → nội dung mới từ Gemini                (1p)
3. Health Score gauge: 76/100 - GOOD màu xanh            (1p)
4. Tạo transaction 5tr tiền mặt (avg 500k)               (1p)
5. Chờ 2s → GET /api/anomalies → UNUSUAL_AMOUNT          (1p)
6. Explanation: "Khoản chi này cao gấp 10x..."            (30s)
7. Dismiss anomaly → biến mất                            (30s)

Family:
8. Group insight → "Tháng 5, cả nhóm chi..."             (1p)
```

---

### ⏰ 12:30–13:00 | Sprint Retrospective (30 phút)

```
✅ Went WELL:
   Ví dụ: Rule-based anomaly detection không tốn quota,
   Gemini chỉ dùng để explain → tiết kiệm token

⚠️ Could IMPROVE:
   Ví dụ: Health Score cần test với nhiều edge case hơn
   (không có budget, không có lịch sử...)

🚀 Next Sprint:
   Ví dụ: Sprint 8 = Polish + Optimization + Demo
   Cần dọn dẹp code + document API + setup môi trường demo
```

**Final Commit + Tag:**
```bash
git commit -m "chore: integration bugs fixed, all flows tested, Sprint 7 complete"
git tag -a sprint-7 -m "Sprint 7: AI Insight + Anomaly + Health Score"
git push origin main --tags
```

---

## 📊 Sprint 7 Summary

### Thời Gian Phân Bổ
```
Day 1: Migrations + entities + InsightDataCollector  → 6h
Day 2: Prompt engineering + InsightService + DB cache → 6h  ← Prompt tự làm
Day 3: AnomalyDetector (rule-based) + Explainer      → 6h  ← Business logic nặng
Day 4: HealthScoreCalculator + Family Insight        → 6h  ← Math thuần, không AI
Day 5: Tests + Postman + FE integration              → 6h
Day 6: Bug fix + Review + Retro                      → 6h
──────────────────────────────────────────────────────────
Total:                                                36h
```

### Chiến Lược Tiết Kiệm Gemini Quota Sprint 7

| Feature | Cách tiếp cận | Quota dùng |
|---------|--------------|-----------|
| Monthly Insight | Generate 1 lần, cache DB | 1 call/user/tháng |
| Anomaly Explanation | Gọi khi detect, ngắn (1-2 câu) | ~5-10 calls/tháng |
| Health Score | Thuần toán học | 0 calls |
| Family Insight | 1 call/group/tháng | 1 call/group/tháng |

**Tổng quota ~tháng:** 10-20 calls/user → **chưa đến 1% free tier**

### Nếu Bị Trễ — Cắt Theo Thứ Tự
```
Giữ:   Insight API + Health Score (2 feature core)
Giảm:  Anomaly explanation (bỏ Gemini, chỉ hiện technical message)
Dời:   Family Insight (sang Sprint 8)
Bỏ:    AnomalyDetector NEW_CATEGORY (giữ UNUSUAL_AMOUNT + LARGE_TXN)
```

---

*Sprint 7 Complete → AI Insight + Anomaly + Health Score ✅ → Sprint 8: Polish, Performance & Final Demo* 🚀
