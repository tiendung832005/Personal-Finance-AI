package com.data.personalfinanceinsightai.controller;

import com.data.personalfinanceinsightai.dto.request.account.CreateSharedAccountRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.request.group.CreateGroupRequest;
import com.data.personalfinanceinsightai.dto.response.PagedResponse;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyMemberBreakdownResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilySummaryResponse;
import com.data.personalfinanceinsightai.dto.response.summary.FamilyTrendResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.SharedTransactionResponse;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.dto.request.group.InviteMemberRequest;
import com.data.personalfinanceinsightai.dto.request.group.UpdateMemberRoleRequest;
import com.data.personalfinanceinsightai.dto.response.ApiResponse;
import com.data.personalfinanceinsightai.dto.response.group.GroupMemberResponse;
import com.data.personalfinanceinsightai.dto.response.group.GroupResponse;
import com.data.personalfinanceinsightai.dto.response.group.InvitationResponse;
import com.data.personalfinanceinsightai.service.FamilySummaryService;
import com.data.personalfinanceinsightai.service.GroupService;
import com.data.personalfinanceinsightai.service.SharedAccountService;
import com.data.personalfinanceinsightai.service.SharedBudgetService;
import com.data.personalfinanceinsightai.service.SharedTransactionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final SharedAccountService sharedAccountService;
    private final SharedTransactionService sharedTransactionService;
    private final SharedBudgetService sharedBudgetService;
    private final FamilySummaryService familySummaryService;

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

    @PostMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> createSharedAccount(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateSharedAccountRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        sharedAccountService.createSharedAccount(principal.getUsername(), id, request)));
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> listSharedAccounts(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(sharedAccountService.getSharedAccounts(principal.getUsername(), id)));
    }

    @PostMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<SharedTransactionResponse>> createSharedTransaction(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionCreateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        sharedTransactionService.createSharedTransaction(principal.getUsername(), id, request)));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<PagedResponse<SharedTransactionResponse>>> listSharedTransactions(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(sharedTransactionService.getSharedTransactions(
                principal.getUsername(), id, month, categoryId, type, page, size)));
    }

    @PostMapping("/{id}/budgets")
    public ResponseEntity<ApiResponse<BudgetResponse>> createGroupBudget(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetCreateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sharedBudgetService.createGroupBudget(principal.getUsername(), id, request)));
    }

    @GetMapping("/{id}/budgets")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> listGroupBudgets(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(sharedBudgetService.listGroupBudgets(principal.getUsername(), id, month)));
    }

    @GetMapping("/{id}/budgets/status")
    public ResponseEntity<ApiResponse<BudgetStatusResponse>> getGroupBudgetStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                sharedBudgetService.getGroupBudgetStatus(principal.getUsername(), id, month)));
    }

    @PutMapping("/{id}/budgets/{budgetId}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateGroupBudget(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                sharedBudgetService.updateGroupBudget(principal.getUsername(), id, budgetId, request)));
    }

    @DeleteMapping("/{id}/budgets/{budgetId}")
    public ResponseEntity<Void> deleteGroupBudget(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long budgetId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        sharedBudgetService.deleteGroupBudget(principal.getUsername(), id, budgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<FamilySummaryResponse>> getFamilySummary(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(required = false) String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(familySummaryService.getGroupSummary(principal.getUsername(), id, month)));
    }

    @GetMapping("/{id}/summary/by-member")
    public ResponseEntity<ApiResponse<FamilyMemberBreakdownResponse>> getByMemberBreakdown(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(required = false) String month) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                familySummaryService.getByMemberBreakdown(principal.getUsername(), id, month)));
    }

    @GetMapping("/{id}/summary/trend")
    public ResponseEntity<ApiResponse<FamilyTrendResponse>> getFamilyTrend(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") Integer months) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(familySummaryService.getGroupTrend(principal.getUsername(), id, months)));
    }

    @DeleteMapping("/{id}/transactions/{transactionId}")
    public ResponseEntity<Void> deleteSharedTransaction(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long transactionId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        sharedTransactionService.deleteSharedTransaction(principal.getUsername(), id, transactionId);
        return ResponseEntity.noContent().build();
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
