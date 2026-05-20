package com.data.personalfinanceinsightai.dto.response.summary;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyTrendResponse {

    private final Long groupId;
    private final List<TrendItem> trend;
}
