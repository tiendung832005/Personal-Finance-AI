package com.data.personalfinanceinsightai.dto.response.goal;

import com.data.personalfinanceinsightai.entity.GoalMonthlySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalSnapshotResponse {

    private LocalDate month;
    private BigDecimal savedAmount;
    private BigDecimal plannedAmount;
    private Boolean onTrack;
    private BigDecimal difference;

    public static GoalSnapshotResponse fromEntity(GoalMonthlySnapshot snapshot) {
        BigDecimal planned = snapshot.getPlannedAmount() == null ? BigDecimal.ZERO : snapshot.getPlannedAmount();
        BigDecimal saved = snapshot.getSavedAmount() == null ? BigDecimal.ZERO : snapshot.getSavedAmount();
        return GoalSnapshotResponse.builder()
                .month(snapshot.getMonth())
                .savedAmount(saved)
                .plannedAmount(snapshot.getPlannedAmount())
                .onTrack(snapshot.getOnTrack())
                .difference(saved.subtract(planned))
                .build();
    }
}
