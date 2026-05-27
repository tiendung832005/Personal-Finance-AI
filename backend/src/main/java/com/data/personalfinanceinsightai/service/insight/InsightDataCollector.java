package com.data.personalfinanceinsightai.service.insight;

import com.data.personalfinanceinsightai.repository.BudgetRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.service.insight.InsightData.BudgetStatus;
import com.data.personalfinanceinsightai.service.insight.InsightData.CategorySpending;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * T06 — Thu thập và format data tháng cho Gemini prompt.
 *
 * <p>Chiến lược tiết kiệm token:
 * <ul>
 *   <li>Chỉ lấy Top 5 category chi nhiều nhất</li>
 *   <li>Format thành text ngắn (~100–150 tokens)</li>
 *   <li>Không truyền toàn bộ list transaction vào prompt</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightDataCollector {

    private final TransactionRepository txnRepo;
    private final BudgetRepository budgetRepo;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Thu thập data tháng, trả về InsightData đã tổng hợp.
     *
     * @param userId ID người dùng
     * @param month  chuỗi "yyyy-MM", ví dụ "2026-05"
     */
    public InsightData collectForMonth(Long userId, String month) {
        // 1. Tổng thu / chi tháng này
        BigDecimal totalIncome  = orZero(txnRepo.sumByUserTypeMonth(userId, "INCOME",  month));
        BigDecimal totalExpense = orZero(txnRepo.sumByUserTypeMonth(userId, "EXPENSE", month));
        BigDecimal savingsRate  = calculateSavingsRate(totalIncome, totalExpense);

        // 2. Top 5 category chi nhiều nhất
        List<CategorySpending> topExpenses = txnRepo
                .getTopExpenseCategories(userId, month, 5)
                .stream()
                .map(row -> CategorySpending.builder()
                        .categoryId(toLong(row[0]))
                        .categoryName((String) row[1])
                        .amount(toBigDecimal(row[2]))
                        .build())
                .collect(Collectors.toList());

        // 3. So sánh với tháng trước
        String lastMonth = YearMonth.parse(month).minusMonths(1).toString();
        BigDecimal lastExpense = orZero(txnRepo.sumByUserTypeMonth(userId, "EXPENSE", lastMonth));
        BigDecimal expenseChange = totalExpense.subtract(lastExpense);

        // 4. Budget compliance — bao nhiêu category vượt budget
        List<BudgetStatus> budgetStatuses = buildBudgetStatuses(userId, month);
        long exceededCount = budgetStatuses.stream()
                .filter(b -> b.getUsedPercentage() >= 100)
                .count();

        log.debug("InsightData collected: userId={}, month={}, income={}, expense={}",
                userId, month, totalIncome, totalExpense);

        return InsightData.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savingsRate(savingsRate)
                .topExpenses(topExpenses)
                .expenseChangeFromLastMonth(expenseChange)
                .budgetExceededCount((int) exceededCount)
                .totalBudgetCount(budgetStatuses.size())
                .build();
    }

    /**
     * Thu thập data tháng cho nhóm gia đình.
     */
    public InsightData collectForGroup(Long groupId, String month) {
        BigDecimal totalIncome  = orZero(txnRepo.sumAmountByTypeForGroupAndMonth(groupId, month).stream()
                .filter(row -> "INCOME".equals(row[0])).map(row -> toBigDecimal(row[1])).findFirst().orElse(BigDecimal.ZERO));
        BigDecimal totalExpense = orZero(txnRepo.sumAmountByTypeForGroupAndMonth(groupId, month).stream()
                .filter(row -> "EXPENSE".equals(row[0])).map(row -> toBigDecimal(row[1])).findFirst().orElse(BigDecimal.ZERO));
        BigDecimal savingsRate  = calculateSavingsRate(totalIncome, totalExpense);

        List<CategorySpending> topExpenses = txnRepo
                .getTopExpenseCategoriesForGroup(groupId, month, 5)
                .stream()
                .map(row -> CategorySpending.builder()
                        .categoryId(toLong(row[0]))
                        .categoryName((String) row[1])
                        .amount(toBigDecimal(row[2]))
                        .build())
                .collect(Collectors.toList());

        // Budget nhóm (áp dụng cho family_id)
        List<BudgetStatus> budgetStatuses = buildGroupBudgetStatuses(groupId, month);
        long exceededCount = budgetStatuses.stream()
                .filter(b -> b.getUsedPercentage() >= 100)
                .count();

        return InsightData.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savingsRate(savingsRate)
                .topExpenses(topExpenses)
                .budgetExceededCount((int) exceededCount)
                .totalBudgetCount(budgetStatuses.size())
                .build();
    }

    /**
     * Format InsightData thành text compact để đưa vào Gemini prompt.
     * Giữ ngắn (~100–150 tokens) để tiết kiệm quota.
     */
    public String formatForPrompt(InsightData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("Tháng ").append(data.getMonth()).append(":\n");
        sb.append("- Thu: ").append(formatVND(data.getTotalIncome())).append("\n");
        sb.append("- Chi: ").append(formatVND(data.getTotalExpense())).append("\n");
        sb.append("- Tiết kiệm: ").append(data.getSavingsRate()).append("%\n");

        BigDecimal change = data.getExpenseChangeFromLastMonth();
        if (change != null) {
            String trend = change.compareTo(BigDecimal.ZERO) >= 0 ? "tăng " : "giảm ";
            sb.append("- So tháng trước: chi ").append(trend)
              .append(formatVND(change.abs())).append("\n");
        }

        sb.append("- Top chi tiêu:\n");
        if (data.getTopExpenses() == null || data.getTopExpenses().isEmpty()) {
            sb.append("  (chưa có)\n");
        } else {
            data.getTopExpenses().forEach(c ->
                sb.append("  + ").append(c.getCategoryName()).append(": ")
                  .append(formatVND(c.getAmount())).append("\n")
            );
        }

        if (data.getTotalBudgetCount() > 0) {
            sb.append("- Budget: ").append(data.getBudgetExceededCount())
              .append("/").append(data.getTotalBudgetCount())
              .append(" danh mục vượt ngân sách\n");
        }

        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal expense) {
        if (income == null || income.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return income.subtract(expense)
                .divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private String formatVND(BigDecimal amount) {
        if (amount == null) return "0đ";
        return NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                .format(amount) + "đ";
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        return ((Number) val).longValue();
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }

    /**
     * Lấy danh sách BudgetStatus từ BudgetRepository.
     * Mỗi row trả về: [budgetId, categoryId, categoryName, budgetAmount, actualAmount]
     */
    private List<BudgetStatus> buildBudgetStatuses(Long userId, String month) {
        return budgetRepo.aggregateBudgetSpendingForMonth(userId, month)
                .stream()
                .map(row -> BudgetStatus.builder()
                        .categoryId(toLong(row[1]))
                        .categoryName((String) row[2])
                        .budgetAmount(toBigDecimal(row[3]))
                        .actualAmount(toBigDecimal(row[4]))
                        .build())
                .collect(Collectors.toList());
    }

    private List<BudgetStatus> buildGroupBudgetStatuses(Long groupId, String month) {
        return budgetRepo.aggregateGroupBudgetSpendingForMonth(groupId, month)
                .stream()
                .map(row -> BudgetStatus.builder()
                        .categoryId(toLong(row[1]))
                        .categoryName((String) row[2])
                        .budgetAmount(toBigDecimal(row[3]))
                        .actualAmount(toBigDecimal(row[4]))
                        .build())
                .collect(Collectors.toList());
    }
}
