package com.data.personalfinanceinsightai.dto.response.group;

import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private long memberCount;
    private GroupRole myRole;
    private LocalDateTime createdAt;
}
