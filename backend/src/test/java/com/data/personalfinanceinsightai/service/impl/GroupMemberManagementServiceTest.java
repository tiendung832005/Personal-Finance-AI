package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.group.UpdateMemberRoleRequest;
import com.data.personalfinanceinsightai.dto.response.group.GroupMemberResponse;
import com.data.personalfinanceinsightai.entity.GroupMember;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import com.data.personalfinanceinsightai.exception.BusinessException;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.GroupInvitationRepository;
import com.data.personalfinanceinsightai.repository.GroupMemberRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupMemberManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupInvitationRepository groupInvitationRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private NotificationService notificationService;

    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupServiceImpl(
                userRepository,
                familyGroupRepository,
                groupMemberRepository,
                groupInvitationRepository,
                groupAuthorizationService,
                notificationService);
    }

    @Test
    void kickMember_success() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        GroupMember target = GroupMember.builder()
                .id(5L)
                .groupId(10L)
                .userId(2L)
                .role(GroupRole.MEMBER)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).thenReturn(Optional.of(target));

        groupService.kickMember("admin@test.com", 10L, 2L);

        verify(groupAuthorizationService).requireAdmin(10L, 1L);
        verify(groupMemberRepository).delete(target);
    }

    @Test
    void kickMember_notAdmin() {
        User user = User.builder().id(1L).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(user));
        when(groupAuthorizationService.requireAdmin(10L, 1L))
                .thenThrow(new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này"));

        assertThatThrownBy(() -> groupService.kickMember("member@test.com", 10L, 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void kickMember_selfKick() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        GroupMember self = GroupMember.builder()
                .id(5L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.ADMIN)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> groupService.kickMember("admin@test.com", 10L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("remove yourself");
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void kickMember_kickAdmin() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        GroupMember otherAdmin = GroupMember.builder()
                .id(6L)
                .groupId(10L)
                .userId(2L)
                .role(GroupRole.ADMIN)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).thenReturn(Optional.of(otherAdmin));

        assertThatThrownBy(() -> groupService.kickMember("admin@test.com", 10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("another ADMIN");
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void updateRole_success() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        User targetUser = User.builder().id(2L).email("m@test.com").fullName("Member").build();
        GroupMember target = GroupMember.builder()
                .id(5L)
                .groupId(10L)
                .userId(2L)
                .role(GroupRole.MEMBER)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).thenReturn(Optional.of(target));
        when(groupMemberRepository.save(target)).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(GroupRole.ADMIN);

        GroupMemberResponse r = groupService.updateMemberRole("admin@test.com", 10L, 2L, req);

        assertThat(r.getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(target.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void updateRole_selfChange() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(GroupRole.MEMBER);

        assertThatThrownBy(() -> groupService.updateMemberRole("admin@test.com", 10L, 1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("your own role");
    }

    @Test
    void leaveGroup_lastAdmin() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        GroupMember member = GroupMember.builder()
                .id(5L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.ADMIN)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(groupMemberRepository.countByGroupIdAndRole(10L, GroupRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> groupService.leaveGroup("admin@test.com", 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only ADMIN");
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void leaveGroup_success() {
        User member = User.builder().id(2L).email("member@test.com").build();
        GroupMember row = GroupMember.builder()
                .id(5L)
                .groupId(10L)
                .userId(2L)
                .role(GroupRole.MEMBER)
                .build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).thenReturn(Optional.of(row));

        groupService.leaveGroup("member@test.com", 10L);

        verify(groupMemberRepository).delete(row);
    }
}
