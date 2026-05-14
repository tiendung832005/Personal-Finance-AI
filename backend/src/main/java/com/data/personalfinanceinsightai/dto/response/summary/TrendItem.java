package com.data.personalfinanceinsightai.dto.response.summary;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrendItem {

    private final String month;
    private final BigDecimal income;
    private final BigDecimal expense;
    private final BigDecimal net;
}
