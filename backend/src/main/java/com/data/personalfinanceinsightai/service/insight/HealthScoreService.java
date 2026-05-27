package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.dto.response.insight.HealthScoreResponse;
import com.data.personalfinanceinsightai.dto.response.insight.HealthScoreResponse.ScoreBreakdownItem;
import com.data.personalfinanceinsightai.entity.FinancialHealthScore;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.ScoreLabel;
import com.data.personalfinanceinsightai.repository.FinancialHealthScoreRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.data.personalfinanceinsightai.service.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T13 — HealthScoreService: Tính toán điểm sức khỏe tài chính (0-100).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreService {

    private final FinancialHealthScoreRepository healthScoreRepository;
    private final InsightDataCollector dataCollector;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    /**
     * Lấy hoặc tính toán điểm sức khỏe tài chính của tháng.
     */
    @Transactional
    public HealthScoreResponse getHealthScore(Long userId, String monthStr) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);

        // 1. Kiểm tra xem đã tính điểm tháng này chưa
        // 1. Kiểm tra cache (Bỏ qua cache nếu scoreLabel hoặc aiAnalysis bị thiếu - phục vụ migration)
        Optional<FinancialHealthScore> existing = healthScoreRepository
                .findByUser_IdAndMonth(userId, monthDate);
        
        if (existing.isPresent() && existing.get().getAiAnalysis() != null) {
            return mapToResponse(existing.get(), monthStr);
        }

        // 2. Thu thập dữ liệu để tính toán và lưu
        InsightData data = dataCollector.collectForMonth(userId, monthStr);
        return calculateAndSave(userId, monthStr, monthDate, data);
    }

    @Transactional
    public HealthScoreResponse regenerateHealthScore(Long userId, String monthStr) {
        LocalDate monthDate = YearMonth.parse(monthStr).atDay(1);
        
        // Luôn tính toán lại, bỏ qua cache
        InsightData data = dataCollector.collectForMonth(userId, monthStr);
        return calculateAndSave(userId, monthStr, monthDate, data);
    }

    private HealthScoreResponse calculateAndSave(Long userId, String monthStr, LocalDate monthDate, InsightData data) {
        // 3. Thực hiện tính điểm theo các tiêu chí
        int savingsScore = calculateSavingsScore(data.getSavingsRate(), data.getTotalIncome(), data.getTotalExpense());
        int budgetScore = calculateBudgetScore(data.getBudgetExceededCount(), data.getTotalBudgetCount());
        int trendScore = calculateTrendScore(data.getExpenseChangeFromLastMonth(), data.getTotalExpense());

        int overallScore = savingsScore + budgetScore + trendScore;
        ScoreLabel label = determineLabel(overallScore);

        // 4. Gọi AI để phân tích điểm và đưa ra gợi ý tiết kiệm
        String aiAnalysis = generateAiAnalysis(overallScore, savingsScore, budgetScore, trendScore, data);
        String savingsTips = generateAiSavingsTips(data);

        // 5. Lưu vào DB (Tìm bản ghi cũ để update hoặc tạo mới)
        User user = userRepository.findById(userId).orElseThrow();
        FinancialHealthScore scoreEntity = healthScoreRepository
                .findByUser_IdAndMonth(userId, monthDate)
                .orElse(new FinancialHealthScore());
        
        scoreEntity.setUser(user);
        scoreEntity.setMonth(monthDate);
        scoreEntity.setOverallScore(overallScore);
        scoreEntity.setSavingsScore(savingsScore);
        scoreEntity.setBudgetScore(budgetScore);
        scoreEntity.setSpendingTrendScore(trendScore);
        scoreEntity.setScoreLabel(label);
        scoreEntity.setAiAnalysis(aiAnalysis);
        scoreEntity.setSavingsTips(savingsTips);

        FinancialHealthScore saved = healthScoreRepository.save(scoreEntity);
        log.info("Regenerated health score for userId={}, month={}: score={}", userId, monthStr, overallScore);

        return mapToResponse(saved, monthStr);
    }

    private int calculateSavingsScore(BigDecimal savingsRate, BigDecimal income, BigDecimal expense) {
        // Fix Bug: Nếu Chi > Thu, điểm tiết kiệm phải bằng 0 bất kể tỷ lệ
        if (income.compareTo(expense) <= 0) return 0;
        
        if (savingsRate == null || savingsRate.compareTo(BigDecimal.ZERO) <= 0) return 0;
        
        double rate = savingsRate.doubleValue();
        if (rate >= 25.0) return 50;
        return (int) (rate * 2); 
    }

    private int calculateBudgetScore(int exceeded, int total) {
        // Tuân thủ ngân sách đóng góp tối đa 30 điểm
        if (total == 0) return 20; // Default điểm nếu không đặt budget
        
        double compliance = (double) (total - exceeded) / total;
        return (int) (compliance * 30);
    }

    private int calculateTrendScore(BigDecimal change, BigDecimal currentExpense) {
        // Xu hướng chi tiêu đóng góp tối đa 20 điểm
        if (change == null || currentExpense == null || currentExpense.compareTo(BigDecimal.ZERO) == 0) return 10;

        // Nếu chi tiêu giảm so với tháng trước -> 20đ
        if (change.compareTo(BigDecimal.ZERO) <= 0) return 20;

        // Nếu chi tiêu tăng: tăng < 10% -> 10đ | tăng > 30% -> 0đ
        double increasePercent = change.multiply(BigDecimal.valueOf(100))
                .divide(currentExpense.subtract(change).abs().add(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        if (increasePercent < 10) return 10;
        if (increasePercent < 30) return 5;
        return 0;
    }

    private ScoreLabel determineLabel(int score) {
        if (score >= 81) return ScoreLabel.EXCELLENT;
        if (score >= 61) return ScoreLabel.GOOD;
        if (score >= 41) return ScoreLabel.FAIR;
        return ScoreLabel.POOR;
    }

    private String generateAiAnalysis(int overall, int savings, int budget, int trend, InsightData data) {
        String prompt = String.format("""
                Hãy phân tích điểm sức khỏe tài chính của tôi (thang điểm 100).
                - Điểm tổng: %d/100
                - Các thành phần: Tiết kiệm (%d/50), Ngân sách (%d/30), Xu hướng chi tiêu (%d/20).
                - Dữ liệu thực tế: Thu nhập %s, Chi tiêu %s, Tỷ lệ tiết kiệm %s%%.
                
                Yêu cầu: Viết 2 câu ngắn gọn giải thích tại sao tôi nhận được mức điểm này. Thân thiện và khách quan.
                """, overall, savings, budget, trend, data.getTotalIncome(), data.getTotalExpense(), data.getSavingsRate());
        
        return geminiClient.chat("Bạn là chuyên gia phân tích điểm tài chính.", prompt, 300);
    }

    private String generateAiSavingsTips(InsightData data) {
        String topExpensesStr = data.getTopExpenses().stream()
                .map(c -> c.getCategoryName() + " (" + c.getAmount() + ")")
                .collect(Collectors.joining(", "));

        String prompt = String.format("""
                Dựa trên dữ liệu chi tiêu này, hãy đưa ra 4 gợi ý tiết kiệm NGẮN GỌN và THỰC TẾ.
                - Top chi tiêu: %s
                - Các mục vượt ngân sách: %d/%d
                
                Định dạng kết quả trả về BẮT BUỘC theo cấu trúc JSON mảng các object:
                [{"title": "Tiêu đề", "description": "Mô tả ngắn"}]
                """, topExpensesStr, data.getBudgetExceededCount(), data.getTotalBudgetCount());

        return cleanAiResponse(geminiClient.chat("Bạn là chuyên gia tư vấn tiết kiệm. Trả về JSON.", prompt, 600));
    }

    private String cleanAiResponse(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "");
        }
        return cleaned.trim();
    }

    private HealthScoreResponse mapToResponse(FinancialHealthScore entity, String month) {
        List<ScoreBreakdownItem> breakdown = new ArrayList<>();
        
        breakdown.add(ScoreBreakdownItem.builder()
                .label("Tỷ lệ tiết kiệm")
                .score(entity.getSavingsScore())
                .maxScore(50)
                .description("Dựa trên phần trăm thu nhập bạn giữ lại được.")
                .build());

        breakdown.add(ScoreBreakdownItem.builder()
                .label("Tuân thủ ngân sách")
                .score(entity.getBudgetScore())
                .maxScore(30)
                .description("Đánh giá việc bạn giữ chi tiêu trong giới hạn đã đặt ra.")
                .build());

        breakdown.add(ScoreBreakdownItem.builder()
                .label("Xu hướng chi tiêu")
                .score(entity.getSpendingTrendScore())
                .maxScore(20)
                .description("So sánh mức chi tiêu của bạn với tháng trước.")
                .build());

        return HealthScoreResponse.builder()
                .month(month)
                .overallScore(entity.getOverallScore())
                .savingsScore(entity.getSavingsScore())
                .budgetScore(entity.getBudgetScore())
                .spendingTrendScore(entity.getSpendingTrendScore())
                .scoreLabel(entity.getScoreLabel().name())
                .aiAnalysis(entity.getAiAnalysis())
                .savingsTips(entity.getSavingsTips())
                .breakdown(breakdown)
                .build();
    }
}
