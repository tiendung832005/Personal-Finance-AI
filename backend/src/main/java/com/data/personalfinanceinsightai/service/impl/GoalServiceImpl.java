package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.goal.CreateGoalRequest;
import com.data.personalfinanceinsightai.dto.request.goal.UpdateGoalRequest;
import com.data.personalfinanceinsightai.dto.response.goal.GoalDetailResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalProgressResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalProgressResponse.ProgressSummary;
import com.data.personalfinanceinsightai.dto.response.goal.GoalResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalSnapshotResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.entity.GoalMonthlySnapshot;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.FinancialGoalRepository;
import com.data.personalfinanceinsightai.repository.GoalMonthlySnapshotRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GoalProgressCalculator;
import com.data.personalfinanceinsightai.service.GoalService;
import com.data.personalfinanceinsightai.service.goal.GoalSnapshotService;
import com.data.personalfinanceinsightai.service.goal.GoalStatusService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final FinancialGoalRepository goalRepository;
    private final GoalMonthlySnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final GoalProgressCalculator progressCalculator;
    private final GoalStatusService goalStatusService;
    private final GoalSnapshotService goalSnapshotService;
    private final Clock clock;

    @Override
    @Transactional
    public GoalResponse create(String email, CreateGoalRequest request) {
        User user = findUser(email);
        validateDeadline(request.getDeadline());
        validateLinkedAccount(user.getId(), request.getLinkedAccountId());

        FinancialGoal goal = FinancialGoal.builder()
                .userId(user.getId())
                .name(request.getName().trim())
                .targetAmount(request.getTargetAmount())
                .deadline(request.getDeadline())
                .linkedAccountId(request.getLinkedAccountId())
                .status(GoalStatus.ACTIVE)
                .build();

        return progressCalculator.buildGoalResponse(goalStatusService.updateGoalStatus(goalRepository.save(goal)));
    }

    @Override
    @Transactional
    public List<GoalResponse> listForUser(String email) {
        User user = findUser(email);
        return goalRepository.findByUserIdOrderByDeadlineAsc(user.getId()).stream()
                .map(goalStatusService::updateGoalStatus)
                .map(progressCalculator::buildGoalResponse)
                .toList();
    }

    @Override
    @Transactional
    public GoalDetailResponse getById(String email, Long id) {
        User user = findUser(email);
        FinancialGoal goal = goalStatusService.updateGoalStatus(findGoal(id, user.getId()));
        List<GoalSnapshotResponse> snapshots = snapshotRepository.findByGoalIdOrderByMonthAsc(goal.getId()).stream()
                .map(GoalSnapshotResponse::fromEntity)
                .toList();

        return GoalDetailResponse.builder()
                .goal(progressCalculator.buildGoalResponse(goal))
                .aiPlan(goal.getAiPlan())
                .aiPlanGeneratedAt(goal.getAiPlanGeneratedAt())
                .monthlySnapshots(snapshots)
                .build();
    }

    @Override
    @Transactional
    public GoalResponse update(String email, Long id, UpdateGoalRequest request) {
        User user = findUser(email);
        FinancialGoal goal = findGoal(id, user.getId());

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new IllegalArgumentException("Goal name is required");
            }
            goal.setName(name);
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getDeadline() != null) {
            validateDeadline(request.getDeadline());
            goal.setDeadline(request.getDeadline());
        }
        if (request.getLinkedAccountId() != null) {
            validateLinkedAccount(user.getId(), request.getLinkedAccountId());
            goal.setLinkedAccountId(request.getLinkedAccountId());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }

        return progressCalculator.buildGoalResponse(goalStatusService.updateGoalStatus(goalRepository.save(goal)));
    }

    @Override
    @Transactional
    public void delete(String email, Long id) {
        User user = findUser(email);
        FinancialGoal goal = findGoal(id, user.getId());
        goalRepository.delete(goal);
    }

    @Override
    @Transactional
    public GoalProgressResponse getProgress(String email, Long id) {
        User user = findUser(email);
        FinancialGoal goal = goalStatusService.updateGoalStatus(findGoal(id, user.getId()));
        goalSnapshotService.takeMonthlySnapshot(goal.getId());
        List<GoalMonthlySnapshot> snapshots = snapshotRepository.findByGoalIdOrderByMonthAsc(goal.getId());
        List<GoalSnapshotResponse> history = snapshots.stream()
                .map(GoalSnapshotResponse::fromEntity)
                .toList();

        long onTrackMonths = snapshots.stream()
                .filter(snapshot -> Boolean.TRUE.equals(snapshot.getOnTrack()))
                .count();
        long behindMonths = snapshots.size() - onTrackMonths;
        long aheadMonths = snapshots.stream()
                .filter(snapshot -> snapshot.getSavedAmount() != null
                        && snapshot.getPlannedAmount() != null
                        && snapshot.getSavedAmount().compareTo(snapshot.getPlannedAmount()) > 0)
                .count();

        return GoalProgressResponse.builder()
                .goal(progressCalculator.buildGoalResponse(goal))
                .monthlyHistory(history)
                .summary(ProgressSummary.builder()
                        .totalMonths((long) snapshots.size())
                        .onTrackMonths(onTrackMonths)
                        .behindMonths(behindMonths)
                        .aheadMonths(aheadMonths)
                        .build())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private FinancialGoal findGoal(Long id, Long userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private void validateLinkedAccount(Long userId, Long accountId) {
        if (accountId == null) {
            return;
        }
        Account account = accountRepository.findByIdAndUser_IdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));
        if (account.getScope() != AccountScope.PERSONAL || account.getFamilyId() != null) {
            throw new IllegalArgumentException("Linked account must be a personal account");
        }
    }

    private void validateDeadline(LocalDate deadline) {
        if (deadline == null) {
            return;
        }
        if (!deadline.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("deadline must be a future date");
        }
    }
}
