package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.config.GeminiProperties;
import com.data.personalfinanceinsightai.dto.response.transaction.CategorizationResult;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationService {

    private final GeminiClient geminiClient;
    private final CategorizationCacheService cacheService;
    private final AiCallLogService logService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AiRateLimiter rateLimiter;
    private final GeminiProperties geminiProperties;

    /** Fallback category name when AI returns unknown category */
    private static final String FALLBACK_CATEGORY = "Khác";

    /** Maximum description length sent to AI */
    private static final int MAX_DESC_LENGTH = 200;

    private static final String SYSTEM_PROMPT = """
        Bạn là assistant phân loại giao dịch tài chính cá nhân tại Việt Nam.
        Chỉ trả về đúng một tên danh mục từ danh sách dưới đây.
        KHÔNG thêm bất kỳ từ nào khác. KHÔNG giải thích.

        Danh mục thu nhập: Lương, Thu nhập khác
        Danh mục chi tiêu: Ăn uống, Đi lại, Mua sắm, Giải trí, Sức khỏe, Giáo dục, Tiện ích, Nhà ở, Du lịch, Cà phê, Thể thao, Quà tặng, Khác

        Nếu không xác định được -> trả về: Khác
        """;

    /**
     * Phân loại giao dịch qua AI với flow: cache → Gemini → log.
     * Fallback handling đầy đủ:
     * - Description < 2 ký tự → SKIPPED
     * - Cache hit → trả kết quả ngay, không gọi AI
     * - Rate limit exceeded → FAILED
     * - Gemini timeout/network error → FAILED (transaction vẫn tạo được)
     * - Gemini trả category không có trong DB → fallback sang "Khác"
     * - Gemini trả empty → FAILED
     * - Description > 200 ký tự → truncate trước khi gửi
     */
    public CategorizationResult categorize(String description, Long userId) {

        // 0. Description quá ngắn -> skip, không gọi AI
        if (cacheService.isTooShort(description)) {
            return CategorizationResult.skipped("Description quá ngắn");
        }

        // Lấy thông tin User để ghi log
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // 1. Check cache trước — tránh gọi AI lại cho cùng description
        Optional<com.data.personalfinanceinsightai.entity.Category> cached = cacheService.getCachedCategory(description);
        if (cached.isPresent()) {
            log.debug("Cache hit for description, skipping AI call");
            return CategorizationResult.fromCache(cached.get().getId(), cached.get().getName());
        }

        // 2. Rate limit (chỉ áp dụng khi phải gọi AI thật)
        if (!rateLimiter.tryConsume(userId)) {
            log.warn("Rate limit exceeded for userId={}", userId);
            return CategorizationResult.failed("Quá nhiều yêu cầu, thử lại sau");
        }

        // 3. Gọi Gemini — wrap trong try-catch để handle mọi lỗi
        String aiResponse;
        long latency;
        try {
            long start = System.currentTimeMillis();
            aiResponse = geminiClient.chat(
                    SYSTEM_PROMPT,
                    "Giao dịch: \"" + truncate(description, MAX_DESC_LENGTH) + "\""
            );
            latency = System.currentTimeMillis() - start;
        } catch (Exception e) {
            // Fallback: timeout, network error, IOException, bất kỳ exception nào
            // Transaction vẫn tạo được — không crash
            String errorMsg = categorizeError(e);
            log.error("Gemini call failed: {} — {}", errorMsg, e.getMessage());
            if (user != null) {
                logService.logFailure(user, "CATEGORIZATION", errorMsg + ": " + e.getMessage());
            }
            return CategorizationResult.failed(errorMsg);
        }

        // 4. Gemini không phản hồi hoặc trả empty string
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("Gemini returned null/empty response");
            if (user != null) {
                logService.logFailure(user, "CATEGORIZATION", "Gemini returned null/empty");
            }
            return CategorizationResult.failed("AI không phản hồi");
        }

        // 5. Map tên category -> category ID
        String cleanedResponse = aiResponse.trim();
        Optional<Category> matched = categoryRepository
                .findByNameIgnoreCase(cleanedResponse);

        if (matched.isEmpty()) {
            log.warn("Gemini returned unknown category: '{}', falling back to '{}'",
                    cleanedResponse, FALLBACK_CATEGORY);
            // Fallback: dùng "Khác" (matching V5 seed data)
            matched = categoryRepository.findByName(FALLBACK_CATEGORY);
            if (matched.isEmpty()) {
                log.error("Fallback category '{}' not found in database!", FALLBACK_CATEGORY);
                if (user != null) {
                    logService.logFailure(user, "CATEGORIZATION",
                            "Cannot map: " + cleanedResponse + ", fallback missing");
                }
                return CategorizationResult.failed("Không map được danh mục");
            }
        }

        Category category = matched.get();

        // 6. Lưu cache cho lần sau
        cacheService.cacheResult(description, category);

        // 7. Log AI call thành công
        if (user != null) {
            logService.logSuccess(user, "CATEGORIZATION",
                    geminiProperties.getModel(), 0, 0, (int) latency);
        }

        return CategorizationResult.fromAI(category.getId(), category.getName());
    }

    /**
     * Truncate description trước khi gửi AI (> 200 chars).
     * Tránh prompt quá dài, tốn token, giảm latency.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * Phân loại lỗi để trả message thân thiện cho user.
     */
    private String categorizeError(Exception e) {
        String className = e.getClass().getSimpleName();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (className.contains("Timeout") || message.contains("timeout")) {
            return "AI phản hồi quá chậm";
        }
        if (message.contains("connection") || message.contains("network")
                || message.contains("unreachable") || message.contains("connect")) {
            return "Không thể kết nối đến AI";
        }
        if (className.contains("WebClientResponseException") || message.contains("429")) {
            return "AI đang quá tải, thử lại sau";
        }
        return "AI xử lý thất bại";
    }
}

