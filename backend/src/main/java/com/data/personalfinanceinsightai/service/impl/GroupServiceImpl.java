package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.group.CreateGroupRequest;
import com.data.personalfinanceinsightai.dto.request.group.InviteMemberRequest;
import com.data.personalfinanceinsightai.dto.request.group.UpdateMemberRoleRequest;
import com.data.personalfinanceinsightai.dto.response.group.GroupMemberResponse;
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
import com.data.personalfinanceinsightai.service.GroupService;
import com.data.personalfinanceinsightai.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final UserRepository userRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public GroupResponse createGroup(String email, CreateGroupRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyGroup group = FamilyGroup.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .createdBy(user.getId())
                .build();
        group = familyGroupRepository.save(group);

        GroupMember adminMember = GroupMember.builder()
                .groupId(group.getId())
                .userId(user.getId())
                .role(GroupRole.ADMIN)
                .build();
        groupMemberRepository.save(adminMember);

        return mapToResponse(group, GroupRole.ADMIN, 1L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> listGroups(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return familyGroupRepository.findGroupsByUserId(user.getId()).stream()
                .map(g -> {
                    GroupMember self = groupMemberRepository
                            .findByGroupIdAndUserId(g.getId(), user.getId())
                            .orElseThrow(() -> new IllegalStateException("Member row missing for listed group"));
                    long memberCount = groupMemberRepository.countByGroupId(g.getId());
                    return mapToResponse(g, self.getRole(), memberCount);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetail(String email, Long groupId) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyGroup group = familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        groupAuthorizationService.requireMember(group.getId(), user.getId());

        GroupMember self = groupMemberRepository
                .findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow();
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        return mapToResponse(group, self.getRole(), memberCount);
    }

    @Override
    @Transactional
    public void deleteGroup(String email, Long groupId) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, currentUser.getId());

        FamilyGroup group = familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!group.getCreatedBy().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the group creator can delete this group");
        }

        familyGroupRepository.delete(group);
    }

    @Override
    @Transactional
    public InvitationResponse inviteMember(String email, Long groupId, InviteMemberRequest request) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, currentUser.getId());

        FamilyGroup group = familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        String inviteEmail = normalizeEmail(request.getEmail());
        if (inviteEmail.equals(normalizeEmail(currentUser.getEmail()))) {
            throw new BusinessException("You cannot invite yourself");
        }

        userRepository
                .findByEmail(inviteEmail)
                .ifPresent(invited -> {
                    if (groupMemberRepository.existsByGroupIdAndUserId(groupId, invited.getId())) {
                        throw new BusinessException(inviteEmail + " is already a member of this group");
                    }
                });

        groupInvitationRepository
                .findByGroupIdAndEmailAndStatus(groupId, inviteEmail, InvitationStatus.PENDING)
                .ifPresent(old -> {
                    old.setStatus(InvitationStatus.CANCELLED);
                    groupInvitationRepository.save(old);
                });

        String token = UUID.randomUUID().toString().replace("-", "");
        GroupInvitation invitation = GroupInvitation.builder()
                .groupId(groupId)
                .email(inviteEmail)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdBy(currentUser.getId())
                .build();
        invitation = groupInvitationRepository.save(invitation);

        notificationService.sendGroupInvitation(inviteEmail, token, group.getName());

        return mapToInvitationResponse(invitation, group.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingInvitations(String email, Long groupId) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, currentUser.getId());

        FamilyGroup group = familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        return groupInvitationRepository.findByGroupIdAndStatus(groupId, InvitationStatus.PENDING).stream()
                .map(inv -> mapToInvitationResponse(inv, group.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listMyPendingInvitations(String email) {
        userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        return groupInvitationRepository
                .findByEmailAndStatus(normalizedEmail, InvitationStatus.PENDING)
                .stream()
                .filter(inv -> inv.getExpiresAt().isAfter(now))
                .map(inv -> {
                    String groupName = familyGroupRepository
                            .findById(inv.getGroupId())
                            .map(FamilyGroup::getName)
                            .orElse("Group");
                    return mapToInvitationResponse(inv, groupName);
                })
                .toList();
    }

    @Override
    @Transactional
    public GroupResponse acceptInvitation(String email, String token) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GroupInvitation invitation = groupInvitationRepository
                .findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("This invitation link has already been used or was cancelled");
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            groupInvitationRepository.save(invitation);
            throw new BusinessException("This invitation link has expired (7 days)");
        }

        if (!currentUser.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenException(
                    "This invitation is for " + invitation.getEmail() + ", not your account");
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(invitation.getGroupId(), currentUser.getId())) {
            throw new BusinessException("You are already a member of this group");
        }

        GroupMember newMember = GroupMember.builder()
                .groupId(invitation.getGroupId())
                .userId(currentUser.getId())
                .role(GroupRole.MEMBER)
                .build();
        groupMemberRepository.save(newMember);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        groupInvitationRepository.save(invitation);

        FamilyGroup group = familyGroupRepository
                .findById(invitation.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        return mapToResponse(group, GroupRole.MEMBER, memberCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(String email, Long groupId) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<Long> userIds = members.stream().map(GroupMember::getUserId).distinct().toList();
        Map<Long, User> userById =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(m -> {
                    User u = userById.get(m.getUserId());
                    if (u == null) {
                        throw new ResourceNotFoundException("User not found for member " + m.getUserId());
                    }
                    return mapToMemberResponse(m, u);
                })
                .toList();
    }

    @Override
    @Transactional
    public void kickMember(String email, Long groupId, Long targetUserId) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, currentUser.getId());

        GroupMember target = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));

        if (targetUserId.equals(currentUser.getId())) {
            throw new BusinessException("You cannot remove yourself. Use leave group instead.");
        }

        if (target.getRole() == GroupRole.ADMIN) {
            throw new BusinessException(
                    "You cannot remove another ADMIN. Downgrade their role to MEMBER first.");
        }

        groupMemberRepository.delete(target);
    }

    @Override
    @Transactional
    public GroupMemberResponse updateMemberRole(
            String email, Long groupId, Long targetUserId, UpdateMemberRoleRequest request) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, currentUser.getId());

        if (targetUserId.equals(currentUser.getId())) {
            throw new BusinessException("You cannot change your own role");
        }

        GroupMember target = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this group"));

        target.setRole(request.getRole());
        target = groupMemberRepository.save(target);

        User user = userRepository
                .findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToMemberResponse(target, user);
    }

    @Override
    @Transactional
    public void leaveGroup(String email, Long groupId) {
        User currentUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));

        if (member.getRole() == GroupRole.ADMIN
                && groupMemberRepository.countByGroupIdAndRole(groupId, GroupRole.ADMIN) <= 1) {
            throw new BusinessException(
                    "You are the only ADMIN. Assign another ADMIN before leaving the group.");
        }

        groupMemberRepository.delete(member);
    }

    private static GroupMemberResponse mapToMemberResponse(GroupMember member, User user) {
        return GroupMemberResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private InvitationResponse mapToInvitationResponse(GroupInvitation invitation, String groupName) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .groupId(invitation.getGroupId())
                .groupName(groupName)
                .email(invitation.getEmail())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .inviteLink(notificationService.buildInvitationAcceptLink(invitation.getToken()))
                .build();
    }

    private static String normalizeEmail(String raw) {
        Objects.requireNonNull(raw, "email");
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static GroupResponse mapToResponse(FamilyGroup group, GroupRole myRole, long memberCount) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .memberCount(memberCount)
                .myRole(myRole)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
