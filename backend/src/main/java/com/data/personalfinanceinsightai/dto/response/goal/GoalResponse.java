package com.data.personalfinanceinsightai.dto.response.goal;

import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalResponse {

    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate deadline;
    private GoalStatus status;
    private Long linkedAccountId;
    private String linkedAccountName;
    private BigDecimal currentAmount;
    private Double progressPercentage;
    private BigDecimal remainingAmount;
    private Long monthsRemaining;
    private BigDecimal monthlyNeeded;
    private Boolean onTrack;
    private LocalDateTime createdAt;
}
