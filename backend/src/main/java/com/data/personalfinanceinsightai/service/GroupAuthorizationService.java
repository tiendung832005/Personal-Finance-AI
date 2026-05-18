package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.entity.GroupMember;
import com.data.personalfinanceinsightai.entity.enums.GroupRole;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupAuthorizationService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupMember requireMember(Long groupId, Long userId) {
        return groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không phải thành viên của nhóm này"));
    }

    public GroupMember requireAdmin(Long groupId, Long userId) {
        GroupMember member = requireMember(groupId, userId);
        if (member.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này");
        }
        return member;
    }

    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    public boolean isAdmin(Long groupId, Long userId) {
        return groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }
}
