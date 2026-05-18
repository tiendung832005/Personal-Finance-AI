package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.group.InviteMemberRequest;
import com.data.personalfinanceinsightai.dto.response.group.GroupResponse;
import com.data.personalfinanceinsightai.dto.response.group.InvitationResponse;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.GroupInvitation;
import com.data.personalfinanceinsightai.entity.GroupMember;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import com.data.personalfinanceinsightai.entity.enums.InvitationStatus;
import com.data.personalfinanceinsightai.exception.BusinessException;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.GroupInvitationRepository;
import com.data.personalfinanceinsightai.repository.GroupMemberRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.NotificationService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupInvitationServiceTest {

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
    void inviteMember_success() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        FamilyGroup group = FamilyGroup.builder().id(10L).name("Family").createdBy(1L).build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupInvitationRepository.findByGroupIdAndEmailAndStatus(10L, "member@test.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(groupInvitationRepository.save(any(GroupInvitation.class)))
                .thenAnswer(inv -> {
                    GroupInvitation i = inv.getArgument(0);
                    i.setId(100L);
                    return i;
                });
        when(notificationService.buildInvitationAcceptLink(any())).thenReturn("http://localhost/accept?token=abc");

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("member@test.com");

        InvitationResponse r = groupService.inviteMember("admin@test.com", 10L, req);

        assertThat(r.getId()).isEqualTo(100L);
        assertThat(r.getEmail()).isEqualTo("member@test.com");
        assertThat(r.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(groupAuthorizationService).requireAdmin(10L, 1L);
        verify(notificationService).sendGroupInvitation(eq("member@test.com"), any(), eq("Family"));
    }

    @Test
    void inviteMember_notAdmin() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(groupAuthorizationService.requireAdmin(10L, 1L))
                .thenThrow(new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này"));

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("other@test.com");

        assertThatThrownBy(() -> groupService.inviteMember("admin@test.com", 10L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void inviteMember_alreadyMember() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        User existing = User.builder().id(2L).email("member@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(10L))
                .thenReturn(Optional.of(FamilyGroup.builder().id(10L).name("G").build()));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(existing));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true);

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("member@test.com");

        assertThatThrownBy(() -> groupService.inviteMember("admin@test.com", 10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void inviteMember_duplicatePending() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        GroupInvitation old = GroupInvitation.builder()
                .id(50L)
                .groupId(10L)
                .email("member@test.com")
                .token("oldtoken")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdBy(1L)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(10L))
                .thenReturn(Optional.of(FamilyGroup.builder().id(10L).name("G").build()));
        when(groupInvitationRepository.findByGroupIdAndEmailAndStatus(10L, "member@test.com", InvitationStatus.PENDING))
                .thenReturn(Optional.of(old));
        when(groupInvitationRepository.save(any(GroupInvitation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.buildInvitationAcceptLink(any())).thenReturn("http://link");

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("member@test.com");

        groupService.inviteMember("admin@test.com", 10L, req);

        assertThat(old.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
        verify(groupInvitationRepository).save(old);

        ArgumentCaptor<GroupInvitation> cap = ArgumentCaptor.forClass(GroupInvitation.class);
        verify(groupInvitationRepository, org.mockito.Mockito.times(2)).save(cap.capture());
        GroupInvitation created = cap.getAllValues().get(1);
        assertThat(created.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(created.getEmail()).isEqualTo("member@test.com");
    }

    @Test
    void acceptInvitation_success() {
        User user = User.builder().id(2L).email("member@test.com").build();
        GroupInvitation inv = GroupInvitation.builder()
                .id(1L)
                .groupId(10L)
                .email("member@test.com")
                .token("tok")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(1L)
                .build();
        FamilyGroup group = FamilyGroup.builder().id(10L).name("G").createdBy(1L).build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(user));
        when(groupInvitationRepository.findByToken("tok")).thenReturn(Optional.of(inv));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(i -> i.getArgument(0));
        when(groupInvitationRepository.save(any(GroupInvitation.class))).thenAnswer(i -> i.getArgument(0));
        when(familyGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(10L)).thenReturn(2L);

        GroupResponse r = groupService.acceptInvitation("member@test.com", "tok");

        assertThat(r.getId()).isEqualTo(10L);
        assertThat(r.getMyRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(inv.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    void acceptInvitation_tokenNotFound() {
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(groupInvitationRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.acceptInvitation("u@test.com", "bad"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invitation not found");
    }

    @Test
    void acceptInvitation_expired() {
        User user = User.builder().id(2L).email("member@test.com").build();
        GroupInvitation inv = GroupInvitation.builder()
                .groupId(10L)
                .email("member@test.com")
                .token("tok")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .createdBy(1L)
                .build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(user));
        when(groupInvitationRepository.findByToken("tok")).thenReturn(Optional.of(inv));
        when(groupInvitationRepository.save(inv)).thenReturn(inv);

        assertThatThrownBy(() -> groupService.acceptInvitation("member@test.com", "tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
        assertThat(inv.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void acceptInvitation_emailMismatch() {
        User user = User.builder().id(2L).email("other@test.com").build();
        GroupInvitation inv = GroupInvitation.builder()
                .groupId(10L)
                .email("member@test.com")
                .token("tok")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(1L)
                .build();
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(user));
        when(groupInvitationRepository.findByToken("tok")).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> groupService.acceptInvitation("other@test.com", "tok"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("member@test.com");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_alreadyMember() {
        User user = User.builder().id(2L).email("member@test.com").build();
        GroupInvitation inv = GroupInvitation.builder()
                .groupId(10L)
                .email("member@test.com")
                .token("tok")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(1L)
                .build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(user));
        when(groupInvitationRepository.findByToken("tok")).thenReturn(Optional.of(inv));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.acceptInvitation("member@test.com", "tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }
}
