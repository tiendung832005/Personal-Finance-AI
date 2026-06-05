package com.data.personalfinanceinsightai.dto.response.transaction.imports;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportPreviewResponse {
    private int validCount;
    private int errorCount;
    private List<ImportPreviewRow> validRows;
    private List<ImportErrorRow> errorRows;
}

