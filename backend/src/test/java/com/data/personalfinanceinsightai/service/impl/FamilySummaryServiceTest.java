package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.response.summary.FamilyMemberBreakdownResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilySummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyTrendResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.AccountType;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilySummaryServiceTest {

    private static final Long GROUP_ID = 10L;
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    private FamilySummaryServiceImpl familySummaryService;

    @BeforeEach
    void setUp() {
        familySummaryService = new FamilySummaryServiceImpl(
                userRepository,
                familyGroupRepository,
                transactionRepository,
                accountRepository,
                groupAuthorizationService,
                FIXED_CLOCK);
    }

    @Test
    void getGroupSummary_onlySharedTransactions() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).name("Gia đình Nguyễn").build()));
        when(transactionRepository.sumAmountByTypeForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[] {"INCOME", new BigDecimal("5000000")},
                        new Object[] {"EXPENSE", new BigDecimal("2750000")}));
        when(transactionRepository.categoryBreakdownForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Collections.emptyList());
        when(accountRepository.findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        GROUP_ID, AccountScope.SHARED))
                .thenReturn(Collections.emptyList());

        FamilySummaryResponse summary =
                familySummaryService.getGroupSummary("member@test.com", GROUP_ID, "2026-05");

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("5000000");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("2750000");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("2250000");
        assertThat(summary.getGroupName()).isEqualTo("Gia đình Nguyễn");
    }

    @Test
    void getGroupSummary_groupAccountBalance() {
        User member = User.builder().id(2L).email("member@test.com").build();
        Account shared = Account.builder()
                .id(5L)
                .familyId(GROUP_ID)
                .scope(AccountScope.SHARED)
                .name("Quỹ gia đình")
                .type(AccountType.CASH)
                .balance(new BigDecimal("10000000"))
                .currency("VND")
                .user(member)
                .build();

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).name("Gia đình").build()));
        when(transactionRepository.sumAmountByTypeForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.categoryBreakdownForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Collections.emptyList());
        when(accountRepository.findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        GROUP_ID, AccountScope.SHARED))
                .thenReturn(List.of(shared));
        when(accountRepository.calculateGroupAccountCurrentBalance(5L))
                .thenReturn(new BigDecimal("12250000"));

        FamilySummaryResponse summary =
                familySummaryService.getGroupSummary("member@test.com", GROUP_ID, "2026-05");

        assertThat(summary.getGroupAccountBalance()).isEqualByComparingTo("12250000");
    }

    @Test
    void getByMemberBreakdown_correctGrouping() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).name("Gia đình").build()));
        when(transactionRepository.memberBreakdownForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[] {
                            1L, "Nguyen Van A", new BigDecimal("4000000"), new BigDecimal("1500000"), 3L
                        },
                        new Object[] {
                            2L, "Nguyen Van B", new BigDecimal("1000000"), new BigDecimal("1250000"), 5L
                        }));

        FamilyMemberBreakdownResponse response =
                familySummaryService.getByMemberBreakdown("member@test.com", GROUP_ID, "2026-05");

        assertThat(response.getMembers()).hasSize(2);

        assertThat(response.getMembers().get(0).getUserId()).isEqualTo(1L);
        assertThat(response.getMembers().get(0).getTransactionCount()).isEqualTo(3);
        assertThat(response.getMembers().get(0).getTotalIncome()).isEqualByComparingTo("4000000");
        assertThat(response.getMembers().get(0).getTotalExpense()).isEqualByComparingTo("1500000");
        assertThat(response.getMembers().get(0).getNetContribution()).isEqualByComparingTo("2500000");

        assertThat(response.getMembers().get(1).getUserId()).isEqualTo(2L);
        assertThat(response.getMembers().get(1).getTransactionCount()).isEqualTo(5);
        assertThat(response.getMembers().get(1).getNetContribution()).isEqualByComparingTo("-250000");

        verify(transactionRepository).memberBreakdownForGroupAndMonth(eq(GROUP_ID), eq("2026-05"));
    }

    @Test
    void getByMemberBreakdown_excludesMembersWithoutTransactions() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).build()));
        when(transactionRepository.memberBreakdownForGroupAndMonth(GROUP_ID, "2026-05"))
                .thenReturn(Collections.singletonList(
                        new Object[] {2L, "Nguyen Van B", BigDecimal.ZERO, new BigDecimal("200000"), 1L}));

        FamilyMemberBreakdownResponse response =
                familySummaryService.getByMemberBreakdown("member@test.com", GROUP_ID, "2026-05");

        assertThat(response.getMembers()).hasSize(1);
        assertThat(response.getMembers().get(0).getUserId()).isEqualTo(2L);
    }

    @Test
    void getGroupTrend_emptyMonthReturnsZero() {
        User member = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).build()));
        when(transactionRepository.sumAmountByTypeGroupedByMonthRangeForGroup(
                        eq(GROUP_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        FamilyTrendResponse response =
                familySummaryService.getGroupTrend("member@test.com", GROUP_ID, 6);

        assertThat(response.getTrend()).hasSize(6);
        assertThat(response.getGroupId()).isEqualTo(GROUP_ID);
        response.getTrend().forEach(item -> {
            assertThat(item.getIncome()).isEqualByComparingTo("0");
            assertThat(item.getExpense()).isEqualByComparingTo("0");
            assertThat(item.getNet()).isEqualByComparingTo("0");
        });
        assertThat(response.getTrend().get(0).getMonth()).isEqualTo("2025-12");
        assertThat(response.getTrend().get(5).getMonth()).isEqualTo("2026-05");
    }

    @Test
    void getGroupSummary_nonMemberForbidden() {
        User outsider = User.builder().id(99L).email("outsider@test.com").build();
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        doThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"))
                .when(groupAuthorizationService)
                .requireMember(GROUP_ID, 99L);

        assertThatThrownBy(() -> familySummaryService.getGroupSummary("outsider@test.com", GROUP_ID, "2026-05"))
                .isInstanceOf(ForbiddenException.class);
    }
}
