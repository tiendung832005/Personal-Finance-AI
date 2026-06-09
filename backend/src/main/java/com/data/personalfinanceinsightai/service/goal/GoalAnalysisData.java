package com.data.personalfinanceinsightai.service.goal;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalAnalysisData {

    private BigDecimal avgMonthlyIncome;
    private BigDecimal avgMonthlyExpense;
    private BigDecimal avgMonthlySavings;
    private List<CategorySpend> topExpenseCategories;

    @Getter
    @Builder
    public static class CategorySpend {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
    }
}
