package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.group.CreateGroupRequest;
import com.data.personalfinanceinsightai.dto.request.group.InviteMemberRequest;
import com.data.personalfinanceinsightai.dto.request.group.UpdateMemberRoleRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.group.GroupMemberResponse;
import com.data.personalfinanceinsightai.dto.response.group.GroupResponse;
import com.data.personalfinanceinsightai.dto.response.group.InvitationResponse;
import com.data.personalfinanceinsightai.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateGroupRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(groupService.createGroup(principal.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(groupService.listGroups(principal.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getById(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(groupService.getGroupDetail(principal.getUsername(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupService.deleteGroup(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<InvitationResponse>> invite(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(groupService.inviteMember(principal.getUsername(), id, request)));
    }

    @GetMapping("/{id}/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> listInvitations(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(groupService.listPendingInvitations(principal.getUsername(), id)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> members(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(groupService.getMembers(principal.getUsername(), id)));
    }

    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupService.leaveGroup(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> kick(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long userId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupService.kickMember(principal.getUsername(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> updateRole(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                groupService.updateMemberRole(principal.getUsername(), id, userId, request)));
    }
}
