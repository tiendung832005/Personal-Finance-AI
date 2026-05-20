package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusItem;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusResponse;
import com.data.personalfinanceinsightai.entity.Budget;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.BudgetLineStatus;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.exception.DuplicateBudgetException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.BudgetRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.SharedBudgetService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedBudgetServiceImpl implements SharedBudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    @Override
    @Transactional
    public BudgetResponse createGroupBudget(String email, Long groupId, BudgetCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, user.getId());
        familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));

        String monthKey = normalizeMonthKey(request.getMonth());
        Category category = categoryRepository
                .findVisibleByIdAndUserId(request.getCategoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != CategoryType.EXPENSE && category.getType() != CategoryType.BOTH) {
            throw new IllegalArgumentException("Budget can only be set for expense categories");
        }
        if (budgetRepository.existsByFamilyIdAndCategory_IdAndMonth(groupId, request.getCategoryId(), monthKey)) {
            throw new DuplicateBudgetException(
                    "Đã có ngân sách cho danh mục này trong tháng " + monthKey);
        }

        Budget budget = Budget.builder()
                .user(null)
                .familyId(groupId)
                .category(category)
                .amount(request.getAmount())
                .month(monthKey)
                .build();

        return BudgetResponse.fromEntity(budgetRepository.save(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> listGroupBudgets(String email, Long groupId, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());
        String monthKey = normalizeMonthKey(month);

        return budgetRepository.findByFamilyIdAndMonthOrderByIdAsc(groupId, monthKey).stream()
                .map(BudgetResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetStatusResponse getGroupBudgetStatus(String email, Long groupId, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());
        String monthKey = normalizeMonthKey(month);

        List<Object[]> rows = budgetRepository.aggregateGroupBudgetSpendingForMonth(groupId, monthKey);
        return buildBudgetStatusResponse(monthKey, rows);
    }

    @Override
    @Transactional
    public BudgetResponse updateGroupBudget(
            String email, Long groupId, Long budgetId, BudgetUpdateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, user.getId());

        Budget budget = budgetRepository
                .findByIdAndFamilyId(budgetId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        budget.setAmount(request.getAmount());
        return BudgetResponse.fromEntity(budgetRepository.save(budget));
    }

    @Override
    @Transactional
    public void deleteGroupBudget(String email, Long groupId, Long budgetId) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, user.getId());

        Budget budget = budgetRepository
                .findByIdAndFamilyId(budgetId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        budgetRepository.delete(budget);
    }

    private static BudgetStatusResponse buildBudgetStatusResponse(String monthKey, List<Object[]> rows) {
        List<BudgetStatusItem> items = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        for (Object[] row : rows) {
            Long categoryId = ((Number) row[1]).longValue();
            String categoryName = (String) row[2];
            BigDecimal budgetAmount = toBigDecimal(row[3]);
            BigDecimal actualAmount = toBigDecimal(row[4]);

            totalBudget = totalBudget.add(budgetAmount);
            totalActual = totalActual.add(actualAmount);

            BigDecimal remaining = budgetAmount.subtract(actualAmount);
            double usedPct = computeUsedPercentage(budgetAmount, actualAmount);
            BudgetLineStatus status = BudgetServiceImpl.resolveStatus(usedPct);

            items.add(BudgetStatusItem.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .budgetAmount(budgetAmount)
                    .actualAmount(actualAmount)
                    .remainingAmount(remaining)
                    .usedPercentage(usedPct)
                    .status(status)
                    .build());
        }

        return BudgetStatusResponse.builder()
                .month(monthKey)
                .totalBudget(totalBudget)
                .totalActual(totalActual)
                .totalRemaining(totalBudget.subtract(totalActual))
                .items(items)
                .build();
    }

    private static double computeUsedPercentage(BigDecimal budgetAmount, BigDecimal actualAmount) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return actualAmount
                .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
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

    private static String normalizeMonthKey(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("month is required (yyyy-MM)");
        }
        try {
            return YearMonth.parse(month.trim()).toString();
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("month must be in format yyyy-MM");
        }
    }
}
