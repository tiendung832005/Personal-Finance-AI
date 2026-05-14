package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.response.summary.CategoryBreakdownItem;
import com.data.personalfinanceinsightai.dto.response.summary.SummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.TrendItem;
import com.data.personalfinanceinsightai.dto.response.summary.TrendResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.SummaryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String email, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        YearMonth yearMonth = resolveYearMonth(month);
        String monthKey = yearMonth.toString();

        List<Object[]> typeSums = transactionRepository.sumAmountByTypeForUserAndMonth(user.getId(), monthKey);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Object[] row : typeSums) {
            TransactionType type = parseTransactionType(row[0]);
            BigDecimal total = toBigDecimal(row[1]);
            switch (type) {
                case INCOME -> totalIncome = totalIncome.add(total);
                case EXPENSE -> totalExpense = totalExpense.add(total);
            }
        }

        List<Object[]> rawBreakdown =
                transactionRepository.categoryBreakdownForUserAndMonth(user.getId(), monthKey);

        List<CategoryBreakdownItem> breakdown = new ArrayList<>();
        for (Object[] row : rawBreakdown) {
            Long categoryId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            CategoryType categoryType = CategoryType.valueOf(row[2].toString());
            BigDecimal total = toBigDecimal(row[3]);
            long txnCount = ((Number) row[4]).longValue();
            Double percentage = computeExpensePercentage(categoryType, total, totalExpense);
            breakdown.add(CategoryBreakdownItem.builder()
                    .categoryId(categoryId)
                    .categoryName(name)
                    .type(categoryType)
                    .total(total)
                    .percentage(percentage)
                    .transactionCount(txnCount)
                    .build());
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        BigDecimal currentBalance = accountRepository.sumBalanceForUser(user.getId());
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        return SummaryResponse.builder()
                .month(monthKey)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .currentBalance(currentBalance)
                .categoryBreakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TrendResponse getTrend(String email, Integer monthsParam) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int months = resolveTrendMonthCount(monthsParam);
        YearMonth end = YearMonth.now(clock);
        YearMonth start = end.minusMonths(months - 1L);
        LocalDate fromInclusive = start.atDay(1);
        LocalDate toInclusive = end.atEndOfMonth();

        List<Object[]> rows =
                transactionRepository.sumAmountByTypeGroupedByMonthRange(user.getId(), fromInclusive, toInclusive);

        Map<String, EnumMap<TransactionType, BigDecimal>> byMonth = new HashMap<>();
        for (Object[] row : rows) {
            String ym = String.valueOf(row[0]);
            TransactionType type = parseTransactionType(row[1]);
            BigDecimal total = toBigDecimal(row[2]);
            byMonth.computeIfAbsent(ym, k -> new EnumMap<>(TransactionType.class))
                    .merge(type, total, BigDecimal::add);
        }

        List<TrendItem> trend = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            String key = cursor.toString();
            EnumMap<TransactionType, BigDecimal> sums = byMonth.getOrDefault(key, new EnumMap<>(TransactionType.class));
            BigDecimal income = sums.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
            BigDecimal expense = sums.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            trend.add(TrendItem.builder()
                    .month(key)
                    .income(income)
                    .expense(expense)
                    .net(net)
                    .build());
        }

        return TrendResponse.builder().trend(trend).build();
    }

    private static int resolveTrendMonthCount(Integer monthsParam) {
        if (monthsParam == null || monthsParam < 1) {
            return 6;
        }
        return Math.min(monthsParam, 36);
    }

    private static Double computeExpensePercentage(
            CategoryType categoryType, BigDecimal lineTotal, BigDecimal totalExpense) {
        if (categoryType != CategoryType.EXPENSE) {
            return null;
        }
        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return lineTotal
                .multiply(BigDecimal.valueOf(100))
                .divide(totalExpense, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private YearMonth resolveYearMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(clock);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("month must be in format yyyy-MM");
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
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

    private static TransactionType parseTransactionType(Object value) {
        if (value instanceof TransactionType t) {
            return t;
        }
        return TransactionType.valueOf(value.toString());
    }
}
