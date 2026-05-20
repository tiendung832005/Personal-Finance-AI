# 🏃 Sprint 6 (Gemini) — Daily Breakdown (Day 4–6)
**AI Categorization: Override + FE Integration + Tests**

---

## 📅 DAY 4 — User Override + Rate Limiting + Tests + Postman
**6 tiếng | Thứ Năm**

### 🌅 Daily Standup
```
✅ Hôm qua: CategorizationService hoàn chỉnh, tích hợp vào transaction
🎯 Hôm nay: User override → update cache, rate limiting, unit tests, Postman
🚧 Blocker: Test tạo transaction "Grab đi làm" → có tự classify "Đi lại" không?
```

---

### ⏰ 09:00–10:00 | T15: User Override → Update Cache (1h)

Khi user sửa category của transaction đã auto-categorize → cache học theo:

**Sửa `TransactionService.update()`:**
```java
@Transactional
public TransactionResponse update(Long id, UpdateTransactionRequest request,
                                   Long currentUserId) {
    Transaction txn = findOwnedTransaction(id, currentUserId);

    // Nếu user đổi category của giao dịch đã auto-categorize
    if (request.getCategoryId() != null
            && !request.getCategoryId().equals(txn.getCategoryId())
            && Boolean.TRUE.equals(txn.getIsAutoCategrized())) {

        // Cập nhật cache → lần sau sẽ trả đúng luôn
        if (txn.getDescription() != null) {
            cacheService.cacheResult(txn.getDescription(), request.getCategoryId());
            log.info("Cache updated from user override: '{}' → categoryId {}",
                txn.getDescription(), request.getCategoryId());
        }
        // Đánh dấu lại là manual
        txn.setIsAutoCategrized(false);
    }

    if (request.getCategoryId() != null)      txn.setCategoryId(request.getCategoryId());
    if (request.getAmount() != null)          txn.setAmount(request.getAmount());
    if (request.getDescription() != null)     txn.setDescription(request.getDescription());
    if (request.getTransactionDate() != null) txn.setTransactionDate(request.getTransactionDate());

    return mapToResponse(transactionRepository.save(txn));
}
```

**Hiệu quả:** Lần sau nhập lại cùng description → cache trả đúng, không gọi Gemini nữa.

---

### ⏰ 10:00–11:30 | T16: Rate Limiting cho AI Endpoints (1.5h)

**Lý do cần:** Dù Gemini free tier 1500 req/ngày, một vòng lặp FE bug vẫn có thể hết quota trong vài phút.

**Dùng Bucket4j:**
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

**`AiRateLimiter`:**
```java
@Component
public class AiRateLimiter {

    // Mỗi user: tối đa 30 AI calls / phút (cache hit không tính)
    private final Map<Long, Bucket> userBuckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                30, Refill.intervally(30, Duration.ofMinutes(1))))
            .build();
    }

    public boolean tryConsume(Long userId) {
        return userBuckets
            .computeIfAbsent(userId, id -> createBucket())
            .tryConsume(1);
    }
}
```

> **Note:** Rate limit chỉ check khi phải gọi Gemini thật (sau cache miss). Cache hit không bị giới hạn.

**✅ Done khi:** Gọi `/api/transactions/categorize` 31 lần liên tiếp → lần 31 trả lỗi rate limit.

---

### ⏰ 11:30–13:30 | T17: Unit Tests (2h)

```java
@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private CategorizationCacheService cacheService;
    @Mock private AiCallLogService logService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AiRateLimiter rateLimiter;

    @InjectMocks private CategorizationService service;

    @Test
    @DisplayName("Cache hit → không gọi Gemini")
    void categorize_cacheHit_noGeminiCall() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategoryId("grab đi làm"))
            .thenReturn(Optional.of(5L));

        CategorizationResult result = service.categorize("grab đi làm", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(ResultSource.CACHE, result.getSource());
        verify(geminiClient, never()).chat(any(), any());
    }

    @Test
    @DisplayName("Cache miss → gọi Gemini thành công → lưu cache")
    void categorize_cacheMiss_geminiSuccess() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategoryId(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(1L)).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Đi lại");
        when(categoryRepository.findByNameIgnoreCase("Đi lại"))
            .thenReturn(Optional.of(mockCategory(5L, "Đi lại")));

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(ResultSource.AI, result.getSource());
        assertEquals(5L, result.getCategoryId());
        verify(cacheService).cacheResult(any(), eq(5L));
    }

    @Test
    @DisplayName("Gemini trả null → FAILED, không crash")
    void categorize_geminiReturnsNull_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategoryId(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn(null);

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
        verify(cacheService, never()).cacheResult(any(), any());
    }

    @Test
    @DisplayName("Gemini trả category không biết → fallback Chi khác")
    void categorize_unknownCategory_fallbackToChiKhac() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategoryId(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Danh mục lạ XYZ");
        when(categoryRepository.findByNameIgnoreCase("Danh mục lạ XYZ"))
            .thenReturn(Optional.empty());
        when(categoryRepository.findByName("Chi khác"))
            .thenReturn(Optional.of(mockCategory(15L, "Chi khác")));

        CategorizationResult result = service.categorize("abc xyz", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(15L, result.getCategoryId());
    }

    @Test
    @DisplayName("Rate limit vượt → FAILED, không gọi Gemini")
    void categorize_rateLimitExceeded() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategoryId(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(1L)).thenReturn(false);

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        verify(geminiClient, never()).chat(any(), any());
    }

    @Test
    @DisplayName("Description quá ngắn → SKIPPED")
    void categorize_tooShort_skipped() {
        when(cacheService.isTooShort("a")).thenReturn(true);

        CategorizationResult result = service.categorize("a", 1L);

        assertEquals(ResultSource.SKIPPED, result.getSource());
        verify(geminiClient, never()).chat(any(), any());
    }

    private Category mockCategory(Long id, String name) {
        Category c = new Category();
        c.setId(id); c.setName(name);
        return c;
    }
}
```

**Cache tests:**
- `getCachedCategoryId_hit()` — hash match → trả id, tăng hitCount
- `getCachedCategoryId_miss()` → empty Optional
- `cacheResult_newEntry()` — lưu mới
- `cacheResult_overwrite()` — hash trùng → update (user override)

---

### ⏰ 14:30–15:30 | T18: Postman Collection Sprint 6 (1h)

```
📁 AI Features (Sprint 6)
  │
  ├── POST /api/transactions/categorize
  │     Body: { "description": "Grab đi làm" }
  │     Expected: { source: "AI", categoryName: "Đi lại" }
  │
  ├── POST /api/transactions/categorize (lần 2 cùng description)
  │     Expected: { source: "CACHE" } ← phải thấy CACHE
  │
  ├── POST /api/transactions/categorize
  │     Body: { "description": "a" }
  │     Expected: { source: "SKIPPED" }
  │
  ├── POST /api/transactions (description có, không có categoryId)
  │     Body: { description: "KFC", amount: 150000, type: "EXPENSE", ... }
  │     Expected: isAutoCategrized: true, categoryName: "Ăn uống"
  │
  ├── PUT /api/transactions/{id} (override category)
  │     Expected: isAutoCategrized: false sau khi sửa
  │
  └── GET /api/admin/ai-usage?from=...
        Expected: totalCalls, totalTokens
```

**Postman test tự động:**
```javascript
// Verify lần 2 dùng cache
pm.test("Second call uses CACHE", () => {
    pm.expect(pm.response.json().data.source).to.equal("CACHE");
});

// Verify auto-categorize trong transaction
pm.test("Transaction auto-categorized", () => {
    const data = pm.response.json().data;
    pm.expect(data.isAutoCategrized).to.be.true;
    pm.expect(data.categoryName).to.not.be.null;
});
```

**Commit Day 4:**
```bash
git commit -m "feat: user override + cache update, rate limiting (Bucket4j), unit tests GREEN, Postman Sprint 6"
```

---

## 📅 DAY 5 — FE Integration: AI Suggestion + Override
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Unit tests GREEN, rate limiting hoạt động
🎯 Hôm nay: FE — debounce AI suggestion, badge 🤖, user override flow
🚧 Blocker: Bug nào từ Day 3-4 chưa fix?
```

---

### ⏰ 09:00–11:00 | T19: FE — AI Suggestion Với Debounce (2h)

**UX flow:**
```
User nhập: "Grab đi làm"
→ 800ms sau khi ngừng gõ
→ Gọi POST /api/transactions/categorize
→ Dropdown category tự select "Đi lại"
→ Hiện badge "🤖 AI gợi ý: Đi lại"
```

```javascript
const [aiSuggestion, setAiSuggestion] = useState(null);
const [isLoadingAI, setIsLoadingAI] = useState(false);

// Debounce 800ms
useEffect(() => {
    if (!description || description.length < 3) {
        setAiSuggestion(null);
        return;
    }

    const timer = setTimeout(async () => {
        setIsLoadingAI(true);
        try {
            const res = await api.post('/transactions/categorize', { description });
            const suggestion = res.data.data;

            if (suggestion.isSuccessful) {
                setAiSuggestion(suggestion);
                // Auto-select chỉ khi user chưa tự chọn category
                if (!formData.categoryId) {
                    setFormData(prev => ({
                        ...prev,
                        categoryId: suggestion.categoryId
                    }));
                }
            } else {
                setAiSuggestion(null);
            }
        } catch {
            setAiSuggestion(null); // Lỗi AI → không hiện gì, form vẫn dùng được
        } finally {
            setIsLoadingAI(false);
        }
    }, 800);

    return () => clearTimeout(timer); // Clear khi description thay đổi
}, [description]);
```

**UI:**
```jsx
<div className="form-field">
    <label>Danh mục</label>
    <select
        value={formData.categoryId || ''}
        onChange={e => {
            setFormData(prev => ({ ...prev, categoryId: e.target.value }));
            setAiSuggestion(null); // User tự chọn → bỏ badge
        }}
    >
        <option value="">-- Chọn danh mục --</option>
        {categories.map(c => (
            <option key={c.id} value={c.id}>{c.name}</option>
        ))}
    </select>

    {isLoadingAI && (
        <span className="ai-badge loading">🤖 Đang phân loại...</span>
    )}
    {aiSuggestion && !isLoadingAI && (
        <span className="ai-badge">
            🤖 AI gợi ý: <strong>{aiSuggestion.categoryName}</strong>
            {aiSuggestion.source === 'CACHE' && (
                <span className="cache-tag"> (cached)</span>
            )}
        </span>
    )}
</div>
```

**CSS:**
```css
.ai-badge {
    font-size: 0.75rem;
    color: #6366f1;
    background: #ede9fe;
    padding: 2px 8px;
    border-radius: 12px;
    margin-top: 4px;
    display: inline-block;
}
.ai-badge.loading { color: #9ca3af; background: #f3f4f6; }
.cache-tag { color: #7c3aed; font-size: 0.7rem; }
```

---

### ⏰ 11:00–12:00 | T20: FE — Badge 🤖 Trong Transaction List (1h)

```jsx
{transactions.map(txn => (
    <div key={txn.id} className="txn-row">
        <div className="txn-info">
            <span className="txn-desc">{txn.description}</span>
            <span className="txn-category">
                {txn.categoryName}
                {txn.isAutoCategrized && (
                    <span
                        className="auto-badge"
                        title="Danh mục được AI tự động phân loại"
                    >
                        🤖
                    </span>
                )}
            </span>
        </div>
        <span className={`amount ${txn.type.toLowerCase()}`}>
            {txn.type === 'EXPENSE' ? '-' : '+'}
            {formatVND(txn.amount)}
        </span>
    </div>
))}
```

```css
.auto-badge {
    font-size: 0.7rem;
    margin-left: 4px;
    cursor: help;
    opacity: 0.7;
}
.auto-badge:hover { opacity: 1; }
```

---

### ⏰ 12:00–13:30 | T21: FE — User Override + Hint (1.5h)

**Trong Edit Transaction modal:**
```javascript
const [showOverrideHint, setShowOverrideHint] = useState(false);

const handleCategoryChange = (newCategoryId) => {
    // Nếu đang sửa transaction đã auto-categorize → hiện hint
    if (transaction?.isAutoCategrized
            && newCategoryId !== String(transaction.categoryId)) {
        setShowOverrideHint(true);
    } else {
        setShowOverrideHint(false);
    }
    setFormData(prev => ({ ...prev, categoryId: newCategoryId }));
};

const handleSubmit = async () => {
    await api.put(`/transactions/${transaction.id}`, formData);
    // BE tự cập nhật cache trong TransactionService.update()
    onSuccess();
};
```

**UI hint:**
```jsx
{showOverrideHint && (
    <div className="override-hint">
        💡 Thay đổi này sẽ giúp AI phân loại chính xác hơn cho lần sau.
    </div>
)}
```

```css
.override-hint {
    font-size: 0.8rem;
    color: #6366f1;
    background: #ede9fe;
    padding: 6px 10px;
    border-radius: 6px;
    margin-top: 6px;
}
```

---

### ⏰ 14:00–15:30 | T22: Integration Test + Quota Check (1.5h)

**Test flow đầy đủ:**
```
1.  Tạo transaction "Grab đi làm" không có categoryId
    → isAutoCategrized: true, categoryName: "Đi lại"         ✅/❌

2.  Tạo lại "Grab đi làm"
    → source: CACHE (không gọi Gemini)                       ✅/❌

3.  Edit transaction → đổi "Đi lại" → "Mua sắm"
    → isAutoCategrized: false, hint hiện                      ✅/❌

4.  Tạo "Grab đi làm" lần 3
    → source: CACHE, category: "Mua sắm" (AI đã học)         ✅/❌

5.  Nhập description "a" (quá ngắn)
    → Không có AI badge                                       ✅/❌

6.  Tắt internet → tạo transaction "Netflix"
    → Vẫn tạo được, categoryId = null, không crash           ✅/❌

7.  FE: nhập "KFC" → 800ms → dropdown auto-select "Ăn uống" ✅/❌

8.  Nhập "KFC" lần 2 → badge hiện "(cached)"                ✅/❌
```

**Quota check:**
```
GET /api/admin/ai-usage
→ Xem tổng calls đã dùng hôm nay
→ Free tier còn bao nhiêu (1500/ngày)
→ Đảm bảo cache đang hoạt động (hit rate cao)
```

**Commit Day 5:**
```bash
git commit -m "feat: FE AI suggestion debounce, auto-categorized badge, override + hint, integration tested"
```

---

## 📅 DAY 6 — Bug Fix + Edge Cases + Sprint Review
**6 tiếng | Thứ Bảy**

### 🌅 Daily Standup
```
✅ Hôm qua: FE AI flow hoàn chỉnh + integration tested
🎯 Hôm nay: Bug fix, edge cases, Sprint Review
🚧 Blocker: Liệt kê tất cả bug từ Day 3-5
```

---

### ⏰ 09:00–11:30 | T23: Bug Fix + Edge Cases (2.5h)

**Common bugs:**

| Bug | Fix |
|-----|-----|
| AI gọi mỗi keystroke | Đảm bảo `clearTimeout` trong `useEffect` return |
| Cache hash sai khi description có dấu câu | Normalize trước khi hash: lowercase + trim |
| `isAutoCategrized` không reset khi sửa | `txn.setIsAutoCategrized(false)` trong `update()` |
| AI badge hiện khi Gemini fail | Chỉ set `aiSuggestion` khi `isSuccessful === true` |
| Rate limit đếm cả cache hit | Rate limit check phải sau cache check |
| Transaction fail vì exception từ Gemini | Wrap toàn bộ AI block trong try-catch |
| Dropdown không reset sau submit | Clear `formData` + `aiSuggestion` trong `onSuccess` |

**Edge case checklist:**
- [ ] Description có emoji "🍜 ăn sáng" → normalize + hash đúng không?
- [ ] Description toàn số "12345" → Gemini classify ra gì? Có hợp lý không?
- [ ] Description > 200 chars → truncate trước khi gửi Gemini
- [ ] Gemini trả về response kèm thêm chữ "Category: Đi lại" → `.trim()` có đủ không? Cần strip thêm
- [ ] User đang chọn category rồi nhập description → không override lựa chọn của user

**Fix Gemini response có prefix thừa:**
```java
// Gemini đôi khi trả "Category: Đi lại" hoặc "Danh mục: Đi lại"
String cleaned = aiResponse.trim()
    .replaceAll("(?i)^(category|danh mục|phân loại)\\s*[:：]\\s*", "")
    .trim();
Optional<Category> matched = categoryRepository.findByNameIgnoreCase(cleaned);
```

---

### ⏰ 11:30–12:30 | Sprint Review (1h)

**Deliverables Checklist:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | `POST /api/transactions/categorize` với Gemini | ⬜ |
| 2 | Cache: lần 2 cùng description → source: CACHE | ⬜ |
| 3 | Auto-categorize Personal transaction | ⬜ |
| 4 | Auto-categorize Shared transaction | ⬜ |
| 5 | Transaction vẫn tạo được khi Gemini fail | ⬜ |
| 6 | User override → cache học theo | ⬜ |
| 7 | Rate limiting 30 calls/min/user | ⬜ |
| 8 | AI call logs ghi đầy đủ | ⬜ |
| 9 | FE: debounce suggestion 800ms | ⬜ |
| 10 | FE: badge 🤖 trong transaction list | ⬜ |
| 11 | FE: override hint message | ⬜ |
| 12 | Unit tests GREEN (6+ cases) | ⬜ |
| 13 | Quota free tier còn đủ dùng | ⬜ |

**Demo Script (8 phút):**
```
1. Mở form thêm giao dịch
2. Nhập "Grab đi làm" → 800ms → dropdown "Đi lại" + badge 🤖
3. Submit → transaction list hiện badge 🤖
4. Nhập "Grab đi làm" lần 2 → cực nhanh, badge "(cached)"
5. Edit → đổi category → hint 💡 hiện
6. Nhập lại "Grab đi làm" → category mới từ cache
7. Show GET /api/admin/ai-usage → calls, tokens
8. Tắt wifi → tạo transaction → vẫn OK (no crash)
```

---

### ⏰ 12:30–13:00 | Sprint Retrospective (30 phút)

```
✅ Went WELL:
   Ví dụ: Gemini free tier + cache → chi phí = 0đ
   trong toàn bộ Sprint 6

⚠️ Could IMPROVE:
   Ví dụ: Gemini đôi khi trả kèm prefix "Category: ..."
   → cần clean response tốt hơn

🚀 Next Sprint:
   Ví dụ: Sprint 7 AI Insight prompt phức tạp hơn,
   cần timebox 4h thay vì 3h
```

**Final Commit + Tag:**
```bash
git commit -m "chore: edge case fixes (Gemini response cleaning), bug fixes, Sprint 6 complete"
git tag -a sprint-6 -m "Sprint 6: AI Auto-Categorization (Gemini)"
git push origin main --tags
```

---

## 📊 Sprint 6 Summary

### Thời Gian Phân Bổ
```
Day 1: Gemini setup + migrations + GeminiClient   → 6h
Day 2: Prompt engineering + cache + log service   → 6h  ← Prompt: tự làm
Day 3: CategorizationService + integration + fallback → 6h  ← Core logic
Day 4: Override + rate limit + unit tests + Postman → 6h
Day 5: FE integration (suggestion + badge + override) → 6h
Day 6: Bug fix + edge cases + Sprint Review         → 6h
──────────────────────────────────────────────────────
Total:                                               36h
```

### Phần PHẢI tự làm (AI coding assistant không làm thay được)
- **Prompt engineering** — phải test với data tiếng Việt thực tế của mình
- **Clean Gemini response** — Gemini đôi khi trả thêm prefix, cần xử lý tùy response thực tế
- **Cache strategy** — exact match đủ dùng chưa hay cần fuzzy?

### Nếu Bị Trễ — Cắt Theo Thứ Tự
```
Giữ:   GeminiClient + CategorizationService + Cache + Fallback
Giảm:  Rate limiting (không implement, monitor quota thủ công)
Dời:   AI usage endpoint
Bỏ:    FE debounce suggestion (chỉ dùng POST /categorize thủ công)
```

---

*Sprint 6 Complete → AI Categorization ✅ → Sprint 7: AI Insight, Anomaly & Health Score* 🚀
