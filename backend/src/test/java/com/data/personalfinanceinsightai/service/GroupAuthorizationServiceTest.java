package com.data.personalfinanceinsightai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.entity.GroupMember;
import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.GroupMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupAuthorizationServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    private GroupAuthorizationService groupAuthorizationService;

    @BeforeEach
    void setUp() {
        groupAuthorizationService = new GroupAuthorizationService(groupMemberRepository);
    }

    @Test
    void requireAdmin_success() {
        GroupMember admin = GroupMember.builder()
                .id(1L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.ADMIN)
                .build();
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(admin));

        GroupMember m = groupAuthorizationService.requireAdmin(10L, 1L);

        assertThat(m.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void requireAdmin_notAdmin() {
        GroupMember member = GroupMember.builder()
                .id(1L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.MEMBER)
                .build();
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> groupAuthorizationService.requireAdmin(10L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void requireMember_notMember() {
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupAuthorizationService.requireMember(10L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("thành viên");
    }

    @Test
    void isMember_and_isAdmin() {
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(GroupMember.builder()
                        .groupId(10L)
                        .userId(1L)
                        .role(GroupRole.ADMIN)
                        .build()));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);

        assertThat(groupAuthorizationService.isMember(10L, 1L)).isTrue();
        assertThat(groupAuthorizationService.isAdmin(10L, 1L)).isTrue();
    }
}
