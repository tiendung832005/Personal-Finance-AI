package com.data.personalfinanceinsightai.service.insight;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Data thu thập được của 1 tháng — được truyền vào prompt Gemini.
 * Kept as a simple value object (no JPA mapping).
 */
@Getter
@Builder
public class InsightData {

    /** Tháng dạng "yyyy-MM" */
    private String month;

    /** Tổng thu nhập tháng */
    private BigDecimal totalIncome;

    /** Tổng chi tiêu tháng */
    private BigDecimal totalExpense;

    /**
     * Tỷ lệ tiết kiệm (%) = (income - expense) / income * 100.
     * Âm nếu chi vượt thu.
     */
    private BigDecimal savingsRate;

    /** Thay đổi chi tiêu so tháng trước (dương = tăng, âm = giảm) */
    private BigDecimal expenseChangeFromLastMonth;

    /** Top danh mục chi nhiều nhất */
    private List<CategorySpending> topExpenses;

    /** Số category đã vượt budget */
    private int budgetExceededCount;

    /** Tổng số category có budget */
    private int totalBudgetCount;

    // ----------------------------------------------------------------

    @Getter
    @Builder
    public static class CategorySpending {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
    }

    @Getter
    @Builder
    public static class BudgetStatus {
        private Long categoryId;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal actualAmount;

        /** usedPercentage = actualAmount / budgetAmount * 100 */
        public double getUsedPercentage() {
            if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
            return actualAmount.divide(budgetAmount, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
    }
}
