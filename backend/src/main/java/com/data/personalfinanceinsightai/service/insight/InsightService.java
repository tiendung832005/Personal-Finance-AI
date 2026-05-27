package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.dto.response.insight.InsightResponse;
import com.data.personalfinanceinsightai.entity.AiInsight;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.InsightType;
import com.data.personalfinanceinsightai.repository.AiInsightRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AiCallLogService;
import com.data.personalfinanceinsightai.service.GeminiClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T08 — InsightService: Logic phân tích và tạo nhận xét chi tiêu hàng tháng.
 * Tích hợp Gemini API và Cache DB để tối ưu chi phí (quota).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final GeminiClient geminiClient;
    private final InsightDataCollector dataCollector;
    private final AiInsightRepository insightRepository;
    private final UserRepository userRepository;
    private final AiCallLogService callLogService;

    /**
     * Tìm UserId từ Email (Username).
     */
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tài chính AI thông minh và thân thiện của ứng dụng Personal Finance Insight AI.
            Nhiệm vụ của bạn là phân tích dữ liệu chi tiêu hàng tháng và đưa ra nhận xét ngắn gọn, thực tế.
            
            Yêu cầu:
            1. Viết bằng tiếng Việt tự nhiên, ấm áp, như một người bạn tư vấn tài chính.
            2. Độ dài: BẮT BUỘC từ 3 đến 5 câu văn hoàn chỉnh. 
            3. KHÔNG được dừng giữa chừng. Phải viết thành một đoạn văn thống nhất, không dùng gạch đầu dòng.
            4. Cấu trúc: [Tình hình chung] -> [Điểm nổi bật cụ thể từ dữ liệu] -> [Một lời khuyên thực tế].
            5. Không phán xét, luôn giữ thái độ khuyến khích.
            """;

    private static final String FAMILY_SYSTEM_PROMPT = """
            Bạn là cố vấn tài chính gia đình AI thông minh của ứng dụng Personal Finance Insight AI.
            Nhiệm vụ của bạn là phân tích dữ liệu chi tiêu CHUNG của cả gia đình/nhóm và đưa ra nhận xét giúp mọi thành viên cùng tiến bộ.
            
            Yêu cầu:
            1. Viết bằng tiếng Việt thân thiện, đề cao tính kết nối và trách nhiệm chung.
            2. Độ dài: BẮT BUỘC từ 4 đến 6 câu văn hoàn chỉnh.
            3. Tập trung vào: Tổng chi tiêu nhóm, sự đóng góp giữa các thành viên, và việc tuân thủ ngân sách chung.
            4. Cấu trúc: [Tổng quan quỹ chung] -> [Nhận xét về các khoản chi/danh mục nổi bật] -> [Lời khuyên tiết kiệm hoặc động viên chung cho cả nhà].
            5. Tuyệt đối không dừng giữa chừng, không dùng gạch đầu dòng.
            """;

    /**
     * Lấy insight tháng cho người dùng cá nhân.
     * Kiểm tra DB trước (cache hit), nếu chưa có mới gọi AI (cache miss).
     *
     * @param userId   ID người dùng
     * @param monthStr Chuỗi tháng định dạng "yyyy-MM"
     */
    @Transactional
    public InsightResponse getMonthlyInsight(Long userId, String monthStr) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);

        // 1. Kiểm tra Cache trong DB
        Optional<AiInsight> cached = insightRepository
                .findByUser_IdAndMonthAndInsightType(userId, monthDate, InsightType.PERSONAL);

        if (cached.isPresent()) {
            log.debug("Insight cache hit for userId={}, month={}", userId, monthStr);
            return mapToResponse(cached.get(), monthStr, true);
        }

        // 2. Thu thập dữ liệu tháng
        InsightData data = dataCollector.collectForMonth(userId, monthStr);

        // 3. Kiểm tra xem có đủ dữ liệu chi tiêu không (tránh gọi AI lãng phí)
        if (data.getTotalExpense().compareTo(BigDecimal.ZERO) == 0) {
            return InsightResponse.builder()
                    .month(monthStr)
                    .content("Tháng này bạn chưa ghi chép giao dịch chi tiêu nào nên mình chưa thể đưa ra nhận xét. Hãy cập nhật giao dịch nhé!")
                    .isFromCache(false)
                    .build();
        }

        // 4. Gọi Gemini để sinh Insight
        User user = userRepository.findById(userId).orElseThrow();
        String formattedData = dataCollector.formatForPrompt(data);
        
        long startMs = System.currentTimeMillis();
        // Tăng lên 1000 tokens để thoải mái cho tiếng Việt
        String aiContent = geminiClient.chat(
                SYSTEM_PROMPT,
                "Dưới đây là tóm tắt tài chính của tôi trong tháng " + monthStr + ":\n" + formattedData,
                1000 
        );
        int latency = (int) (System.currentTimeMillis() - startMs);

        if (aiContent == null || aiContent.isBlank()) {
            callLogService.logFailure(user, "MONTHLY_INSIGHT", "Gemini returned empty response");
            return InsightResponse.builder()
                    .month(monthStr)
                    .content("Hệ thống AI đang bận một chút, mình chưa thể nhận xét lúc này. Bạn vui lòng thử lại sau nhé!")
                    .isFromCache(false)
                    .build();
        }

        // 5. Lưu vào Cache DB cho lần truy cập sau
        AiInsight insight = AiInsight.builder()
                .user(user)
                .month(monthDate)
                .insightType(InsightType.PERSONAL)
                .content(aiContent)
                .model("gemini-1.5-flash") 
                .build();
        
        AiInsight saved = insightRepository.save(insight);
        callLogService.logSuccess(user, "MONTHLY_INSIGHT", "gemini-1.5-flash", 0, 0, latency);
        log.info("Successfully generated and cached new insight for userId={}, month={}", userId, monthStr);

        return mapToResponse(saved, monthStr, false);
    }

    /**
     * Xóa cache cũ và yêu cầu AI generate lại insight mới.
     * Sử dụng khi người dùng vừa cập nhật dữ liệu và muốn AI đánh giá lại.
     */
    @Transactional
    public InsightResponse regenerateInsight(Long userId, String monthStr) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);
        insightRepository.deleteByUser_IdAndMonthAndInsightType(userId, monthDate, InsightType.PERSONAL);
        log.info("Force regenerating insight for userId={}, month={}", userId, monthStr);
        return getMonthlyInsight(userId, monthStr);
    }

    /**
     * Lấy insight tháng cho nhóm gia đình.
     */
    @Transactional
    public InsightResponse getFamilyInsight(Long groupId, String monthStr, User user) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);

        Optional<AiInsight> cached = insightRepository
                .findByGroupIdAndMonthAndInsightType(groupId, monthDate, InsightType.FAMILY);

        if (cached.isPresent()) {
            return mapToResponse(cached.get(), monthStr, true);
        }
        
        InsightData data = dataCollector.collectForGroup(groupId, monthStr);

        if (data.getTotalExpense().compareTo(BigDecimal.ZERO) == 0) {
            return InsightResponse.builder()
                    .month(monthStr)
                    .content("Nhóm gia đình chưa có chi tiêu chung nào trong tháng này.")
                    .isFromCache(false)
                    .build();
        }

        String formattedData = dataCollector.formatForPrompt(data);
        String aiContent = geminiClient.chat(
                FAMILY_SYSTEM_PROMPT,
                "Dữ liệu chi tiêu chung của gia đình tháng " + monthStr + ":\n" + formattedData,
                1000
        );

        AiInsight insight = AiInsight.builder()
                .groupId(groupId)
                .month(monthDate)
                .insightType(InsightType.FAMILY)
                .content(aiContent)
                .model("gemini-1.5-flash")
                .build();
        
        insightRepository.save(insight);
        return mapToResponse(insight, monthStr, false);
    }

    /**
     * Làm mới nhận xét AI cho nhóm gia đình.
     */
    @Transactional
    public InsightResponse regenerateFamilyInsight(Long groupId, String monthStr, User user) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);
        insightRepository.deleteByGroupIdAndMonthAndInsightType(groupId, monthDate, InsightType.FAMILY);
        log.info("Force regenerating family insight for groupId={}, month={}", groupId, monthStr);
        return getFamilyInsight(groupId, monthStr, user);
    }


    private InsightResponse mapToResponse(AiInsight insight, String month, boolean isFromCache) {
        return InsightResponse.builder()
                .month(month)
                .content(insight.getContent())
                .createdAt(insight.getCreatedAt())
                .isFromCache(isFromCache)
                .build();
    }

        
}
