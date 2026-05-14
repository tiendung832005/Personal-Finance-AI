package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.response.summary.SummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.TrendResponse;

public interface SummaryService {

    SummaryResponse getSummary(String email, String month);

    TrendResponse getTrend(String email, Integer months);
}
