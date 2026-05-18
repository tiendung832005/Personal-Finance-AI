package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.group.CreateGroupRequest;
import com.data.personalfinanceinsightai.dto.request.group.InviteMemberRequest;
import com.data.personalfinanceinsightai.dto.request.group.UpdateMemberRoleRequest;
import com.data.personalfinanceinsightai.dto.response.group.GroupMemberResponse;
import com.data.personalfinanceinsightai.dto.response.group.GroupResponse;
import com.data.personalfinanceinsightai.dto.response.group.InvitationResponse;
import java.util.List;

public interface GroupService {

    GroupResponse createGroup(String email, CreateGroupRequest request);

    List<GroupResponse> listGroups(String email);

    GroupResponse getGroupDetail(String email, Long groupId);

    void deleteGroup(String email, Long groupId);

    InvitationResponse inviteMember(String email, Long groupId, InviteMemberRequest request);

    List<InvitationResponse> listPendingInvitations(String email, Long groupId);

    List<InvitationResponse> listMyPendingInvitations(String email);

    GroupResponse acceptInvitation(String email, String token);

    List<GroupMemberResponse> getMembers(String email, Long groupId);

    void kickMember(String email, Long groupId, Long targetUserId);

    GroupMemberResponse updateMemberRole(
            String email, Long groupId, Long targetUserId, UpdateMemberRoleRequest request);

    void leaveGroup(String email, Long groupId);
}
