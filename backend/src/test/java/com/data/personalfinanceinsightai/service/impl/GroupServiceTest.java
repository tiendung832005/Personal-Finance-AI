package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.group.CreateGroupRequest;
import com.data.personalfinanceinsightai.dto.response.group.GroupResponse;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.GroupMember;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.GroupInvitationRepository;
import com.data.personalfinanceinsightai.repository.GroupMemberRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.NotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupInvitationRepository groupInvitationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

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
    void createGroup_success() {
        User user = User.builder().id(1L).email("owner@test.com").build();
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        when(familyGroupRepository.save(any(FamilyGroup.class)))
                .thenAnswer(invocation -> {
                    FamilyGroup g = invocation.getArgument(0);
                    g.setId(10L);
                    return g;
                });
        when(groupMemberRepository.save(any(GroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("  Gia đình A  ");
        req.setDescription("  mô tả  ");

        GroupResponse r = groupService.createGroup("owner@test.com", req);

        assertThat(r.getId()).isEqualTo(10L);
        assertThat(r.getName()).isEqualTo("Gia đình A");
        assertThat(r.getDescription()).isEqualTo("mô tả");
        assertThat(r.getCreatedBy()).isEqualTo(1L);
        assertThat(r.getMemberCount()).isEqualTo(1L);
        assertThat(r.getMyRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void createGroup_creatorAutoAddedAsAdmin() {
        User user = User.builder().id(1L).email("owner@test.com").build();
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        when(familyGroupRepository.save(any(FamilyGroup.class)))
                .thenAnswer(invocation -> {
                    FamilyGroup g = invocation.getArgument(0);
                    g.setId(10L);
                    return g;
                });
        when(groupMemberRepository.save(any(GroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("G1");

        groupService.createGroup("owner@test.com", req);

        ArgumentCaptor<GroupMember> cap = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(cap.capture());
        assertThat(cap.getValue().getGroupId()).isEqualTo(10L);
        assertThat(cap.getValue().getUserId()).isEqualTo(1L);
        assertThat(cap.getValue().getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void getGroups_onlyReturnUserGroups() {
        User user = User.builder().id(1L).email("u@test.com").build();
        FamilyGroup g1 = FamilyGroup.builder().id(10L).name("A").createdBy(2L).build();
        FamilyGroup g2 = FamilyGroup.builder().id(11L).name("B").createdBy(3L).build();
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(familyGroupRepository.findGroupsByUserId(1L)).thenReturn(List.of(g1, g2));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(GroupMember.builder()
                        .id(1L)
                        .groupId(10L)
                        .userId(1L)
                        .role(GroupRole.MEMBER)
                        .build()));
        when(groupMemberRepository.findByGroupIdAndUserId(11L, 1L))
                .thenReturn(Optional.of(GroupMember.builder()
                        .id(2L)
                        .groupId(11L)
                        .userId(1L)
                        .role(GroupRole.ADMIN)
                        .build()));
        when(groupMemberRepository.countByGroupId(10L)).thenReturn(5L);
        when(groupMemberRepository.countByGroupId(11L)).thenReturn(2L);

        List<GroupResponse> list = groupService.listGroups("u@test.com");

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(10L);
        assertThat(list.get(0).getMyRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(list.get(0).getMemberCount()).isEqualTo(5L);
        assertThat(list.get(1).getMyRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(list.get(1).getMemberCount()).isEqualTo(2L);
    }

    @Test
    void getGroupDetail_notMember() {
        User user = User.builder().id(1L).email("u@test.com").build();
        FamilyGroup group = FamilyGroup.builder().id(99L).name("X").createdBy(2L).build();
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(familyGroupRepository.findById(99L)).thenReturn(Optional.of(group));
        when(groupAuthorizationService.requireMember(eq(99L), eq(1L)))
                .thenThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"));

        assertThatThrownBy(() -> groupService.getGroupDetail("u@test.com", 99L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("thành viên");
    }
}
