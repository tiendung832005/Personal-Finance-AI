package com.data.personalfinanceinsightai.dto.response.goal;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalDetailResponse {

    private GoalResponse goal;
    private String aiPlan;
    private LocalDateTime aiPlanGeneratedAt;
    private List<GoalSnapshotResponse> monthlySnapshots;
}
