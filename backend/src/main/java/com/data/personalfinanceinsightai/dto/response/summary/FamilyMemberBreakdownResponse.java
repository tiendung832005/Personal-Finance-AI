package com.data.personalfinanceinsightai.dto.response.summary;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyMemberBreakdownResponse {

    private final String month;
    private final Long groupId;
    private final List<MemberSummaryItem> members;
}
