package com.data.personalfinanceinsightai.dto.response.goal;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalPlanResponse {

    private String plan;
    private LocalDateTime generatedAt;
    private Boolean isFromCache;
}
