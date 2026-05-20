# 🏃 Sprint 6 (Gemini) — Daily Breakdown (Day 1–3)
**AI: Auto-Categorization với Google Gemini | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** Nhập mô tả giao dịch → Gemini AI tự gợi ý danh mục. Áp dụng cho cả personal và shared transaction. Không crash khi AI lỗi.

> ✅ **Ưu điểm Gemini so với OpenAI:**
> - Free tier **1500 requests/ngày** — không cần thẻ tín dụng
> - `gemini-2.0-flash` cực nhanh + hiểu tiếng Việt rất tốt
> - Không cần lo billing limit trong giai đoạn dev/demo

---

## Backlog Sprint 6

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Lấy Gemini API key từ Google AI Studio | 0.5h | Day 1 |
| T02 | Migration V9: bảng ai_categorization_cache | 0.5h | Day 1 |
| T03 | Migration V10: bảng ai_call_logs | 0.5h | Day 1 |
| T04 | Thêm dependency + cấu hình Gemini | 1h | Day 1 |
| T05 | GeminiClient wrapper service | 2h | Day 1 |
| T06 | Test GeminiClient kết nối thật | 1h | Day 1 |
| T07 | Prompt engineering: thiết kế + test accuracy | 3h | Day 2 |
| T08 | CategorizationCacheService | 1.5h | Day 2 |
| T09 | AiCallLogService | 1h | Day 2 |
| T10 | CategorizationService (cache → Gemini → log) | 2h | Day 3 |
| T11 | POST /api/transactions/categorize endpoint | 1h | Day 3 |
| T12 | Tích hợp auto-categorize vào Personal transaction | 1.5h | Day 3 |
| T13 | Tích hợp vào Shared transaction | 1h | Day 3 |
| T14 | Fallback handling (timeout, error, unknown category) | 1.5h | Day 3 |
| T15 | User override → update cache | 1h | Day 4 |
| T16 | Rate limiting cho AI endpoints | 1.5h | Day 4 |
| T17 | Unit tests CategorizationService | 2h | Day 4 |
| T18 | Postman collection Sprint 6 | 1h | Day 4 |
| T19 | FE: AI suggestion với debounce | 2h | Day 5 |
| T20 | FE: Badge 🤖 trong transaction list | 1h | Day 5 |
| T21 | FE: User override + hint message | 1.5h | Day 5 |
| T22 | Integration test + usage check | 1.5h | Day 5 |
| T23 | Bug fix + edge cases | 2.5h | Day 6 |
| T24 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Gemini Setup + Migrations + Client Wrapper
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 5 xong — Family MVP hoàn chỉnh
🎯 Hôm nay: Lấy Gemini API key, setup config, viết GeminiClient và test thật
🚧 Blocker: Cần có Google account để lấy API key
```

---

### ⏰ 09:00–09:30 | T01: Lấy Gemini API Key (30 phút)

**Checklist:**
- [ ] Vào [aistudio.google.com](https://aistudio.google.com)
- [ ] Đăng nhập Google account
- [ ] Click **"Get API Key"** → **"Create API key"**
- [ ] Copy API key → dán vào `.env`:
```
GEMINI_API_KEY=AIzaSy...
```
- [ ] Kiểm tra quota: Free tier = **1500 requests/ngày**, đủ dùng hoàn toàn cho dev + demo

> ✅ **Không cần thêm billing** cho free tier Gemini. Chỉ cần Google account.

---

### ⏰ 09:30–10:00 | T02+T03: Migrations (30 phút)

**`V9__create_ai_categorization_cache.sql`:**
```sql
CREATE TABLE ai_categorization_cache (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    description_hash VARCHAR(64) NOT NULL UNIQUE,
    category_id      BIGINT NOT NULL,
    hit_count        INT DEFAULT 1,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cache_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V10__create_ai_call_logs.sql`:**
```sql
CREATE TABLE ai_call_logs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_type         VARCHAR(50) NOT NULL,
    model             VARCHAR(50),
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    latency_ms        BIGINT,
    success           BOOLEAN NOT NULL,
    error_message     VARCHAR(500),
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**✅ Done khi:** App start, Flyway log `"Successfully applied 10 migrations"`

---

### ⏰ 10:00–11:00 | T04: Thêm Dependency + Config (1h)

**Thêm vào `pom.xml`:**
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

**`application.yml`:**
```yaml
app:
  gemini:
    api-key: ${GEMINI_API_KEY}
    base-url: https://generativelanguage.googleapis.com/v1beta
    model: gemini-2.0-flash
    timeout-seconds: 15
    max-retries: 2
```

**`GeminiProperties.java`:**
```java
@Configuration
@ConfigurationProperties(prefix = "app.gemini")
@Getter @Setter
public class GeminiProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private int timeoutSeconds;
    private int maxRetries;
}
```

Thêm `@EnableConfigurationProperties(GeminiProperties.class)` vào main class.

---

### ⏰ 11:00–13:00 | T05: GeminiClient Wrapper Service (2h)

> ⚠️ **Phải tự hiểu rõ.** Đây là integration point với external service — bug ở đây gây crash cả app.

**Gemini API format khác OpenAI:**
- URL: `{baseUrl}/models/{model}:generateContent?key={apiKey}`
- Request: `contents[]` + `systemInstruction`
- Response: `candidates[0].content.parts[0].text`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final GeminiProperties props;
    private final ObjectMapper objectMapper;
    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();
    }

    /**
     * Gửi request tới Gemini API
     * @return text response từ AI, hoặc null nếu lỗi
     */
    public String chat(String systemPrompt, String userMessage) {
        String url = props.getBaseUrl()
            + "/models/" + props.getModel()
            + ":generateContent?key=" + props.getApiKey();

        Map<String, Object> requestBody = Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
            ),
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
                )
            ),
            "generationConfig", Map.of(
                "temperature",     0.1,   // thấp → deterministic, tốt cho classify
                "maxOutputTokens", 50     // tên category ngắn, không cần nhiều
            )
        );

        for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                long startMs = System.currentTimeMillis();

                Request request = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    long latency = System.currentTimeMillis() - startMs;

                    // Rate limit → retry
                    if (response.code() == 429) {
                        log.warn("Gemini rate limited, attempt {}/{}", attempt, props.getMaxRetries());
                        Thread.sleep(1000L * attempt);
                        continue;
                    }

                    if (!response.isSuccessful()) {
                        log.error("Gemini error: {} {}", response.code(), response.message());
                        return null;
                    }

                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);

                    // Parse Gemini response
                    String text = node
                        .path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text")
                        .asText("").trim();

                    // Log token usage
                    int totalTokens = node.path("usageMetadata")
                                         .path("totalTokenCount").asInt();
                    log.info("Gemini OK: model={}, tokens={}, latency={}ms",
                        props.getModel(), totalTokens, latency);

                    return text;
                }

            } catch (IOException | InterruptedException e) {
                log.error("Gemini attempt {}/{} failed: {}",
                    attempt, props.getMaxRetries(), e.getMessage());
                if (attempt == props.getMaxRetries()) return null;
            }
        }
        return null;
    }
}
```

---

### ⏰ 14:00–15:00 | T06: Test GeminiClient Thật (1h)

Tạo quick test để đảm bảo kết nối OK:

```java
@SpringBootTest
class GeminiClientTest {

    @Autowired
    private GeminiClient geminiClient;

    @Test
    void testConnection() {
        String result = geminiClient.chat(
            "Bạn là assistant phân loại giao dịch tài chính.",
            "Phân loại giao dịch: 'Grab đi làm'"
        );
        System.out.println("Gemini response: " + result);
        assertNotNull(result, "Kết nối Gemini thất bại — kiểm tra API key");
    }
}
```

**Chạy test:** `mvn test -Dtest=GeminiClientTest`

**✅ Done khi:** Console hiện `"Gemini response: Đi lại"` (hoặc tương đương)

**Commit Day 1:**
```bash
git commit -m "feat: Gemini API setup, migrations V9-V10 (cache + logs), GeminiClient wrapper, connection verified"
```

---

## 📅 DAY 2 — Prompt Engineering + Cache + Log Service
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: GeminiClient kết nối thật OK
🎯 Hôm nay: Thiết kế prompt + test accuracy + cache service + log service
🚧 Blocker: GeminiClient test đã pass chưa?
```

> ⚠️ **Day 2 là ngày quan trọng nhất Sprint 6.**
> Prompt engineering **PHẢI tự làm** — AI coding assistant không thể làm thay.
> **Timebox:** Tối đa 3h. Đạt ≥80% accuracy là đủ.

---

### ⏰ 09:00–12:00 | T07: Prompt Engineering với Gemini (3h)

**Bước 1 — Chuẩn bị 30 test cases tiếng Việt (30 phút):**
```
"Grab đi làm"           → Đi lại
"KFC bữa tối"           → Ăn uống
"Lương tháng 5"         → Lương
"Netflix tháng"         → Giải trí
"Tiền điện tháng 5"     → Hóa đơn
"Mua áo H&M"            → Mua sắm
"Khám bệnh Vinmec"      → Sức khỏe
"Học phí Udemy"         → Giáo dục
"Thưởng dự án Q2"       → Thưởng
"Ăn sáng phở"           → Ăn uống
"Xe buýt"               → Đi lại
"Tiền nhà tháng 6"      → Nhà cửa
"Shopee flash sale"     → Mua sắm
"Thuốc cảm cúm"         → Sức khỏe
"Gửi tiết kiệm"         → Tiết kiệm
...
```

**Bước 2 — Test các prompt versions (2h):**

**Prompt V1 — Đơn giản:**
```
System:
"Bạn là assistant phân loại giao dịch tài chính cá nhân.
Chỉ trả về TÊN DANH MỤC, không giải thích thêm."

User: "Phân loại: 'Grab đi làm'"
```

**Prompt V2 — Danh sách category cố định (khuyến nghị cho Gemini):**
```
System:
"""
Bạn là assistant phân loại giao dịch tài chính cá nhân tại Việt Nam.
Chỉ trả về đúng một tên danh mục từ danh sách dưới đây.
KHÔNG thêm bất kỳ từ nào khác. KHÔNG giải thích.

Danh mục thu nhập: Lương, Thưởng, Thu nhập khác
Danh mục chi tiêu: Ăn uống, Đi lại, Mua sắm, Giải trí, Sức khỏe,
Giáo dục, Hóa đơn, Nhà cửa, Du lịch, Tiết kiệm, Đầu tư, Chi khác

Nếu không xác định được → trả về: Chi khác
"""

User: "Giao dịch: 'Grab đi làm'"
```

**Prompt V3 — Few-shot examples:**
```
System: (như V2)
User:
"""
Ví dụ:
Grab đi làm → Đi lại
KFC bữa tối → Ăn uống
Lương tháng 5 → Lương
Tiền điện → Hóa đơn

Phân loại: '{description}'
"""
```

> 💡 **Tip Gemini:** Gemini hiểu tiếng Việt rất tốt. V2 thường đạt 85–90% với data tiếng Việt thường gặp. Thêm few-shot (V3) nếu V2 chưa đạt 80%.

**Bước 3 — Đếm accuracy (30 phút):**
```
V1: ___ / 30 = ___%
V2: ___ / 30 = ___%
V3: ___ / 30 = ___%
→ Chọn version tốt nhất
```

**Lưu prompt đã chọn vào `docs/prompt_templates.md`**

---

### ⏰ 12:00–13:30 | T08: CategorizationCacheService (1.5h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationCacheService {

    private final CategorizationCacheRepository cacheRepository;
    private static final int MIN_DESC_LENGTH = 2;

    /** Check cache — trả category nếu có */
    public Optional<Long> getCachedCategoryId(String description) {
        String hash = hashDescription(description);
        return cacheRepository.findByDescriptionHash(hash)
            .map(cache -> {
                cache.setHitCount(cache.getHitCount() + 1);
                cacheRepository.save(cache);
                log.debug("Cache HIT: hash={}", hash);
                return cache.getCategoryId();
            });
    }

    /** Lưu kết quả vào cache (tạo mới hoặc overwrite) */
    public void cacheResult(String description, Long categoryId) {
        String hash = hashDescription(description);
        CategorizationCache cache = cacheRepository
            .findByDescriptionHash(hash)
            .orElse(new CategorizationCache());
        cache.setDescriptionHash(hash);
        cache.setCategoryId(categoryId);
        cache.setHitCount(1);
        cacheRepository.save(cache);
        log.debug("Cached categoryId={} for hash={}", categoryId, hash);
    }

    /** Normalize + SHA-256 hash */
    private String hashDescription(String description) {
        String normalized = description.toLowerCase().trim()
            .replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTooShort(String description) {
        return description == null || description.trim().length() < MIN_DESC_LENGTH;
    }
}
```

**Repository:**
```java
public interface CategorizationCacheRepository
        extends JpaRepository<CategorizationCache, Long> {
    Optional<CategorizationCache> findByDescriptionHash(String hash);
}
```

---

### ⏰ 14:30–15:30 | T09: AiCallLogService (1h)

```java
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    private final AiCallLogRepository logRepository;

    public void logSuccess(String callType, String model,
                           int totalTokens, long latencyMs) {
        logRepository.save(AiCallLog.builder()
            .callType(callType).model(model)
            .totalTokens(totalTokens).latencyMs(latencyMs)
            .success(true).build());
    }

    public void logFailure(String callType, String errorMessage) {
        logRepository.save(AiCallLog.builder()
            .callType(callType).success(false)
            .errorMessage(errorMessage).latencyMs(0L).build());
    }
}
```

**Endpoint xem usage (để kiểm soát quota):**
```
GET /api/admin/ai-usage?from=2026-05-01&to=2026-05-31
→ { totalCalls, successRate, totalTokens, remainingFreeQuota }
```

**Commit Day 2:**
```bash
git commit -m "feat: prompt engineering finalized (Gemini), cache service, AI call log service"
```

---

## 📅 DAY 3 — CategorizationService + Integration + Fallback
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Prompt Gemini đạt ≥80% accuracy, cache service OK
🎯 Hôm nay: Orchestrate toàn bộ flow + tích hợp transaction + fallback
🚧 Blocker: Prompt đã lưu vào docs/prompt_templates.md chưa?
```

---

### ⏰ 09:00–11:00 | T10: CategorizationService (2h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationService {

    private final GeminiClient geminiClient;
    private final CategorizationCacheService cacheService;
    private final AiCallLogService logService;
    private final CategoryRepository categoryRepository;
    private final AiRateLimiter rateLimiter;

    // Prompt đã test và chọn từ Day 2
    private static final String SYSTEM_PROMPT = """
        Bạn là assistant phân loại giao dịch tài chính cá nhân tại Việt Nam.
        Chỉ trả về đúng một tên danh mục từ danh sách dưới đây.
        KHÔNG thêm bất kỳ từ nào khác. KHÔNG giải thích.

        Danh mục thu nhập: Lương, Thưởng, Thu nhập khác
        Danh mục chi tiêu: Ăn uống, Đi lại, Mua sắm, Giải trí, Sức khỏe,
        Giáo dục, Hóa đơn, Nhà cửa, Du lịch, Tiết kiệm, Đầu tư, Chi khác

        Nếu không xác định được → trả về: Chi khác
        """;

    public CategorizationResult categorize(String description, Long userId) {

        // 0. Description quá ngắn → skip
        if (cacheService.isTooShort(description)) {
            return CategorizationResult.skipped("Description quá ngắn");
        }

        // 1. Check cache trước
        Optional<Long> cached = cacheService.getCachedCategoryId(description);
        if (cached.isPresent()) {
            return CategorizationResult.fromCache(cached.get());
        }

        // 2. Rate limit (chỉ áp dụng khi phải gọi AI thật)
        if (!rateLimiter.tryConsume(userId)) {
            log.warn("Rate limit exceeded for userId={}", userId);
            return CategorizationResult.failed("Quá nhiều yêu cầu, thử lại sau");
        }

        // 3. Gọi Gemini
        long start = System.currentTimeMillis();
        String aiResponse = geminiClient.chat(
            SYSTEM_PROMPT,
            "Giao dịch: \"" + truncate(description, 200) + "\""
        );
        long latency = System.currentTimeMillis() - start;

        // 4. Gemini không phản hồi
        if (aiResponse == null || aiResponse.isBlank()) {
            logService.logFailure("CATEGORIZATION", "Gemini returned null/empty");
            return CategorizationResult.failed("AI không phản hồi");
        }

        // 5. Map tên category → category ID
        Optional<Category> matched = categoryRepository
            .findByNameIgnoreCase(aiResponse.trim());

        if (matched.isEmpty()) {
            log.warn("Gemini returned unknown category: '{}'", aiResponse);
            // Fallback: dùng "Chi khác"
            matched = categoryRepository.findByName("Chi khác");
            if (matched.isEmpty()) {
                logService.logFailure("CATEGORIZATION", "Cannot map: " + aiResponse);
                return CategorizationResult.failed("Không map được danh mục");
            }
        }

        Long categoryId = matched.get().getId();

        // 6. Lưu cache
        cacheService.cacheResult(description, categoryId);

        // 7. Log AI call
        logService.logSuccess("CATEGORIZATION", "gemini-2.0-flash", 0, latency);

        return CategorizationResult.fromAI(categoryId, matched.get().getName());
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
```

**`CategorizationResult`:**
```java
@Getter @Builder
public class CategorizationResult {
    private Long categoryId;
    private String suggestedCategoryName;
    private ResultSource source; // CACHE, AI, FAILED, SKIPPED
    private String message;

    public boolean isSuccessful() { return categoryId != null; }

    public static CategorizationResult fromCache(Long id) {
        return builder().categoryId(id).source(ResultSource.CACHE).build();
    }
    public static CategorizationResult fromAI(Long id, String name) {
        return builder().categoryId(id).suggestedCategoryName(name)
            .source(ResultSource.AI).build();
    }
    public static CategorizationResult failed(String msg) {
        return builder().source(ResultSource.FAILED).message(msg).build();
    }
    public static CategorizationResult skipped(String msg) {
        return builder().source(ResultSource.SKIPPED).message(msg).build();
    }
}
```

---

### ⏰ 11:00–12:00 | T11: POST /api/transactions/categorize (1h)

```java
@PostMapping("/categorize")
public ResponseEntity<ApiResponse<CategorizationResultDTO>> categorize(
        @Valid @RequestBody CategorizeRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {

    Long userId = securityUtils.getCurrentUserId(userDetails);
    CategorizationResult result = categorizationService
        .categorize(request.getDescription(), userId);

    return ResponseEntity.ok(ApiResponse.success(
        CategorizationResultDTO.builder()
            .categoryId(result.getCategoryId())
            .categoryName(result.getSuggestedCategoryName())
            .source(result.getSource() != null ? result.getSource().name() : null)
            .isSuccessful(result.isSuccessful())
            .build()
    ));
}
```

**Test ngay:** `POST /api/transactions/categorize` body `{"description":"Grab đi làm"}`
→ Phải nhận `{"categoryName":"Đi lại","source":"AI"}`

---

### ⏰ 12:00–13:30 | T12+T13: Tích Hợp Vào Transaction (2.5h)

**Sửa `TransactionService.create()` — Personal:**
```java
@Transactional
public TransactionResponse create(CreateTransactionRequest request, Long currentUserId) {
    // ... validation như cũ ...

    Long finalCategoryId = request.getCategoryId();
    boolean autoCategrized = false;

    // Auto-categorize nếu không có categoryId
    if (finalCategoryId == null && request.getDescription() != null) {
        try {
            CategorizationResult result = categorizationService
                .categorize(request.getDescription(), currentUserId);
            if (result.isSuccessful()) {
                finalCategoryId = result.getCategoryId();
                autoCategrized = true;
            }
        } catch (Exception e) {
            // AI lỗi → transaction vẫn tạo được, không crash
            log.warn("Auto-categorize failed, creating transaction without category: {}",
                e.getMessage());
        }
    }

    Transaction txn = Transaction.builder()
        .categoryId(finalCategoryId)       // null nếu AI fail
        .isAutoCategrized(autoCategrized)
        // ... các field khác ...
        .build();

    return mapToResponse(transactionRepository.save(txn));
}
```

**Tương tự cho Shared Transaction** — cùng logic, thêm `groupId` + `scope = SHARED`.

**Quy tắc bất di bất dịch:** AI fail → transaction vẫn tạo được. `categoryId` có thể null.

---

### ⏰ 14:30–16:00 | T14: Fallback Handling (1.5h)

**Các tình huống fallback:**

| Tình huống | Xử lý |
|-----------|-------|
| Gemini timeout (>15s) | `IOException` catch → return `null` → `FAILED` |
| HTTP 429 rate limit | Retry với sleep 1s, 2s → nếu vẫn fail → `null` |
| HTTP 500 server error | Log, không retry, return `null` |
| Response là tên category không có trong DB | Map sang "Chi khác" |
| Response là empty string | return `FAILED` |
| Description < 2 ký tự | `SKIPPED`, không gọi AI |
| Description > 200 chars | Truncate trước khi gửi |
| Network không có internet | `IOException` → `FAILED` |

**Test fallback thủ công:**
```
1. Tắt wifi → POST /api/transactions body không có categoryId
   → Transaction vẫn tạo được, isAutoCategrized = false ✅

2. Nhập description "a" (1 ký tự)
   → source: SKIPPED, transaction tạo OK ✅
```

**Commit Day 3:**
```bash
git commit -m "feat: CategorizationService (Gemini cache→AI→fallback), auto-categorize in transactions, robust error handling"
```

**📊 End of Day 3 Check:**
```
T01 ✅ Gemini API key (Google AI Studio)
T02 ✅ Migration ai_categorization_cache
T03 ✅ Migration ai_call_logs
T04 ✅ GeminiProperties + config
T05 ✅ GeminiClient wrapper
T06 ✅ Connection test OK
T07 ✅ Prompt finalized ≥80% accuracy
T08 ✅ CategorizationCacheService
T09 ✅ AiCallLogService
T10 ✅ CategorizationService (orchestrator)
T11 ✅ POST /api/transactions/categorize
T12 ✅ Auto-categorize Personal transaction
T13 ✅ Auto-categorize Shared transaction
T14 ✅ Fallback handling đầy đủ
```

---

*→ Tiếp theo: Day 4–6 trong sprint6_v2_p2.md*
