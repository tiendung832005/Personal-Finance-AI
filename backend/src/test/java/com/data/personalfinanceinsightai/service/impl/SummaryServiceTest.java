package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.response.summary.CategoryBreakdownItem;
import com.data.personalfinanceinsightai.dto.response.summary.SummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.TrendItem;
import com.data.personalfinanceinsightai.dto.response.summary.TrendResponse;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
class SummaryServiceTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private Clock clock;
    private SummaryServiceImpl summaryService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(ZonedDateTime.of(2026, 5, 14, 12, 0, 0, 0, APP_ZONE).toInstant(), APP_ZONE);
        summaryService = new SummaryServiceImpl(transactionRepository, accountRepository, userRepository, clock);
    }

    @Test
    void getSummary_correctTotals() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(1L, "2026-05"))
                .thenReturn(Arrays.asList(
                        new Object[] {TransactionType.INCOME, new BigDecimal("15500000")},
                        new Object[] {TransactionType.EXPENSE, new BigDecimal("1575000")}));
        when(transactionRepository.categoryBreakdownForUserAndMonth(1L, "2026-05"))
                .thenReturn(Collections.emptyList());
        when(accountRepository.sumBalanceForUser(1L)).thenReturn(new BigDecimal("28805000"));

        SummaryResponse r = summaryService.getSummary("a@test.com", "2026-05");

        assertThat(r.getMonth()).isEqualTo("2026-05");
        assertThat(r.getTotalIncome()).isEqualByComparingTo("15500000");
        assertThat(r.getTotalExpense()).isEqualByComparingTo("1575000");
        assertThat(r.getNetBalance()).isEqualByComparingTo("13925000");
        assertThat(r.getCurrentBalance()).isEqualByComparingTo("28805000");
    }

    @Test
    void getSummary_emptyMonth() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(1L, "2030-01")).thenReturn(Collections.emptyList());
        when(transactionRepository.categoryBreakdownForUserAndMonth(1L, "2030-01")).thenReturn(Collections.emptyList());
        when(accountRepository.sumBalanceForUser(1L)).thenReturn(BigDecimal.ZERO);

        SummaryResponse r = summaryService.getSummary("a@test.com", "2030-01");

        assertThat(r.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(r.getTotalExpense()).isEqualByComparingTo("0");
        assertThat(r.getNetBalance()).isEqualByComparingTo("0");
        assertThat(r.getCurrentBalance()).isEqualByComparingTo("0");
        assertThat(r.getCategoryBreakdown()).isEmpty();
    }

    @Test
    void getSummary_defaultCurrentMonth() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(1L, "2026-05")).thenReturn(Collections.emptyList());
        when(transactionRepository.categoryBreakdownForUserAndMonth(1L, "2026-05")).thenReturn(Collections.emptyList());
        when(accountRepository.sumBalanceForUser(1L)).thenReturn(BigDecimal.ZERO);

        summaryService.getSummary("a@test.com", null);

        verify(transactionRepository).sumAmountByTypeForUserAndMonth(1L, "2026-05");
        verify(transactionRepository).categoryBreakdownForUserAndMonth(1L, "2026-05");
    }

    @Test
    void getSummary_percentageCalculation() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(1L, "2026-05"))
                .thenReturn(Arrays.asList(
                        new Object[] {TransactionType.INCOME, new BigDecimal("1000")},
                        new Object[] {TransactionType.EXPENSE, new BigDecimal("1000")}));
        when(transactionRepository.categoryBreakdownForUserAndMonth(1L, "2026-05"))
                .thenReturn(Arrays.asList(
                        new Object[] {1L, "A", CategoryType.EXPENSE.name(), new BigDecimal("600"), 2L},
                        new Object[] {2L, "B", CategoryType.EXPENSE.name(), new BigDecimal("400"), 1L}));
        when(accountRepository.sumBalanceForUser(1L)).thenReturn(BigDecimal.ZERO);

        SummaryResponse r = summaryService.getSummary("a@test.com", "2026-05");

        List<CategoryBreakdownItem> items = r.getCategoryBreakdown();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getPercentage()).isCloseTo(60.0, within(0.01));
        assertThat(items.get(1).getPercentage()).isCloseTo(40.0, within(0.01));

        double sumPct = items.stream().mapToDouble(CategoryBreakdownItem::getPercentage).sum();
        assertThat(sumPct).isCloseTo(100.0, within(0.1));
    }

    @Test
    void getSummary_incomeCategoryHasNullPercentage() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(1L, "2026-05"))
                .thenReturn(Arrays.asList(
                        new Object[] {TransactionType.INCOME, new BigDecimal("5000")},
                        new Object[] {TransactionType.EXPENSE, new BigDecimal("1000")}));
        List<Object[]> incomeRows =
                Collections.singletonList(new Object[] {13L, "Lương", CategoryType.INCOME.name(), new BigDecimal("5000"), 1L});
        when(transactionRepository.categoryBreakdownForUserAndMonth(1L, "2026-05")).thenReturn(incomeRows);
        when(accountRepository.sumBalanceForUser(1L)).thenReturn(BigDecimal.ZERO);

        SummaryResponse r = summaryService.getSummary("a@test.com", "2026-05");

        assertThat(r.getCategoryBreakdown().get(0).getPercentage()).isNull();
    }

    @Test
    void getSummary_isolatedByUser() {
        User user = User.builder().id(99L).email("b@test.com").build();
        when(userRepository.findByEmail("b@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeForUserAndMonth(99L, "2026-05")).thenReturn(Collections.emptyList());
        when(transactionRepository.categoryBreakdownForUserAndMonth(99L, "2026-05")).thenReturn(Collections.emptyList());
        when(accountRepository.sumBalanceForUser(99L)).thenReturn(BigDecimal.ZERO);

        summaryService.getSummary("b@test.com", "2026-05");

        verify(transactionRepository).sumAmountByTypeForUserAndMonth(eq(99L), anyString());
        verify(transactionRepository).categoryBreakdownForUserAndMonth(eq(99L), anyString());
        verify(transactionRepository, never()).sumAmountByTypeForUserAndMonth(eq(1L), anyString());
    }

    @Test
    void getTrend_returnsSixMonthsWithZerosWhenNoTransactions() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeGroupedByMonthRange(
                        eq(1L), eq(LocalDate.of(2025, 12, 1)), eq(LocalDate.of(2026, 5, 31))))
                .thenReturn(Collections.emptyList());

        TrendResponse r = summaryService.getTrend("a@test.com", 6);

        assertThat(r.getTrend()).hasSize(6);
        assertThat(r.getTrend().get(0).getMonth()).isEqualTo("2025-12");
        assertThat(r.getTrend().get(5).getMonth()).isEqualTo("2026-05");
        for (TrendItem t : r.getTrend()) {
            assertThat(t.getIncome()).isEqualByComparingTo("0");
            assertThat(t.getExpense()).isEqualByComparingTo("0");
            assertThat(t.getNet()).isEqualByComparingTo("0");
        }
    }

    @Test
    void getTrend_aggregatesRowsAndKeepsEmptyMonths() {
        User user = User.builder().id(1L).email("a@test.com").build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByTypeGroupedByMonthRange(
                        eq(1L), eq(LocalDate.of(2025, 12, 1)), eq(LocalDate.of(2026, 5, 31))))
                .thenReturn(Arrays.asList(
                        new Object[] {"2026-05", TransactionType.INCOME, new BigDecimal("15000000")},
                        new Object[] {"2026-05", TransactionType.EXPENSE, new BigDecimal("900000")}));

        TrendResponse r = summaryService.getTrend("a@test.com", null);

        assertThat(r.getTrend()).hasSize(6);
        TrendItem may = r.getTrend().stream()
                .filter(t -> "2026-05".equals(t.getMonth()))
                .findFirst()
                .orElseThrow();
        assertThat(may.getIncome()).isEqualByComparingTo("15000000");
        assertThat(may.getExpense()).isEqualByComparingTo("900000");
        assertThat(may.getNet()).isEqualByComparingTo("14100000");

        TrendItem dec = r.getTrend().stream()
                .filter(t -> "2025-12".equals(t.getMonth()))
                .findFirst()
                .orElseThrow();
        assertThat(dec.getIncome()).isEqualByComparingTo("0");
        assertThat(dec.getExpense()).isEqualByComparingTo("0");
    }
}
