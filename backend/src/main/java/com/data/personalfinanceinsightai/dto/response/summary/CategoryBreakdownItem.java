package com.data.personalfinanceinsightai.dto.response.summary;

import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryBreakdownItem {

    private final Long categoryId;
    private final String categoryName;
    private final CategoryType type;
    private final BigDecimal total;
    private final Double percentage;
    private final long transactionCount;
}
