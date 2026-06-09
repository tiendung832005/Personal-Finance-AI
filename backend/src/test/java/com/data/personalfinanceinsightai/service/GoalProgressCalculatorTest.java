package com.data.personalfinanceinsightai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.service.goal.GoalAlertService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalProgressCalculatorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GoalAlertService goalAlertService;

    private GoalProgressCalculator calculator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        calculator = new GoalProgressCalculator(accountRepository, goalAlertService, clock);
    }

    @Test
    void calculateProgress_zero() {
        assertThat(calculator.calculateProgress(BigDecimal.ZERO, new BigDecimal("10000000"))).isEqualTo(0.0);
    }

    @Test
    void calculateProgress_half() {
        assertThat(calculator.calculateProgress(new BigDecimal("5000000"), new BigDecimal("10000000")))
                .isCloseTo(50.0, within(0.01));
    }

    @Test
    void calculateProgress_full() {
        assertThat(calculator.calculateProgress(new BigDecimal("10000000"), new BigDecimal("10000000")))
                .isEqualTo(100.0);
    }

    @Test
    void calculateProgress_over_isCappedAt100() {
        assertThat(calculator.calculateProgress(new BigDecimal("12000000"), new BigDecimal("10000000")))
                .isEqualTo(100.0);
    }

    @Test
    void calculateMonthlyNeeded_6months() {
        FinancialGoal goal = goal("30000000", LocalDate.of(2026, 12, 31));

        BigDecimal result = calculator.calculateMonthlyNeeded(goal, new BigDecimal("6000000"));

        assertThat(result).isEqualByComparingTo("4000000");
    }

    @Test
    void calculateMonthlyNeeded_deadlineThisMonth_returnsRemaining() {
        FinancialGoal goal = goal("30000000", LocalDate.of(2026, 6, 30));

        BigDecimal result = calculator.calculateMonthlyNeeded(goal, new BigDecimal("10000000"));

        assertThat(result).isEqualByComparingTo("20000000");
    }

    @Test
    void calculateMonthlyNeeded_alreadyAchieved_returnsZero() {
        FinancialGoal goal = goal("30000000", LocalDate.of(2026, 12, 31));

        BigDecimal result = calculator.calculateMonthlyNeeded(goal, new BigDecimal("30000000"));

        assertThat(result).isEqualByComparingTo("0");
    }

    private FinancialGoal goal(String targetAmount, LocalDate deadline) {
        return FinancialGoal.builder()
                .id(1L)
                .userId(10L)
                .name("Mua xe")
                .targetAmount(new BigDecimal(targetAmount))
                .deadline(deadline)
                .build();
    }
}
