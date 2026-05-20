package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.response.summary.CategoryBreakdownItem;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyMemberBreakdownResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilySummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyTrendResponse;
import com.data.personalfinanceinsightai.dto.response.summary.MemberSummaryItem;
import com.data.personalfinanceinsightai.dto.response.summary.TrendItem;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.FamilySummaryService;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
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
public class FamilySummaryServiceImpl implements FamilySummaryService {

    private final UserRepository userRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FamilySummaryResponse getGroupSummary(String email, Long groupId, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

        FamilyGroup group = familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));

        YearMonth yearMonth = resolveYearMonth(month);
        String monthKey = yearMonth.toString();

        List<Object[]> typeSums = transactionRepository.sumAmountByTypeForGroupAndMonth(groupId, monthKey);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Object[] row : typeSums) {
            TransactionType type = TransactionType.valueOf(row[0].toString());
            BigDecimal total = toBigDecimal(row[1]);
            switch (type) {
                case INCOME -> totalIncome = totalIncome.add(total);
                case EXPENSE -> totalExpense = totalExpense.add(total);
            }
        }

        List<Object[]> rawBreakdown =
                transactionRepository.categoryBreakdownForGroupAndMonth(groupId, monthKey);

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
        BigDecimal groupAccountBalance = sumGroupAccountBalances(groupId);

        return FamilySummaryResponse.builder()
                .month(monthKey)
                .groupId(groupId)
                .groupName(group.getName())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .groupAccountBalance(groupAccountBalance)
                .categoryBreakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyMemberBreakdownResponse getByMemberBreakdown(String email, Long groupId, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());
        familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));

        YearMonth yearMonth = resolveYearMonth(month);
        String monthKey = yearMonth.toString();

        List<MemberSummaryItem> members = transactionRepository
                .memberBreakdownForGroupAndMonth(groupId, monthKey)
                .stream()
                .map(this::mapMemberBreakdownRow)
                .toList();

        return FamilyMemberBreakdownResponse.builder()
                .month(monthKey)
                .groupId(groupId)
                .members(members)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyTrendResponse getGroupTrend(String email, Long groupId, Integer monthsParam) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());
        familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));

        int months = resolveTrendMonthCount(monthsParam);
        YearMonth end = YearMonth.now(clock);
        YearMonth start = end.minusMonths(months - 1L);
        LocalDate fromInclusive = start.atDay(1);
        LocalDate toInclusive = end.atEndOfMonth();

        List<Object[]> rows = transactionRepository.sumAmountByTypeGroupedByMonthRangeForGroup(
                groupId, fromInclusive, toInclusive);

        Map<String, EnumMap<TransactionType, BigDecimal>> byMonth = new HashMap<>();
        for (Object[] row : rows) {
            String ym = String.valueOf(row[0]);
            TransactionType type = TransactionType.valueOf(row[1].toString());
            BigDecimal total = toBigDecimal(row[2]);
            byMonth.computeIfAbsent(ym, k -> new EnumMap<>(TransactionType.class))
                    .merge(type, total, BigDecimal::add);
        }

        List<TrendItem> trend = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            String key = cursor.toString();
            EnumMap<TransactionType, BigDecimal> sums =
                    byMonth.getOrDefault(key, new EnumMap<>(TransactionType.class));
            BigDecimal income = sums.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
            BigDecimal expense = sums.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);
            trend.add(TrendItem.builder()
                    .month(key)
                    .income(income)
                    .expense(expense)
                    .net(income.subtract(expense))
                    .build());
        }

        return FamilyTrendResponse.builder().groupId(groupId).trend(trend).build();
    }

    private MemberSummaryItem mapMemberBreakdownRow(Object[] row) {
        Long userId = ((Number) row[0]).longValue();
        String fullName = (String) row[1];
        BigDecimal income = toBigDecimal(row[2]);
        BigDecimal expense = toBigDecimal(row[3]);
        long txnCount = ((Number) row[4]).longValue();
        return MemberSummaryItem.builder()
                .userId(userId)
                .fullName(fullName)
                .totalIncome(income)
                .totalExpense(expense)
                .netContribution(income.subtract(expense))
                .transactionCount(txnCount)
                .build();
    }

    private static int resolveTrendMonthCount(Integer monthsParam) {
        if (monthsParam == null || monthsParam < 1) {
            return 6;
        }
        return Math.min(monthsParam, 36);
    }

    private BigDecimal sumGroupAccountBalances(Long groupId) {
        List<Account> accounts =
                accountRepository.findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        groupId, AccountScope.SHARED);

        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            BigDecimal balance = accountRepository.calculateGroupAccountCurrentBalance(account.getId());
            total = total.add(balance != null ? balance : account.getBalance());
        }
        return total;
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
}
