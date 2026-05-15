package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusItem;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusResponse;
import com.data.personalfinanceinsightai.entity.Budget;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.BudgetLineStatus;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.exception.DuplicateBudgetException;
import com.data.personalfinanceinsightai.repository.BudgetRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetServiceImpl(budgetRepository, userRepository, categoryRepository);
    }

    @Test
    void createBudget_success() {
        User user = User.builder().id(1L).email("a@test.com").build();
        Category category = Category.builder()
                .id(10L)
                .name("Ăn uống")
                .type(CategoryType.EXPENSE)
                .build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUser_IdAndFamilyIdIsNullAndCategory_IdAndMonth(1L, 10L, "2026-05"))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> {
                    Budget b = invocation.getArgument(0);
                    b.setId(99L);
                    return b;
                });

        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setCategoryId(10L);
        req.setAmount(new BigDecimal("3000000"));
        req.setMonth("2026-05");

        BudgetResponse r = budgetService.create("a@test.com", req);

        assertThat(r.getId()).isEqualTo(99L);
        assertThat(r.getCategoryId()).isEqualTo(10L);
        assertThat(r.getCategoryName()).isEqualTo("Ăn uống");
        assertThat(r.getAmount()).isEqualByComparingTo("3000000");
        assertThat(r.getMonth()).isEqualTo("2026-05");

        ArgumentCaptor<Budget> cap = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(cap.capture());
        assertThat(cap.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(cap.getValue().getFamilyId()).isNull();
    }

    @Test
    void createBudget_duplicate() {
        User user = User.builder().id(1L).email("a@test.com").build();
        Category category = Category.builder()
                .id(10L)
                .name("Ăn uống")
                .type(CategoryType.EXPENSE)
                .build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUser_IdAndFamilyIdIsNullAndCategory_IdAndMonth(1L, 10L, "2026-05"))
                .thenReturn(true);

        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setCategoryId(10L);
        req.setAmount(new BigDecimal("1000"));
        req.setMonth("2026-05");

        assertThatThrownBy(() -> budgetService.create("a@test.com", req))
                .isInstanceOf(DuplicateBudgetException.class)
                .hasMessageContaining("2026-05");
    }

    @Test
    void getBudgetStatus_ok() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(new Object[] {1L, 4L, "Ăn uống", new BigDecimal("10000"), new BigDecimal("5000")}));

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2026-05");

        assertThat(r.getItems()).hasSize(1);
        BudgetStatusItem item = r.getItems().get(0);
        assertThat(item.getStatus()).isEqualTo(BudgetLineStatus.OK);
        assertThat(item.getUsedPercentage()).isCloseTo(50.0, within(0.05));
        assertThat(item.getActualAmount()).isEqualByComparingTo("5000");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("5000");
    }

    @Test
    void getBudgetStatus_warning() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(new Object[] {1L, 4L, "Ăn uống", new BigDecimal("10000"), new BigDecimal("8500")}));

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2026-05");

        BudgetStatusItem item = r.getItems().get(0);
        assertThat(item.getStatus()).isEqualTo(BudgetLineStatus.WARNING);
        assertThat(item.getUsedPercentage()).isCloseTo(85.0, within(0.05));
    }

    @Test
    void getBudgetStatus_warning83Percent() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(new Object[] {1L, 4L, "Ăn uống", new BigDecimal("100"), new BigDecimal("83")}));

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2026-05");

        assertThat(r.getItems().get(0).getStatus()).isEqualTo(BudgetLineStatus.WARNING);
        assertThat(r.getItems().get(0).getUsedPercentage()).isCloseTo(83.0, within(0.05));
    }

    @Test
    void getBudgetStatus_exceeded() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(new Object[] {1L, 5L, "Đi lại", new BigDecimal("1000"), new BigDecimal("1100")}));

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2026-05");

        BudgetStatusItem item = r.getItems().get(0);
        assertThat(item.getStatus()).isEqualTo(BudgetLineStatus.EXCEEDED);
        assertThat(item.getUsedPercentage()).isCloseTo(110.0, within(0.05));
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("-100");
    }

    @Test
    void getBudgetStatus_noSpending() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(new Object[] {1L, 8L, "Khác", new BigDecimal("10000"), BigDecimal.ZERO}));

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2026-05");

        BudgetStatusItem item = r.getItems().get(0);
        assertThat(item.getStatus()).isEqualTo(BudgetLineStatus.OK);
        assertThat(item.getActualAmount()).isEqualByComparingTo("0");
        assertThat(item.getUsedPercentage()).isEqualTo(0.0);
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void getBudgetStatus_emptyMonth() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(budgetRepository.aggregateBudgetSpendingForMonth(1L, "2030-01")).thenReturn(Collections.emptyList());

        BudgetStatusResponse r = budgetService.getBudgetStatus("a@test.com", "2030-01");

        assertThat(r.getItems()).isEmpty();
        assertThat(r.getTotalBudget()).isEqualByComparingTo("0");
        assertThat(r.getTotalActual()).isEqualByComparingTo("0");
        assertThat(r.getTotalRemaining()).isEqualByComparingTo("0");
        assertThat(r.getMonth()).isEqualTo("2030-01");
    }
}
