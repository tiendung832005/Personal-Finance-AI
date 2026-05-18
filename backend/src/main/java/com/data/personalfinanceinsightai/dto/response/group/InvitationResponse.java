package com.data.personalfinanceinsightai.dto.response.group;

import com.data.personalfinanceinsightai.entity.enums.InvitationStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private String email;
    private InvitationStatus status;
    private LocalDateTime expiresAt;
    private String inviteLink;
}
