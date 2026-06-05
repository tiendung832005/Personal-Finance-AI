package com.data.personalfinanceinsightai.dto.response.transaction.imports;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportErrorRow {
    private Integer rowNumber;
    private String rawData;
    private String error;
}

