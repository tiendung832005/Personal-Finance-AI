package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusResponse;
import com.data.personalfinanceinsightai.entity.Budget;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.exception.DuplicateBudgetException;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.BudgetRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
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
class SharedBudgetServiceTest {

    private static final Long GROUP_ID = 10L;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    private SharedBudgetServiceImpl sharedBudgetService;

    @BeforeEach
    void setUp() {
        sharedBudgetService = new SharedBudgetServiceImpl(
                budgetRepository,
                userRepository,
                categoryRepository,
                familyGroupRepository,
                groupAuthorizationService);
    }

    @Test
    void createGroupBudget_adminSuccess() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        Category category = Category.builder()
                .id(4L)
                .name("Ăn uống")
                .type(CategoryType.EXPENSE)
                .build();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).name("Gia đình").build()));
        when(categoryRepository.findVisibleByIdAndUserId(4L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByFamilyIdAndCategory_IdAndMonth(GROUP_ID, 4L, "2026-05"))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> {
                    Budget b = invocation.getArgument(0);
                    b.setId(99L);
                    return b;
                });

        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setCategoryId(4L);
        req.setAmount(new BigDecimal("3000000"));
        req.setMonth("2026-05");

        BudgetResponse response = sharedBudgetService.createGroupBudget("admin@test.com", GROUP_ID, req);

        assertThat(response.getAmount()).isEqualByComparingTo("3000000");
        ArgumentCaptor<Budget> cap = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(cap.capture());
        assertThat(cap.getValue().getUser()).isNull();
        assertThat(cap.getValue().getFamilyId()).isEqualTo(GROUP_ID);
        verify(groupAuthorizationService).requireAdmin(GROUP_ID, 1L);
    }

    @Test
    void createGroupBudget_memberForbidden() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        doThrow(new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này"))
                .when(groupAuthorizationService)
                .requireAdmin(GROUP_ID, 2L);

        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setCategoryId(4L);
        req.setAmount(new BigDecimal("3000000"));
        req.setMonth("2026-05");

        assertThatThrownBy(() -> sharedBudgetService.createGroupBudget("member@test.com", GROUP_ID, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createGroupBudget_duplicate() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        Category category = Category.builder().id(4L).name("Ăn uống").type(CategoryType.EXPENSE).build();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).build()));
        when(categoryRepository.findVisibleByIdAndUserId(4L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByFamilyIdAndCategory_IdAndMonth(GROUP_ID, 4L, "2026-05"))
                .thenReturn(true);

        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setCategoryId(4L);
        req.setAmount(new BigDecimal("3000000"));
        req.setMonth("2026-05");

        assertThatThrownBy(() -> sharedBudgetService.createGroupBudget("admin@test.com", GROUP_ID, req))
                .isInstanceOf(DuplicateBudgetException.class);
    }

    @Test
    void getBudgetStatus_calculatesFromSharedTxns() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(budgetRepository.aggregateGroupBudgetSpendingForMonth(GROUP_ID, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[] {1L, 4L, "Ăn uống", new BigDecimal("3000000"), new BigDecimal("500000")}));

        BudgetStatusResponse status =
                sharedBudgetService.getGroupBudgetStatus("member@test.com", GROUP_ID, "2026-05");

        assertThat(status.getItems()).hasSize(1);
        assertThat(status.getItems().get(0).getActualAmount()).isEqualByComparingTo("500000");
        verify(budgetRepository).aggregateGroupBudgetSpendingForMonth(eq(GROUP_ID), eq("2026-05"));
        verify(groupAuthorizationService).requireMember(GROUP_ID, 2L);
    }

    @Test
    void listGroupBudgets_memberCanView() {
        User member = User.builder().id(2L).email("member@test.com").build();
        Category category = Category.builder().id(4L).name("Ăn uống").type(CategoryType.EXPENSE).build();
        Budget budget = Budget.builder()
                .id(1L)
                .familyId(GROUP_ID)
                .category(category)
                .amount(new BigDecimal("3000000"))
                .month("2026-05")
                .build();

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(budgetRepository.findByFamilyIdAndMonthOrderByIdAsc(GROUP_ID, "2026-05"))
                .thenReturn(Collections.singletonList(budget));

        var list = sharedBudgetService.listGroupBudgets("member@test.com", GROUP_ID, "2026-05");

        assertThat(list).hasSize(1);
        verify(groupAuthorizationService).requireMember(GROUP_ID, 2L);
    }
}
