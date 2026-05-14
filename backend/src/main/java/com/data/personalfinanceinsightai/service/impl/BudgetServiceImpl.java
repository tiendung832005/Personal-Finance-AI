package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.entity.Budget;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.exception.DuplicateBudgetException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.BudgetRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.BudgetService;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> listForUser(String email, String month) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String monthKey = normalizeMonthKey(month);
        return budgetRepository.findByUser_IdAndFamilyIdIsNullAndMonthOrderByIdAsc(user.getId(), monthKey).stream()
                .map(BudgetResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public BudgetResponse create(String email, BudgetCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String monthKey = normalizeMonthKey(request.getMonth());
        Category category = categoryRepository
                .findVisibleByIdAndUserId(request.getCategoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != CategoryType.EXPENSE && category.getType() != CategoryType.BOTH) {
            throw new IllegalArgumentException("Budget can only be set for expense categories");
        }
        if (budgetRepository.existsByUser_IdAndFamilyIdIsNullAndCategory_IdAndMonth(
                user.getId(), request.getCategoryId(), monthKey)) {
            throw new DuplicateBudgetException(
                    "Đã có ngân sách cho danh mục này trong tháng " + monthKey);
        }
        Budget budget = Budget.builder()
                .user(user)
                .familyId(null)
                .category(category)
                .amount(request.getAmount())
                .month(monthKey)
                .build();
        budgetRepository.save(budget);
        return BudgetResponse.fromEntity(budget);
    }

    @Override
    @Transactional
    public BudgetResponse update(String email, Long id, BudgetUpdateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Budget budget = budgetRepository
                .findByIdAndUser_IdAndFamilyIdIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budget.setAmount(request.getAmount());
        return BudgetResponse.fromEntity(budget);
    }

    @Override
    @Transactional
    public void delete(String email, Long id) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Budget budget = budgetRepository
                .findByIdAndUser_IdAndFamilyIdIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
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
