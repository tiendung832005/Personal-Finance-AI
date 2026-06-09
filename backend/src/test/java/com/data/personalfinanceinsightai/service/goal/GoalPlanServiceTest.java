package com.data.personalfinanceinsightai.service.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.response.goal.GoalPlanResponse;
import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.repository.FinancialGoalRepository;
import com.data.personalfinanceinsightai.service.GeminiClient;
import com.data.personalfinanceinsightai.service.GoalProgressCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalPlanServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GoalDataCollector dataCollector;

    @Mock
    private GoalProgressCalculator progressCalculator;

    @Mock
    private FinancialGoalRepository goalRepository;

    private GoalPlanService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new GoalPlanService(geminiClient, dataCollector, progressCalculator, goalRepository, clock);
    }

    @Test
    void getPlan_cacheHit_noGeminiCall() {
        FinancialGoal goal = goal();
        goal.setAiPlan("Cached plan");
        goal.setAiPlanGeneratedAt(LocalDateTime.of(2026, 6, 6, 10, 0));
        when(goalRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(goal));

        GoalPlanResponse response = service.getOrGeneratePlan(1L, 10L);

        assertThat(response.getPlan()).isEqualTo("Cached plan");
        assertThat(response.getIsFromCache()).isTrue();
        verifyNoInteractions(geminiClient, dataCollector, progressCalculator);
        verify(goalRepository, never()).save(any());
    }

    @Test
    void getPlan_cacheStale_callsGeminiAndSaves() {
        FinancialGoal goal = goal();
        goal.setAiPlan("Old plan");
        goal.setAiPlanGeneratedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        GoalAnalysisData data = analysisData();
        when(goalRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(goal));
        when(progressCalculator.calculateCurrentAmount(goal)).thenReturn(new BigDecimal("5000000"));
        when(progressCalculator.calculateMonthlyNeeded(eq(goal), eq(new BigDecimal("5000000"))))
                .thenReturn(new BigDecimal("4166667"));
        when(dataCollector.collect(10L)).thenReturn(data);
        when(dataCollector.formatForPrompt(eq(data), eq(goal), eq(new BigDecimal("5000000")), eq(new BigDecimal("4166667"))))
                .thenReturn("prompt data");
        when(geminiClient.chat(any(), eq("prompt data"), eq(500))).thenReturn("AI plan moi");

        GoalPlanResponse response = service.getOrGeneratePlan(1L, 10L);

        assertThat(response.getPlan()).isEqualTo("AI plan moi");
        assertThat(response.getIsFromCache()).isFalse();
        assertThat(goal.getAiPlan()).isEqualTo("AI plan moi");
        verify(goalRepository).save(goal);
    }

    @Test
    void getPlan_geminiFail_usesFallbackAndSaves() {
        FinancialGoal goal = goal();
        GoalAnalysisData data = analysisData();
        when(goalRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(goal));
        when(progressCalculator.calculateCurrentAmount(goal)).thenReturn(new BigDecimal("5000000"));
        when(progressCalculator.calculateMonthlyNeeded(eq(goal), eq(new BigDecimal("5000000"))))
                .thenReturn(new BigDecimal("4166667"));
        when(dataCollector.collect(10L)).thenReturn(data);
        when(dataCollector.formatForPrompt(eq(data), eq(goal), eq(new BigDecimal("5000000")), eq(new BigDecimal("4166667"))))
                .thenReturn("prompt data");
        when(geminiClient.chat(any(), eq("prompt data"), eq(500))).thenReturn(null);

        GoalPlanResponse response = service.getOrGeneratePlan(1L, 10L);

        assertThat(response.getPlan()).contains("Mua xe");
        assertThat(response.getPlan()).contains("4.166.667 VND");
        assertThat(response.getIsFromCache()).isFalse();
        verify(goalRepository).save(goal);
    }

    @Test
    void regeneratePlan_alwaysCallsGemini() {
        FinancialGoal goal = goal();
        goal.setAiPlan("Fresh plan");
        goal.setAiPlanGeneratedAt(LocalDateTime.of(2026, 6, 7, 10, 0));
        GoalAnalysisData data = analysisData();
        when(goalRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(goal));
        when(progressCalculator.calculateCurrentAmount(goal)).thenReturn(new BigDecimal("5000000"));
        when(progressCalculator.calculateMonthlyNeeded(eq(goal), eq(new BigDecimal("5000000"))))
                .thenReturn(new BigDecimal("4166667"));
        when(dataCollector.collect(10L)).thenReturn(data);
        when(dataCollector.formatForPrompt(eq(data), eq(goal), eq(new BigDecimal("5000000")), eq(new BigDecimal("4166667"))))
                .thenReturn("prompt data");
        when(geminiClient.chat(any(), eq("prompt data"), eq(500))).thenReturn("Forced new plan");

        GoalPlanResponse response = service.regeneratePlan(1L, 10L);

        assertThat(response.getPlan()).isEqualTo("Forced new plan");
        assertThat(response.getIsFromCache()).isFalse();
        verify(geminiClient).chat(any(), eq("prompt data"), eq(500));
        verify(goalRepository).save(goal);
    }

    private FinancialGoal goal() {
        return FinancialGoal.builder()
                .id(1L)
                .userId(10L)
                .name("Mua xe")
                .targetAmount(new BigDecimal("30000000"))
                .deadline(LocalDate.of(2026, 12, 31))
                .build();
    }

    private GoalAnalysisData analysisData() {
        return GoalAnalysisData.builder()
                .avgMonthlyIncome(new BigDecimal("8000000"))
                .avgMonthlyExpense(new BigDecimal("7200000"))
                .avgMonthlySavings(new BigDecimal("800000"))
                .topExpenseCategories(List.of(GoalAnalysisData.CategorySpend.builder()
                        .categoryId(1L)
                        .categoryName("Giai tri")
                        .amount(new BigDecimal("3000000"))
                        .build()))
                .build();
    }
}
