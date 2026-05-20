package com.data.personalfinanceinsightai.dto.response.summary;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberSummaryItem {

    private final Long userId;
    private final String fullName;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netContribution;
    private final long transactionCount;
}
