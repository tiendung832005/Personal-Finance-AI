package com.data.personalfinanceinsightai.dto.response.transaction;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategorizationResultDTO {
    private Long categoryId;
    private String categoryName;
    private String source;
    private boolean isSuccessful;
    private String message;
}
