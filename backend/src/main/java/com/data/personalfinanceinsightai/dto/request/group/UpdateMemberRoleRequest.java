package com.data.personalfinanceinsightai.dto.request.group;

import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull
    private GroupRole role;
}
