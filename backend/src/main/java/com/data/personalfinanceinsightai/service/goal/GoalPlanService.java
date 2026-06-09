package com.data.personalfinanceinsightai.service.goal;

import com.data.personalfinanceinsightai.dto.response.goal.GoalPlanResponse;
import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.FinancialGoalRepository;
import com.data.personalfinanceinsightai.service.GeminiClient;
import com.data.personalfinanceinsightai.service.GoalProgressCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalPlanService {

    private static final int CACHE_DAYS = 7;
    private static final int MAX_PLAN_TOKENS = 500;

    private static final String SYSTEM_PROMPT = """
            Ban la chuyen gia tu van tai chinh ca nhan tai Viet Nam.
            Dua vao du lieu thu chi thuc te va muc tieu tai chinh cua user,
            hay viet ke hoach bang tieng Viet tu nhien trong 4-6 cau.
            Bat buoc co con so cu the, gom: danh gia kha thi, so tien can tiet kiem moi thang,
            khoang thieu so voi dong tien hien tai, goi y cat giam 1-3 danh muc neu can,
            va deadline thuc te neu muc tieu qua gap.
            Khong dua loi khuyen dau tu rui ro cao. Khong tra ve JSON hoac markdown bang.
            """;

    private final GeminiClient geminiClient;
    private final GoalDataCollector dataCollector;
    private final GoalProgressCalculator progressCalculator;
    private final FinancialGoalRepository goalRepository;
    private final Clock clock;

    @Transactional
    public GoalPlanResponse getOrGeneratePlan(Long goalId, Long userId) {
        FinancialGoal goal = findGoal(goalId, userId);
        if (isFreshCache(goal)) {
            return GoalPlanResponse.builder()
                    .plan(goal.getAiPlan())
                    .generatedAt(goal.getAiPlanGeneratedAt())
                    .isFromCache(true)
                    .build();
        }
        return generateAndSavePlan(goal, userId);
    }

    @Transactional
    public GoalPlanResponse regeneratePlan(Long goalId, Long userId) {
        FinancialGoal goal = findGoal(goalId, userId);
        return generateAndSavePlan(goal, userId);
    }

    private GoalPlanResponse generateAndSavePlan(FinancialGoal goal, Long userId) {
        BigDecimal currentAmount = progressCalculator.calculateCurrentAmount(goal);
        BigDecimal safeCurrentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        BigDecimal monthlyNeeded = progressCalculator.calculateMonthlyNeeded(goal, currentAmount);
        GoalAnalysisData data = dataCollector.collect(userId);
        String promptData = dataCollector.formatForPrompt(data, goal, safeCurrentAmount, monthlyNeeded);

        long startedAt = System.currentTimeMillis();
        String plan = geminiClient.chat(SYSTEM_PROMPT, promptData, MAX_PLAN_TOKENS);
        long latencyMs = System.currentTimeMillis() - startedAt;
        if (plan == null || plan.isBlank()) {
            log.warn("Goal plan Gemini failed, using fallback: goalId={}, userId={}", goal.getId(), userId);
            plan = generateFallbackPlan(goal, safeCurrentAmount, monthlyNeeded, data);
        } else {
            log.info("Generated goal plan: goalId={}, userId={}, latency={}ms", goal.getId(), userId, latencyMs);
        }

        LocalDateTime generatedAt = LocalDateTime.now(clock);
        goal.setAiPlan(plan.trim());
        goal.setAiPlanGeneratedAt(generatedAt);
        goalRepository.save(goal);

        return GoalPlanResponse.builder()
                .plan(goal.getAiPlan())
                .generatedAt(generatedAt)
                .isFromCache(false)
                .build();
    }

    private FinancialGoal findGoal(Long goalId, Long userId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private boolean isFreshCache(FinancialGoal goal) {
        if (goal.getAiPlan() == null || goal.getAiPlan().isBlank() || goal.getAiPlanGeneratedAt() == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(CACHE_DAYS);
        return goal.getAiPlanGeneratedAt().isAfter(cutoff);
    }

    private String generateFallbackPlan(
            FinancialGoal goal,
            BigDecimal current,
            BigDecimal monthlyNeeded,
            GoalAnalysisData data) {
        BigDecimal avgSavings = data.getAvgMonthlySavings() == null ? BigDecimal.ZERO : data.getAvgMonthlySavings();
        BigDecimal remaining = goal.getTargetAmount().subtract(current).max(BigDecimal.ZERO);
        long monthsLeft = Math.max(0, ChronoUnit.MONTHS.between(
                YearMonth.now(clock),
                YearMonth.from(goal.getDeadline())));
        BigDecimal gap = monthlyNeeded.subtract(avgSavings);

        String feasibility;
        if (monthlyNeeded.compareTo(BigDecimal.ZERO) == 0) {
            feasibility = "Bạn đã đạt hoặc vượt số tiền mục tiêu hiện tại.";
        } else if (avgSavings.compareTo(monthlyNeeded) >= 0) {
            feasibility = "Mục tiêu này đang khả thi với dòng tiền hiện tại.";
        } else if (avgSavings.compareTo(BigDecimal.ZERO) > 0) {
            feasibility = "Mục tiêu này hơi căng vì số tiền dư hàng tháng chưa đủ.";
        } else {
            feasibility = "Mục tiêu này chưa khả thi nếu không giảm chi hoặc tăng thu nhập.";
        }

        String cutSuggestion = buildCutSuggestion(data, gap);
        String realisticDeadline = "";
        if (avgSavings.compareTo(BigDecimal.ZERO) > 0 && avgSavings.compareTo(monthlyNeeded) < 0) {
            BigDecimal realisticMonths = remaining.divide(avgSavings, 0, RoundingMode.CEILING);
            realisticDeadline = " Với mức dư hiện tại, thời gian thực tế khoảng "
                    + realisticMonths.longValue()
                    + " tháng nếu không thay đổi chi tiêu.";
        }

        return feasibility
                + " Bạn cần thêm " + formatVnd(remaining) + " cho mục tiêu \"" + goal.getName()
                + "\" trong " + monthsLeft + " tháng, tương đương khoảng " + formatVnd(monthlyNeeded) + "/tháng."
                + " Thu chi 3 tháng gần nhất cho thấy bạn đang dư trung bình " + formatVnd(avgSavings) + "/tháng."
                + cutSuggestion
                + realisticDeadline;
    }

    private String buildCutSuggestion(GoalAnalysisData data, BigDecimal gap) {
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return " Bạn nên chuyển khoản vào tài khoản mục tiêu ngay khi nhận thu nhập để giữ nhịp tiết kiệm.";
        }
        if (data.getTopExpenseCategories() == null || data.getTopExpenseCategories().isEmpty()) {
            return " Bạn cần giảm thêm khoảng " + formatVnd(gap)
                    + "/tháng hoặc tăng thu nhập để theo kịp kế hoạch.";
        }

        StringBuilder builder = new StringBuilder(" Bạn cần bù thêm khoảng ")
                .append(formatVnd(gap))
                .append("/tháng; ưu tiên rà soát ");
        int limit = Math.min(2, data.getTopExpenseCategories().size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(" và ");
            }
            GoalAnalysisData.CategorySpend category = data.getTopExpenseCategories().get(i);
            BigDecimal monthlyAverage = category.getAmount() == null
                    ? BigDecimal.ZERO
                    : category.getAmount().divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
            BigDecimal suggestedCut = monthlyAverage
                    .multiply(BigDecimal.valueOf(0.15))
                    .setScale(0, RoundingMode.HALF_UP);
            builder.append(category.getCategoryName()).append(" khoảng ").append(formatVnd(suggestedCut));
        }
        builder.append("/tháng.");
        return builder.toString();
    }

    private String formatVnd(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(safeAmount) + " VND";
    }
}
