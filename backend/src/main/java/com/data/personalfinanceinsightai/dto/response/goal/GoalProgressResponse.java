package com.data.personalfinanceinsightai.dto.response.goal;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalProgressResponse {

    private GoalResponse goal;
    private List<GoalSnapshotResponse> monthlyHistory;
    private ProgressSummary summary;

    @Getter
    @Builder
    public static class ProgressSummary {
        private Long totalMonths;
        private Long onTrackMonths;
        private Long behindMonths;
        private Long aheadMonths;
    }
}
