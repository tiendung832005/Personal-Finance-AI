# 🏃 Sprint 10 — Bill / Receipt Scanner
**Gemini Vision API | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** User chụp ảnh hoá đơn → Gemini Vision đọc tự động → form được điền sẵn → 1 click lưu giao dịch.

> **Stack mới trong Sprint này:**
> - **Gemini Vision** (`gemini-2.0-flash` hỗ trợ multimodal — cùng model đang dùng, không cần key mới)
> - **Multipart File Upload** (Spring Boot `MultipartFile`)
> - **Base64 encoding** để gửi ảnh lên Gemini API
>
> **Không cần lưu ảnh vào server** → xử lý xong bỏ đi → tiết kiệm storage, bảo mật hơn

---

## DB Schema + Luồng Tổng Quát

```
User upload ảnh
      ↓
BE: Validate (type/size/rate limit)
      ↓
Convert → Base64 → Gemini Vision API
      ↓
Parse JSON response → BillScanResult DTO
      ↓
Trả về FE (KHÔNG lưu transaction)
      ↓
FE hiển thị form đã điền sẵn
      ↓
User confirm → POST /api/transactions (Sprint 2)
```

---

## Backlog Sprint 10

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Migration V17: bảng bill_scan_logs | 0.5h | Day 1 |
| T02 | Entity BillScanLog + Repository | 1h | Day 1 |
| T03 | DTO: BillScanRequest, BillScanResult | 1h | Day 1 |
| T04 | FileValidationService: type + size + HEIC | 1.5h | Day 1 |
| T05 | ScanRateLimiter: 20 lần/ngày/user | 1.5h | Day 1 |
| T06 | Extend GeminiClient: hỗ trợ multimodal (ảnh + text) | 2h | Day 2 |
| T07 | Prompt engineering: receipt extraction | 3h | Day 2 |
| T08 | BillScannerService: orchestrate toàn bộ flow | 2h | Day 3 |
| T09 | Handle edge cases: mờ, sai ảnh, không có số tiền | 2h | Day 3 |
| T10 | POST /api/scanner/scan endpoint | 1h | Day 3 |
| T11 | POST /api/scanner/scan-history | 0.5h | Day 3 |
| T12 | Unit tests BillScannerService | 2h | Day 4 |
| T13 | Unit tests FileValidationService | 1h | Day 4 |
| T14 | Postman collection Sprint 10 | 1h | Day 4 |
| T15 | Config: multipart maxFileSize, timeout | 0.5h | Day 4 |
| T16 | FE: Upload Component (drag-drop + camera + gallery) | 2h | Day 5 |
| T17 | FE: Scanning Animation (bước 1-4) | 1.5h | Day 5 |
| T18 | FE: Pre-filled form + confidence badge | 2h | Day 5 |
| T19 | FE: Error states + retry | 0.5h | Day 5 |
| T20 | Integration test toàn bộ flow | 2h | Day 6 |
| T21 | Bug fix + edge cases | 2.5h | Day 6 |
| T22 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Migration + DTO + File Validation + Rate Limiter
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 9 xong — Goal Planning hoàn chỉnh
🎯 Hôm nay: Setup DB, validate file, rate limiter cho scanner
🚧 Blocker: Gemini free tier có hỗ trợ Vision không?
            → Có. gemini-2.0-flash hỗ trợ multimodal sẵn.
```

---

### ⏰ 09:00–09:30 | T01: Migration V17 (30 phút)

**`V17__create_bill_scan_logs.sql`:**
```sql
CREATE TABLE bill_scan_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    scan_date       DATE NOT NULL,               -- để rate limit theo ngày
    file_name       VARCHAR(255),
    file_size_bytes BIGINT,
    mime_type       VARCHAR(50),
    scan_success    BOOLEAN NOT NULL DEFAULT FALSE,
    confidence      DECIMAL(3,2),                -- 0.00 – 1.00
    extracted_amount DECIMAL(15,2),
    error_type      VARCHAR(50),                 -- NOT_A_RECEIPT, LOW_CONFIDENCE, TIMEOUT
    latency_ms      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scan_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_scan_user_date (user_id, scan_date)   -- index cho rate limit query
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> **Lưu ý:** Không lưu ảnh vào DB hoặc filesystem. `bill_scan_logs` chỉ lưu metadata để rate limit và analytics. Ảnh bị discard ngay sau khi gửi Gemini.

---

### ⏰ 09:30–10:30 | T02+T03: Entity + DTOs (1h)

**Entity `BillScanLog`:** id, userId, scanDate, fileName, fileSizeBytes, mimeType, scanSuccess, confidence, extractedAmount, errorType, latencyMs, createdAt

**`BillScanResult` — Response trả về FE:**
```java
@Getter @Builder
public class BillScanResult {
    // Kết quả trích xuất
    private BigDecimal amount;
    private String description;         // tên cửa hàng / mô tả
    private LocalDate transactionDate;
    private Long categoryId;
    private String categoryName;
    private Double confidence;          // 0.0 – 1.0

    // Meta
    private ScanStatus status;          // SUCCESS, LOW_CONFIDENCE, NOT_A_RECEIPT,
                                        // AMOUNT_MISSING, TIMEOUT, ERROR
    private String warningMessage;      // hiển thị cho user nếu có

    // Flags để FE biết field nào cần user verify
    private boolean amountNeedsVerification;
    private boolean dateNeedsVerification;
    private boolean categoryNeedsVerification;
}
```

**Enum `ScanStatus`:**
```java
public enum ScanStatus {
    SUCCESS,            // Đọc đủ, confidence >= 0.7
    LOW_CONFIDENCE,     // Đọc được nhưng confidence < 0.7 → cảnh báo vàng
    AMOUNT_MISSING,     // Không đọc được số tiền → bắt buộc nhập tay
    NOT_A_RECEIPT,      // Ảnh không phải hoá đơn
    TIMEOUT,            // Gemini không phản hồi trong 15s
    ERROR               // Lỗi khác
}
```

**`ScanRequest`:** Nhận qua `@RequestParam MultipartFile image`

---

### ⏰ 10:30–12:00 | T04: FileValidationService (1.5h)

```java
@Service
@Slf4j
public class FileValidationService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/heic", "image/heif"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ảnh hoá đơn");
        }

        // Check size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                "File quá lớn. Tối đa 10MB, file của bạn: "
                + String.format("%.1f", file.getSize() / 1024.0 / 1024.0) + "MB");
        }

        // Check MIME type
        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(
                "Chỉ chấp nhận ảnh JPG, PNG, HEIC. File của bạn: " + mimeType);
        }
    }

    /** Detect MIME từ magic bytes — không tin vào file.getContentType() từ client */
    public String detectMimeType(MultipartFile file) {
        try {
            byte[] header = Arrays.copyOf(file.getBytes(), 12);

            // JPEG: FF D8 FF
            if (header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF)
                return "image/jpeg";

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (header[0] == (byte)0x89 && header[1] == 0x50
                    && header[2] == 0x4E && header[3] == 0x47)
                return "image/png";

            // HEIC/HEIF: check ftyp box
            String headerStr = new String(header, StandardCharsets.ISO_8859_1);
            if (headerStr.contains("ftyp")) return "image/heic";

            // Fallback: dùng tika/probeContentType nếu cần
            return file.getContentType() != null
                ? file.getContentType() : "unknown";

        } catch (IOException e) {
            throw new BusinessException("Không thể đọc file ảnh");
        }
    }

    /** Convert file sang base64 để gửi Gemini */
    public String toBase64(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("Lỗi xử lý file ảnh");
        }
    }
}
```

---

### ⏰ 13:00–14:30 | T05: ScanRateLimiter (1.5h)

```java
@Service
@RequiredArgsConstructor
public class ScanRateLimiter {

    private final BillScanLogRepository scanLogRepository;
    private static final int MAX_SCANS_PER_DAY = 20;

    /**
     * Check rate limit — dùng DB (không dùng in-memory vì cần persist qua restart)
     */
    public void checkAndConsume(Long userId) {
        LocalDate today = LocalDate.now();
        int todayCount = scanLogRepository.countByUserIdAndScanDate(userId, today);

        if (todayCount >= MAX_SCANS_PER_DAY) {
            throw new RateLimitException(
                "Đã đạt giới hạn " + MAX_SCANS_PER_DAY + " lần scan/ngày. "
                + "Hạn mức reset lúc 00:00 ngày mai.");
        }
    }

    public int getRemainingScans(Long userId) {
        int used = scanLogRepository.countByUserIdAndScanDate(userId, LocalDate.now());
        return Math.max(0, MAX_SCANS_PER_DAY - used);
    }
}
```

**Repository query:**
```java
@Query("SELECT COUNT(l) FROM BillScanLog l WHERE l.userId = :userId AND l.scanDate = :date")
int countByUserIdAndScanDate(@Param("userId") Long userId, @Param("date") LocalDate date);
```

**`application.yml` — multipart config:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
      enabled: true
```

**Commit Day 1:**
```bash
git commit -m "feat: migration V17 (bill_scan_logs), BillScanResult DTO, FileValidationService, ScanRateLimiter"
```

---

## 📅 DAY 2 — Extend GeminiClient Vision + Prompt Engineering
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: Migration, DTO, file validation, rate limiter xong
🎯 Hôm nay: GeminiClient hỗ trợ ảnh + thiết kế prompt + test thật
🚧 Blocker: Cần có vài ảnh hoá đơn thật để test
            (bill Grab, bill cafe, bill siêu thị)
```

---

### ⏰ 09:00–11:00 | T06: Extend GeminiClient — Multimodal (2h)

**Gemini Vision request format** — thêm `inline_data` vào `parts`:
```json
{
  "systemInstruction": { "parts": [{ "text": "system prompt" }] },
  "contents": [{
    "parts": [
      { "text": "user message" },
      {
        "inline_data": {
          "mime_type": "image/jpeg",
          "data": "<base64 string>"
        }
      }
    ]
  }],
  "generationConfig": {
    "temperature": 0.1,
    "maxOutputTokens": 300,
    "responseMimeType": "application/json"
  }
}
```

> 💡 `responseMimeType: "application/json"` — yêu cầu Gemini trả về **JSON thuần**, không có markdown fence. Quan trọng để parse dễ.

**Thêm method `chatWithImage()` vào `GeminiClient`:**
```java
/**
 * Chat với Gemini Vision — gửi kèm ảnh
 * @param systemPrompt - system instruction
 * @param userText     - message text
 * @param imageBase64  - ảnh dạng base64
 * @param mimeType     - "image/jpeg", "image/png", "image/heic"
 * @return JSON string từ AI, hoặc null nếu lỗi
 */
public String chatWithImage(String systemPrompt, String userText,
                             String imageBase64, String mimeType) {
    String url = props.getBaseUrl()
        + "/models/" + props.getModel()
        + ":generateContent?key=" + props.getApiKey();

    // Build parts: text + image
    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("text", userText));
    parts.add(Map.of("inline_data", Map.of(
        "mime_type", mimeType,
        "data",      imageBase64
    )));

    Map<String, Object> requestBody = Map.of(
        "systemInstruction", Map.of(
            "parts", List.of(Map.of("text", systemPrompt))
        ),
        "contents", List.of(Map.of("parts", parts)),
        "generationConfig", Map.of(
            "temperature",     0.1,
            "maxOutputTokens", 300,
            "responseMimeType","application/json"  // Force JSON output
        )
    );

    for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
        try {
            String jsonBody   = objectMapper.writeValueAsString(requestBody);
            long   startMs    = System.currentTimeMillis();

            Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

            // Timeout riêng cho Vision (dài hơn text) — 20s
            OkHttpClient visionClient = httpClient.newBuilder()
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

            try (Response response = visionClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startMs;

                if (response.code() == 429) {
                    Thread.sleep(1000L * attempt);
                    continue;
                }
                if (!response.isSuccessful()) {
                    log.error("Gemini Vision error: {} {}", response.code(), response.message());
                    return null;
                }

                String body = response.body().string();
                JsonNode node = objectMapper.readTree(body);
                String text = node.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("").trim();

                log.info("Gemini Vision OK: latency={}ms, responseLength={}",
                    latency, text.length());
                return text;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Gemini Vision attempt {}/{}: {}", attempt, props.getMaxRetries(), e.getMessage());
            if (attempt == props.getMaxRetries()) return null;
        }
    }
    return null;
}
```

---

### ⏰ 11:00–14:00 | T07: Prompt Engineering Receipt (3h)

> ⚠️ **PHẢI tự làm với ảnh thật.** Chuẩn bị ít nhất 10 ảnh hoá đơn thật từ nhiều loại: café, siêu thị, Grab, nhà thuốc, điện nước.

**Bước 1 — System Prompt yêu cầu JSON có cấu trúc:**
```
Bạn là hệ thống OCR phân tích hoá đơn cho ứng dụng tài chính cá nhân tại Việt Nam.

Phân tích ảnh hoá đơn và trả về JSON với format CHÍNH XÁC sau:
{
  "is_receipt": true/false,
  "amount": số_tiền_VND_hoặc_null,
  "description": "tên cửa hàng hoặc mô tả ngắn",
  "transaction_date": "YYYY-MM-DD hoặc null nếu không đọc được",
  "suggested_category": "một trong: Ăn uống/Đi lại/Mua sắm/Sức khỏe/Giáo dục/Hóa đơn/Giải trí/Nhà cửa/Chi khác",
  "confidence": số từ 0.0 đến 1.0,
  "notes": "ghi chú nếu có vấn đề với ảnh (mờ, thiếu thông tin, nghiêng)"
}

Quy tắc:
- amount: CHỈ lấy TỔNG tiền cuối bill (thường là "Tổng cộng", "Total", "Thành tiền"). KHÔNG tính từng món.
- Nếu đơn vị là nghìn đồng (VD: "250.000"), trả về nguyên giá trị (250000).
- confidence < 0.5: ảnh mờ, không đọc được rõ.
- is_receipt = false: ảnh không phải hoá đơn (selfie, phong cảnh, văn bản khác).
- Luôn trả về JSON hợp lệ, không có markdown.
```

**Bước 2 — User message:**
```
"Phân tích hoá đơn trong ảnh này."
```

**Bước 3 — Chạy test với 10 ảnh hoá đơn thật + ghi kết quả:**

| Ảnh | Expected category | AI response | Đúng? | Confidence |
|-----|------------------|-------------|-------|------------|
| Bill Grab | Đi lại | | | |
| Bill Highlands | Ăn uống | | | |
| Bill VinMart | Mua sắm | | | |
| Bill Long Châu | Sức khỏe | | | |
| Bill EVN | Hóa đơn | | | |
| Bill ảnh mờ | LOW_CONFIDENCE | | | |
| Ảnh selfie | NOT_A_RECEIPT | | | |
| Bill tiếng Anh | | | | |
| Bill viết tay | | | | |
| Bill chụp nghiêng | | | | |

**Target:** `is_receipt` accuracy ≥ 90%, category accuracy ≥ 80%, amount accuracy ≥ 85%

**Commit Day 2:**
```bash
git commit -m "feat: GeminiClient.chatWithImage() (Vision multimodal), receipt prompt finalized"
```

---

## 📅 DAY 3 — BillScannerService + Edge Cases + API Endpoint
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Gemini Vision client OK, prompt đạt accuracy tốt
🎯 Hôm nay: BillScannerService orchestrate + handle edge cases + endpoint
🚧 Blocker: Prompt có ảnh mờ/ảnh sai có trả về đúng flag không?
```

---

### ⏰ 09:00–11:00 | T08: BillScannerService (2h)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BillScannerService {

    private final GeminiClient geminiClient;
    private final FileValidationService fileValidation;
    private final ScanRateLimiter rateLimiter;
    private final CategoryRepository categoryRepository;
    private final BillScanLogRepository scanLogRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        Bạn là hệ thống OCR phân tích hoá đơn cho ứng dụng tài chính cá nhân tại Việt Nam.
        Phân tích ảnh và trả về JSON CHÍNH XÁC theo format sau (không có markdown):
        {
          "is_receipt": true/false,
          "amount": số_tiền_hoặc_null,
          "description": "tên cửa hàng",
          "transaction_date": "YYYY-MM-DD hoặc null",
          "suggested_category": "Ăn uống/Đi lại/Mua sắm/Sức khỏe/Giáo dục/Hóa đơn/Giải trí/Nhà cửa/Chi khác",
          "confidence": 0.0-1.0,
          "notes": "ghi chú nếu ảnh có vấn đề"
        }
        Quy tắc: amount = TỔNG cuối bill, không tính từng món. confidence < 0.5 = ảnh mờ.
        """;

    @Transactional
    public BillScanResult scan(MultipartFile file, Long userId) {
        long startMs = System.currentTimeMillis();
        BillScanLog scanLog = BillScanLog.builder()
            .userId(userId)
            .scanDate(LocalDate.now())
            .fileName(file.getOriginalFilename())
            .fileSizeBytes(file.getSize())
            .build();

        try {
            // 1. Validate file
            fileValidation.validate(file);
            String mimeType = fileValidation.detectMimeType(file);
            scanLog.setMimeType(mimeType);

            // 2. Rate limit
            rateLimiter.checkAndConsume(userId);

            // 3. Convert to base64
            String base64 = fileValidation.toBase64(file);

            // 4. Gọi Gemini Vision
            String aiResponse = geminiClient.chatWithImage(
                SYSTEM_PROMPT, "Phân tích hoá đơn trong ảnh này.",
                base64, mimeType
            );

            long latency = System.currentTimeMillis() - startMs;
            scanLog.setLatencyMs(latency);

            if (aiResponse == null) {
                scanLog.setScanSuccess(false);
                scanLog.setErrorType("TIMEOUT");
                saveScanLog(scanLog);
                return BillScanResult.builder()
                    .status(ScanStatus.TIMEOUT)
                    .warningMessage("Không thể xử lý ảnh lúc này (timeout). Vui lòng thử lại.")
                    .build();
            }

            // 5. Parse JSON response
            return parseAndBuildResult(aiResponse, scanLog);

        } catch (RateLimitException e) {
            scanLog.setScanSuccess(false);
            scanLog.setErrorType("RATE_LIMITED");
            saveScanLog(scanLog);
            throw e; // Re-throw để GlobalExceptionHandler xử lý
        } catch (BusinessException e) {
            scanLog.setScanSuccess(false);
            scanLog.setErrorType("VALIDATION_ERROR");
            saveScanLog(scanLog);
            throw e;
        } catch (Exception e) {
            log.error("Bill scan unexpected error: {}", e.getMessage(), e);
            scanLog.setScanSuccess(false);
            scanLog.setErrorType("ERROR");
            saveScanLog(scanLog);
            return BillScanResult.builder()
                .status(ScanStatus.ERROR)
                .warningMessage("Đã có lỗi xảy ra. Vui lòng thử lại.")
                .build();
        }
    }

    private BillScanResult parseAndBuildResult(String aiJson, BillScanLog scanLog) {
        try {
            JsonNode node = objectMapper.readTree(cleanJson(aiJson));

            boolean isReceipt  = node.path("is_receipt").asBoolean(true);
            double  confidence = node.path("confidence").asDouble(0.5);
            String  notes      = node.path("notes").asText("");

            // Không phải hoá đơn
            if (!isReceipt) {
                scanLog.setScanSuccess(false);
                scanLog.setErrorType("NOT_A_RECEIPT");
                saveScanLog(scanLog);
                return BillScanResult.builder()
                    .status(ScanStatus.NOT_A_RECEIPT)
                    .confidence(confidence)
                    .warningMessage("Không tìm thấy thông tin hoá đơn trong ảnh. Vui lòng chụp lại.")
                    .build();
            }

            // Parse amount
            BigDecimal amount = null;
            if (!node.path("amount").isNull()) {
                amount = new BigDecimal(node.path("amount").asText("0"));
            }

            // Parse date — fallback today nếu không đọc được
            LocalDate txnDate = null;
            boolean dateNeedsVerify = false;
            String dateStr = node.path("transaction_date").asText("");
            if (!dateStr.isBlank() && !dateStr.equals("null")) {
                try {
                    txnDate = LocalDate.parse(dateStr);
                } catch (Exception ignored) {}
            }
            if (txnDate == null) {
                txnDate = LocalDate.now();
                dateNeedsVerify = true;
            }

            // Map category
            String suggestedCategory = node.path("suggested_category").asText("Chi khác");
            Optional<Category> category = categoryRepository
                .findByNameIgnoreCase(suggestedCategory);

            // Determine status
            ScanStatus status;
            String warningMessage = null;
            boolean amountNeedsVerify = (amount == null);

            if (amount == null) {
                status = ScanStatus.AMOUNT_MISSING;
                warningMessage = "Không đọc được số tiền. Vui lòng nhập tay trước khi lưu.";
            } else if (confidence < 0.5) {
                status = ScanStatus.LOW_CONFIDENCE;
                warningMessage = "⚠️ Ảnh không rõ nét, vui lòng kiểm tra lại thông tin trước khi lưu.";
                if (!notes.isBlank()) warningMessage += " (" + notes + ")";
            } else {
                status = ScanStatus.SUCCESS;
            }

            scanLog.setScanSuccess(status == ScanStatus.SUCCESS
                || status == ScanStatus.LOW_CONFIDENCE);
            scanLog.setConfidence(BigDecimal.valueOf(confidence));
            scanLog.setExtractedAmount(amount);
            saveScanLog(scanLog);

            return BillScanResult.builder()
                .amount(amount)
                .description(node.path("description").asText(""))
                .transactionDate(txnDate)
                .categoryId(category.map(Category::getId).orElse(null))
                .categoryName(category.map(Category::getName).orElse(suggestedCategory))
                .confidence(confidence)
                .status(status)
                .warningMessage(warningMessage)
                .amountNeedsVerification(amountNeedsVerify)
                .dateNeedsVerification(dateNeedsVerify)
                .categoryNeedsVerification(confidence < 0.6)
                .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response: {}", aiJson);
            scanLog.setScanSuccess(false);
            scanLog.setErrorType("PARSE_ERROR");
            saveScanLog(scanLog);
            return BillScanResult.builder()
                .status(ScanStatus.ERROR)
                .warningMessage("Không thể đọc kết quả AI. Vui lòng thử lại.")
                .build();
        }
    }

    /** Gemini đôi khi wrap JSON trong markdown fences — clean trước khi parse */
    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        return raw.trim()
            .replaceAll("^```json\\s*", "")
            .replaceAll("^```\\s*",     "")
            .replaceAll("```$",         "")
            .trim();
    }

    private void saveScanLog(BillScanLog log) {
        try { scanLogRepository.save(log); }
        catch (Exception e) { log.warn("Failed to save scan log: {}", e.getMessage()); }
    }
}
```

---

### ⏰ 11:00–13:00 | T09: Edge Cases Handling (2h)

**Test + Fix từng edge case:**

| Edge Case | Xử lý trong code | Test thủ công |
|-----------|-----------------|---------------|
| Ảnh mờ/nghiêng | `confidence < 0.5` → `LOW_CONFIDENCE` + warning vàng | ✅/❌ |
| Không có số tiền | `amount == null` → `AMOUNT_MISSING`, block submit | ✅/❌ |
| Ảnh selfie / phong cảnh | `is_receipt: false` → `NOT_A_RECEIPT` | ✅/❌ |
| Gemini timeout (>20s) | `aiResponse == null` → `TIMEOUT` | Mock bằng cách ngắt internet |
| File > 10MB | `FileValidationService` throw trước khi gọi Gemini | ✅/❌ |
| File PDF | `detectMimeType` fail → BusinessException | ✅/❌ |
| Vượt 20 scans/ngày | `RateLimitException` → 429 | Test bằng scan 21 lần |
| Amount viết dạng "250.000đ" | Gemini extract thành `250000` — verify kết quả | ✅/❌ |
| Bill tiếng Anh | Category vẫn mapping được | ✅/❌ |
| JSON Gemini có markdown fence | `cleanJson()` strip trước khi parse | ✅/❌ |

---

### ⏰ 14:00–15:30 | T10+T11: Endpoints (1.5h)

```java
@RestController
@RequestMapping("/api/scanner")
@RequiredArgsConstructor
@Tag(name = "Bill Scanner", description = "OCR hoá đơn bằng AI")
@SecurityRequirement(name = "Bearer Auth")
public class BillScannerController {

    private final BillScannerService scannerService;
    private final ScanRateLimiter rateLimiter;

    /**
     * POST /api/scanner/scan
     * Nhận ảnh, trả về kết quả OCR (KHÔNG lưu transaction)
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Scan hoá đơn",
               description = "Upload ảnh hoá đơn → AI trả về thông tin điền sẵn. Giới hạn 20 lần/ngày.")
    public ResponseEntity<ApiResponse<BillScanResult>> scan(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = securityUtils.getCurrentUserId(userDetails);
        BillScanResult result = scannerService.scan(image, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/scanner/quota
     * Trả về số lần scan còn lại trong ngày
     */
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuota(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = securityUtils.getCurrentUserId(userDetails);
        int remaining = rateLimiter.getRemainingScans(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "remainingScans", remaining,
            "maxPerDay",      20,
            "resetAt",        "00:00 ngày mai"
        )));
    }
}
```

**FE confirm → dùng lại `POST /api/transactions` từ Sprint 2** (không cần endpoint mới).

**Commit Day 3:**
```bash
git commit -m "feat: BillScannerService (orchestrate + edge cases + JSON cleaning), scan endpoint, quota endpoint"
```

---

## 📅 DAY 4 — Unit Tests + Postman + Config
**6 tiếng | Thứ Năm**

### ⏰ 09:00–11:00 | T12: Unit Tests BillScannerService (2h)

```java
@ExtendWith(MockitoExtension.class)
class BillScannerServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private FileValidationService fileValidation;
    @Mock private ScanRateLimiter rateLimiter;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BillScanLogRepository scanLogRepository;

    @InjectMocks private BillScannerService service;

    @Test @DisplayName("Scan thành công — bill rõ ràng")
    void scan_success() {
        MultipartFile file = mockFile("bill.jpg", "image/jpeg", 500_000);
        when(fileValidation.detectMimeType(file)).thenReturn("image/jpeg");
        when(fileValidation.toBase64(file)).thenReturn("base64data");
        when(geminiClient.chatWithImage(any(), any(), any(), any())).thenReturn("""
            {"is_receipt":true,"amount":250000,"description":"Highlands Coffee",
             "transaction_date":"2026-06-05","suggested_category":"Ăn uống",
             "confidence":0.92,"notes":""}
            """);
        when(categoryRepository.findByNameIgnoreCase("Ăn uống"))
            .thenReturn(Optional.of(mockCategory(4L, "Ăn uống")));

        BillScanResult result = service.scan(file, 1L);

        assertEquals(ScanStatus.SUCCESS, result.getStatus());
        assertEquals(new BigDecimal("250000"), result.getAmount());
        assertEquals("Highlands Coffee", result.getDescription());
        assertEquals(4L, result.getCategoryId());
        assertEquals(0.92, result.getConfidence());
        assertFalse(result.isAmountNeedsVerification());
    }

    @Test @DisplayName("Confidence thấp → LOW_CONFIDENCE + warning")
    void scan_lowConfidence() {
        // ... mock response với confidence: 0.4
        BillScanResult result = service.scan(file, 1L);
        assertEquals(ScanStatus.LOW_CONFIDENCE, result.getStatus());
        assertNotNull(result.getWarningMessage());
    }

    @Test @DisplayName("Không phải hoá đơn → NOT_A_RECEIPT")
    void scan_notAReceipt() {
        // ... mock response với is_receipt: false
        assertEquals(ScanStatus.NOT_A_RECEIPT, result.getStatus());
        assertTrue(result.getWarningMessage().contains("Không tìm thấy"));
    }

    @Test @DisplayName("Amount null → AMOUNT_MISSING, flag bắt buộc nhập")
    void scan_amountMissing() {
        // ... mock amount: null
        assertEquals(ScanStatus.AMOUNT_MISSING, result.getStatus());
        assertTrue(result.isAmountNeedsVerification());
    }

    @Test @DisplayName("Gemini timeout → TIMEOUT, không crash")
    void scan_geminiTimeout() {
        when(geminiClient.chatWithImage(any(), any(), any(), any())).thenReturn(null);
        BillScanResult result = service.scan(file, 1L);
        assertEquals(ScanStatus.TIMEOUT, result.getStatus());
    }

    @Test @DisplayName("Response có markdown fence → cleanJson() xử lý đúng")
    void scan_jsonWithMarkdownFence() {
        when(geminiClient.chatWithImage(any(), any(), any(), any())).thenReturn(
            "```json\n{\"is_receipt\":true,\"amount\":150000,...}\n```"
        );
        BillScanResult result = service.scan(file, 1L);
        assertNotEquals(ScanStatus.ERROR, result.getStatus()); // Parse không crash
    }

    @Test @DisplayName("Rate limit exceeded → RateLimitException throw")
    void scan_rateLimitExceeded() {
        doThrow(new RateLimitException("...")).when(rateLimiter).checkAndConsume(1L);
        assertThrows(RateLimitException.class, () -> service.scan(file, 1L));
        verify(geminiClient, never()).chatWithImage(any(), any(), any(), any());
    }
}
```

**FileValidationService tests:**
- `validate_validJpeg()` → OK
- `validate_fileTooLarge()` → BusinessException
- `validate_unknownMimeType()` → BusinessException
- `validate_emptyFile()` → BusinessException
- `detectMimeType_jpeg()` → "image/jpeg" từ magic bytes
- `detectMimeType_png()` → "image/png"

---

### ⏰ 11:00–12:00 | T13: Unit Test FileValidation (1h)

Tạo mock file với magic bytes thật:
```java
private MultipartFile mockJpeg() {
    byte[] jpegHeader = new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF,
                                    (byte)0xE0, 0x00, 0x10, 0x4A, 0x46,
                                    0x49, 0x46, 0x00, 0x01 };
    return new MockMultipartFile("img", "test.jpg", "image/jpeg", jpegHeader);
}
```

---

### ⏰ 13:00–14:00 | T14: Postman Collection (1h)

```
📁 Bill Scanner (Sprint 10)
  │
  ├── POST /api/scanner/scan (ảnh bill rõ)
  │     Body: form-data, key="image", value=bill.jpg
  │     Expected: status SUCCESS, amount đúng, category đúng
  │
  ├── POST /api/scanner/scan (ảnh selfie)
  │     Expected: status NOT_A_RECEIPT
  │
  ├── POST /api/scanner/scan (ảnh mờ)
  │     Expected: status LOW_CONFIDENCE, warningMessage có text
  │
  ├── POST /api/scanner/scan (file PDF)
  │     Expected: 400 "Chỉ chấp nhận ảnh JPG, PNG, HEIC"
  │
  ├── POST /api/scanner/scan (file > 10MB)
  │     Expected: 400 "File quá lớn"
  │
  ├── GET /api/scanner/quota
  │     Expected: remainingScans: 19 (sau 1 scan)
  │
  └── FE flow: POST /api/scanner/scan → lấy result →
      POST /api/transactions (confirm) → transaction tạo thành công
```

**Test tự động:**
```javascript
pm.test("Scan success has amount", () => {
    const data = pm.response.json().data;
    if (data.status === 'SUCCESS') {
        pm.expect(data.amount).to.be.greaterThan(0);
    }
});

pm.test("Amount missing triggers flag", () => {
    const data = pm.response.json().data;
    if (data.status === 'AMOUNT_MISSING') {
        pm.expect(data.amountNeedsVerification).to.be.true;
    }
});
```

---

### ⏰ 14:00–14:30 | T15: Config (30 phút)

```yaml
# application.yml — thêm scanner config
app:
  scanner:
    max-scans-per-day: 20
    supported-mime-types: image/jpeg,image/png,image/heic
    max-file-size-mb: 10

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
```

**`SecurityConfig` — cho phép multipart endpoint:**
```java
// Không cần thay đổi — đã có JWT auth, endpoint /api/scanner/** tự được protect
```

**Commit Day 4:**
```bash
git commit -m "feat: unit tests BillScannerService + FileValidation, Postman Sprint 10, config"
```

---

## 📅 DAY 5 — FE: Upload + Animation + Pre-filled Form
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Tests GREEN, Postman flow OK
🎯 Hôm nay: FE — UX scan bill hoàn chỉnh
🚧 Blocker: Test trên mobile browser có camera upload được không?
```

---

### ⏰ 09:00–11:00 | T16: FE — Upload Component (2h)

```jsx
const BillUploader = ({ onScanComplete }) => {
    const [isDragging, setIsDragging] = useState(false);
    const fileInputRef = useRef(null);

    const handleFile = async (file) => {
        if (!file) return;
        onScanComplete(file); // Pass lên parent để xử lý
    };

    // Drag & Drop
    const handleDrop = (e) => {
        e.preventDefault();
        setIsDragging(false);
        const file = e.dataTransfer.files[0];
        handleFile(file);
    };

    return (
        <div
            className={`upload-zone ${isDragging ? 'dragging' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current.click()}
        >
            <div className="upload-icon">📷</div>
            <h3>Chụp hoặc tải ảnh hoá đơn</h3>
            <p>Kéo thả ảnh vào đây hoặc click để chọn</p>
            <p className="upload-hint">JPG, PNG, HEIC — tối đa 10MB</p>

            <div className="upload-buttons">
                {/* Camera button — mobile */}
                <button onClick={(e) => {
                    e.stopPropagation();
                    const input = document.createElement('input');
                    input.type = 'file';
                    input.accept = 'image/*';
                    input.capture = 'environment'; // camera sau
                    input.onchange = (ev) => handleFile(ev.target.files[0]);
                    input.click();
                }}>📷 Chụp ảnh</button>

                {/* Gallery button */}
                <button onClick={(e) => {
                    e.stopPropagation();
                    fileInputRef.current.click();
                }}>🖼️ Chọn từ thư viện</button>
            </div>

            <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/heic"
                hidden
                onChange={(e) => handleFile(e.target.files[0])}
            />
        </div>
    );
};
```

**CSS:**
```css
.upload-zone {
    border: 2px dashed #6366f1;
    border-radius: 16px;
    padding: 40px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    background: #f8f7ff;
}
.upload-zone.dragging {
    background: #ede9fe;
    border-color: #4f46e5;
    transform: scale(1.01);
}
.upload-zone:hover { background: #f0f0ff; }
```

---

### ⏰ 11:00–12:30 | T17: FE — Scanning Animation (1.5h)

```jsx
const ScanningAnimation = ({ status }) => {
    const steps = [
        { icon: '🔍', text: 'Đang nhận diện hoá đơn...' },
        { icon: '💰', text: 'Đang đọc số tiền...' },
        { icon: '🏷️', text: 'Đang phân loại danh mục...' },
        { icon: '✅', text: 'Hoàn tất!' },
    ];
    const [currentStep, setCurrentStep] = useState(0);

    useEffect(() => {
        if (status !== 'scanning') return;
        const timer = setInterval(() => {
            setCurrentStep(prev =>
                prev < steps.length - 1 ? prev + 1 : prev);
        }, 1200); // Mỗi step 1.2s, tổng ~4.8s (nhỏ hơn timeout 15s)
        return () => clearInterval(timer);
    }, [status]);

    if (status !== 'scanning') return null;

    return (
        <div className="scanning-overlay">
            <div className="scanning-card">
                <div className="pulse-ring" />
                <span className="step-icon">{steps[currentStep].icon}</span>
                <p className="step-text">{steps[currentStep].text}</p>
                <div className="progress-dots">
                    {steps.map((_, i) => (
                        <div key={i}
                             className={`dot ${i <= currentStep ? 'active' : ''}`} />
                    ))}
                </div>
            </div>
        </div>
    );
};
```

**CSS animation:**
```css
.scanning-overlay {
    position: fixed; inset: 0;
    background: rgba(0,0,0,0.5);
    display: flex; align-items: center; justify-content: center;
    z-index: 1000;
}
.scanning-card {
    background: white; border-radius: 20px;
    padding: 40px; text-align: center; min-width: 280px;
}
.step-icon { font-size: 3rem; }
.pulse-ring {
    width: 80px; height: 80px; border-radius: 50%;
    border: 3px solid #6366f1;
    animation: pulse 1.5s ease-out infinite;
    margin: 0 auto 16px;
}
@keyframes pulse {
    0%   { transform: scale(0.8); opacity: 1; }
    100% { transform: scale(1.5); opacity: 0; }
}
.dot { width: 8px; height: 8px; border-radius: 50%; background: #e5e7eb; }
.dot.active { background: #6366f1; }
```

---

### ⏰ 13:00–15:00 | T18+T19: FE — Pre-filled Form + Error States (2.5h)

```jsx
const BillScanPage = () => {
    const [scanStatus, setScanStatus] = useState('idle');
    // idle → scanning → reviewing → done
    const [scanResult, setScanResult] = useState(null);
    const [formData, setFormData] = useState({});
    const [preview, setPreview] = useState(null);

    const handleFile = async (file) => {
        // Preview ảnh
        setPreview(URL.createObjectURL(file));
        setScanStatus('scanning');

        const fd = new FormData();
        fd.append('image', file);

        try {
            const res = await api.post('/scanner/scan', fd, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            const result = res.data.data;
            setScanResult(result);

            // Pre-fill form từ kết quả AI
            setFormData({
                amount:          result.amount || '',
                description:     result.description || '',
                transactionDate: result.transactionDate || new Date().toISOString().slice(0,10),
                categoryId:      result.categoryId || '',
                type:            'EXPENSE',
            });

            setScanStatus('reviewing');
        } catch (err) {
            setScanStatus('error');
        }
    };

    const handleConfirm = async () => {
        if (!formData.amount || formData.amount <= 0) {
            alert('Vui lòng nhập số tiền');
            return;
        }
        await api.post('/transactions', formData);
        toast.success('Đã lưu giao dịch!');
        setScanStatus('done');
    };

    return (
        <div className="scan-page">
            {scanStatus === 'idle' && (
                <BillUploader onScanComplete={handleFile} />
            )}

            <ScanningAnimation status={scanStatus} />

            {scanStatus === 'reviewing' && scanResult && (
                <div className="review-form">
                    {/* Preview ảnh nhỏ */}
                    <img src={preview} className="bill-preview" alt="Hoá đơn" />

                    {/* Warning banner */}
                    {scanResult.warningMessage && (
                        <div className={`alert ${
                            scanResult.status === 'AMOUNT_MISSING' ? 'alert-red' : 'alert-yellow'
                        }`}>
                            {scanResult.warningMessage}
                        </div>
                    )}

                    {/* Amount field */}
                    <div className={`form-field ${scanResult.amountNeedsVerification ? 'required' : ''}`}>
                        <label>Số tiền *</label>
                        <input
                            type="number"
                            value={formData.amount}
                            onChange={e => setFormData({...formData, amount: e.target.value})}
                            placeholder="Nhập số tiền..."
                            className={!formData.amount ? 'input-error' : ''}
                        />
                    </div>

                    {/* Category với AI badge */}
                    <div className="form-field">
                        <label>Danh mục</label>
                        <div className="category-row">
                            <select
                                value={formData.categoryId}
                                onChange={e => setFormData({...formData, categoryId: e.target.value})}
                            >
                                {categories.map(c => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                            {scanResult.categoryId && (
                                <span className="ai-badge"
                                      title={`AI tin cậy: ${(scanResult.confidence * 100).toFixed(0)}%`}>
                                    🤖 AI ({(scanResult.confidence * 100).toFixed(0)}%)
                                </span>
                            )}
                        </div>
                    </div>

                    {/* Description */}
                    <div className="form-field">
                        <label>Mô tả</label>
                        <input value={formData.description}
                               onChange={e => setFormData({...formData, description: e.target.value})} />
                    </div>

                    {/* Date */}
                    <div className="form-field">
                        <label>Ngày {scanResult.dateNeedsVerification && '(cần xác nhận)'}</label>
                        <input type="date" value={formData.transactionDate}
                               onChange={e => setFormData({...formData, transactionDate: e.target.value})} />
                    </div>

                    {/* Actions */}
                    <div className="form-actions">
                        <button className="btn-secondary"
                                onClick={() => setScanStatus('idle')}>
                            ← Scan lại
                        </button>
                        <button className="btn-primary"
                                onClick={handleConfirm}
                                disabled={!formData.amount}>
                            ✅ Xác nhận & Lưu
                        </button>
                    </div>
                </div>
            )}

            {/* NOT_A_RECEIPT state */}
            {scanStatus === 'reviewing'
             && scanResult?.status === 'NOT_A_RECEIPT' && (
                <div className="error-state">
                    <span>📷</span>
                    <p>Không tìm thấy hoá đơn trong ảnh</p>
                    <button onClick={() => setScanStatus('idle')}>Chụp lại</button>
                </div>
            )}
        </div>
    );
};
```

**Commit Day 5:**
```bash
git commit -m "feat: FE BillScanPage — upload + drag-drop + camera, scanning animation, pre-filled form, error states"
```

---

## 📅 DAY 6 — Integration Test + Bug Fix + Sprint Review
**6 tiếng | Thứ Bảy**

### ⏰ 09:00–11:30 | Integration Test (2.5h)

```
Flow thành công:
1.  GET /api/scanner/quota → remainingScans: 20           ✅/❌
2.  POST /api/scanner/scan (bill Highlands)               ✅/❌
    → status: SUCCESS, amount: 85000, category: Ăn uống
3.  GET quota → remainingScans: 19                        ✅/❌
4.  POST /api/transactions với data từ scan               ✅/❌
5.  GET /api/transactions → thấy giao dịch vừa lưu        ✅/❌

Edge cases:
6.  Scan file PDF → 400 message rõ ràng                   ✅/❌
7.  Scan file > 10MB → 400                                ✅/❌
8.  Scan ảnh selfie → NOT_A_RECEIPT                       ✅/❌
9.  Scan 20 lần → lần 21: 429 rate limited               ✅/❌
10. Ảnh mờ → LOW_CONFIDENCE + warning message            ✅/❌
11. FE: AMOUNT_MISSING → nút Lưu bị disabled             ✅/❌
12. FE: drag-drop file → preview hiện + scan tự động     ✅/❌

Mobile test (quan trọng):
13. Mở trên Chrome Android → camera button có hiện không ✅/❌
14. Chụp ảnh bằng camera → scan OK                      ✅/❌
```

**Common bugs:**

| Bug | Fix |
|-----|-----|
| Gemini trả JSON có markdown fence | `cleanJson()` strip `\`\`\`json` trước khi parse |
| Amount "250.000" parse thành 250 | Check nếu Gemini strip dấu chấm → `250000` |
| Camera button không hiện trên iOS Safari | Dùng `<input capture="environment">` không dùng JS |
| Preview ảnh bị memory leak | `URL.revokeObjectURL(preview)` khi unmount |
| Form reset sau khi confirm | `setFormData({})`, `setScanResult(null)`, `setPreview(null)` |
| Confidence badge hiện % sai | `(0.92 * 100).toFixed(0)` = "92", không phải "92.0" |

---

### ⏰ 11:30–12:30 | Sprint Review (1h)

**Deliverables:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | POST /api/scanner/scan — nhận ảnh, trả về OCR result | ⬜ |
| 2 | Rate limit: 20 scans/ngày/user | ⬜ |
| 3 | File validation: type + size + magic bytes | ⬜ |
| 4 | SUCCESS: amount + category + date đọc đúng | ⬜ |
| 5 | LOW_CONFIDENCE: warning vàng | ⬜ |
| 6 | AMOUNT_MISSING: block submit | ⬜ |
| 7 | NOT_A_RECEIPT: thông báo rõ | ⬜ |
| 8 | TIMEOUT: fallback message | ⬜ |
| 9 | FE: drag-drop + camera + gallery upload | ⬜ |
| 10 | FE: scanning animation 4 bước | ⬜ |
| 11 | FE: pre-filled form + AI badge confidence | ⬜ |
| 12 | Unit tests GREEN | ⬜ |
| 13 | Gemini JSON fence cleaning | ⬜ |

**Demo (5 phút):**
```
1. Mở trang Scanner
2. Drag-drop ảnh bill Highlands → scanning animation
3. Form điền sẵn: 85.000đ, Ăn uống 🤖 AI (92%)
4. Click Xác nhận → toast "Đã lưu!"
5. Dashboard → thấy giao dịch mới
6. Drag ảnh selfie → "Không phải hoá đơn"
7. Drag ảnh mờ → warning vàng, vẫn điền được
```

**Commit cuối:**
```bash
git commit -m "chore: integration bugs fixed, Sprint 10 complete"
git tag -a sprint-10 -m "Sprint 10: Bill/Receipt Scanner (Gemini Vision)"
git push origin main --tags
```

---

## 📊 Sprint 10 Summary

### Thời Gian
```
Day 1: Migration + DTO + FileValidation + RateLimit    → 6h
Day 2: GeminiClient Vision extension + Prompt          → 6h  ← Prompt tự test
Day 3: BillScannerService + Edge cases + Endpoints     → 6h  ← Core logic
Day 4: Unit tests + Postman + Config                   → 6h
Day 5: FE — Upload + Animation + Form                  → 6h
Day 6: Integration test + Bug fix + Review             → 6h
────────────────────────────────────────────────────────────
Total:                                                 36h
```

### Gemini Vision Quota
```
Mỗi scan = 1 Vision call (~500-1000 tokens input với ảnh)
Free tier Vision: 1500 requests/ngày (giống text)
20 scans/user/ngày → đủ cho nhiều user song song
Ảnh không lưu → không tốn storage
```

### Điểm Kỹ Thuật Cần Nhớ

| Vấn đề | Giải pháp |
|--------|-----------|
| Gemini đôi khi wrap JSON bằng markdown | `cleanJson()` strip ``` trước khi parse |
| `capture="environment"` vs JS camera API | Dùng input HTML để tương thích iOS |
| Base64 ảnh 10MB = ~13MB string | OK cho Gemini, nhưng cân nhắc compress trước |
| Magic bytes vs Content-Type | Luôn detect từ bytes, không tin client |

---

*Sprint 10 Complete → Bill Scanner ✅ | Gemini Vision tích hợp hoàn chỉnh* 📄🤖
