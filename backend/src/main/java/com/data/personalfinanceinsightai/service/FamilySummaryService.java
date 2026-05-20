package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.response.summary.FamilyMemberBreakdownResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilySummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyTrendResponse;

public interface FamilySummaryService {

    FamilySummaryResponse getGroupSummary(String email, Long groupId, String month);

    FamilyMemberBreakdownResponse getByMemberBreakdown(String email, Long groupId, String month);

    FamilyTrendResponse getGroupTrend(String email, Long groupId, Integer months);
}
