package com.data.personalfinanceinsightai.dto.response.transaction;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategorizationResult {
    private Long categoryId;
    private String suggestedCategoryName;
    private ResultSource source;
    private String message;

    public boolean isSuccessful() {
        return categoryId != null;
    }

    public static CategorizationResult fromCache(Long id, String name) {
        return builder().categoryId(id).suggestedCategoryName(name).source(ResultSource.CACHE).build();
    }

    public static CategorizationResult fromAI(Long id, String name) {
        return builder().categoryId(id).suggestedCategoryName(name)
                .source(ResultSource.AI).build();
    }

    public static CategorizationResult failed(String msg) {
        return builder().source(ResultSource.FAILED).message(msg).build();
    }

    public static CategorizationResult skipped(String msg) {
        return builder().source(ResultSource.SKIPPED).message(msg).build();
    }
}
