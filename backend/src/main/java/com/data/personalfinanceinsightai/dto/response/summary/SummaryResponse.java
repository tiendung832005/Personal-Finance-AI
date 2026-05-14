package com.data.personalfinanceinsightai.dto.response.summary;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryResponse {

    private final String month;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netBalance;
    private final BigDecimal currentBalance;
    private final List<CategoryBreakdownItem> categoryBreakdown;
}
