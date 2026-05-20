package com.data.personalfinanceinsightai.dto.response;

import com.data.personalfinanceinsightai.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
