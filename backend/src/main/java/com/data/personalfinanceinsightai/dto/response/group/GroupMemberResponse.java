package com.data.personalfinanceinsightai.dto.response.group;

import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupMemberResponse {

    private Long userId;
    private String fullName;
    private String email;
    private GroupRole role;
    private LocalDateTime joinedAt;
}
