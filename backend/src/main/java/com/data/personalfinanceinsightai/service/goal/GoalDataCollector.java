package com.data.personalfinanceinsightai.service.goal;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalDataCollector {

    private static final int LOOKBACK_MONTHS = 3;

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public GoalAnalysisData collect(Long userId) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.now(clock);
        for (int i = 1; i <= LOOKBACK_MONTHS; i++) {
            String month = currentMonth.minusMonths(i).toString();
            totalIncome = totalIncome.add(orZero(transactionRepository.sumByUserTypeMonth(userId, "INCOME", month)));
            totalExpense = totalExpense.add(orZero(transactionRepository.sumByUserTypeMonth(userId, "EXPENSE", month)));
        }

        BigDecimal divisor = BigDecimal.valueOf(LOOKBACK_MONTHS);
        BigDecimal avgIncome = totalIncome.divide(divisor, 0, RoundingMode.HALF_UP);
        BigDecimal avgExpense = totalExpense.divide(divisor, 0, RoundingMode.HALF_UP);
        BigDecimal avgSavings = avgIncome.subtract(avgExpense);

        YearMonth fromMonth = currentMonth.minusMonths(LOOKBACK_MONTHS);
        YearMonth toMonth = currentMonth.minusMonths(1);
        List<GoalAnalysisData.CategorySpend> topExpenses = transactionRepository
                .getTopExpenseCategoriesBetween(
                        userId,
                        fromMonth.atDay(1),
                        toMonth.atEndOfMonth(),
                        5)
                .stream()
                .map(row -> GoalAnalysisData.CategorySpend.builder()
                        .categoryId(toLong(row[0]))
                        .categoryName((String) row[1])
                        .amount(toBigDecimal(row[2]))
                        .build())
                .toList();

        return GoalAnalysisData.builder()
                .avgMonthlyIncome(avgIncome)
                .avgMonthlyExpense(avgExpense)
                .avgMonthlySavings(avgSavings)
                .topExpenseCategories(topExpenses)
                .build();
    }

    public String formatForPrompt(
            GoalAnalysisData data,
            FinancialGoal goal,
            BigDecimal currentAmount,
            BigDecimal monthlyNeeded) {
        BigDecimal current = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        BigDecimal remaining = goal.getTargetAmount().subtract(current).max(BigDecimal.ZERO);
        long monthsLeft = Math.max(0, ChronoUnit.MONTHS.between(
                YearMonth.now(clock),
                YearMonth.from(goal.getDeadline())));

        StringBuilder topExpenseText = new StringBuilder();
        if (data.getTopExpenseCategories() == null || data.getTopExpenseCategories().isEmpty()) {
            topExpenseText.append("- Chua co du lieu chi tieu theo danh muc\n");
        } else {
            for (GoalAnalysisData.CategorySpend category : data.getTopExpenseCategories()) {
                topExpenseText
                        .append("- ")
                        .append(category.getCategoryName())
                        .append(": ")
                        .append(formatVnd(category.getAmount()))
                        .append("/3 thang\n");
            }
        }

        return """
                MUC TIEU:
                - Ten: %s
                - So tien can: %s
                - Da co: %s
                - Con thieu: %s
                - Deadline: %s (%d thang nua)
                - Can tiet kiem moi thang: %s

                THU CHI TRUNG BINH 3 THANG GAN NHAT:
                - Thu nhap: %s/thang
                - Chi tieu: %s/thang
                - Dang du: %s/thang

                TOP CHI TIEU 3 THANG GAN NHAT:
                %s
                """.formatted(
                goal.getName(),
                formatVnd(goal.getTargetAmount()),
                formatVnd(current),
                formatVnd(remaining),
                goal.getDeadline(),
                monthsLeft,
                formatVnd(monthlyNeeded),
                formatVnd(data.getAvgMonthlyIncome()),
                formatVnd(data.getAvgMonthlyExpense()),
                formatVnd(data.getAvgMonthlySavings()),
                topExpenseText.toString().trim());
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String formatVnd(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(safeAmount) + " VND";
    }
}
