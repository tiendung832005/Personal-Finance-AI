package com.data.personalfinanceinsightai.dto.response.transaction.imports;

import com.data.personalfinanceinsightai.dto.response.transaction.ResultSource;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportPreviewRow {
    private Integer rowNumber;
    private LocalDate date;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private String note;
    private Long categoryId;
    private String categoryName;
    private ResultSource aiSource;
    private boolean autoCategorized;
}

