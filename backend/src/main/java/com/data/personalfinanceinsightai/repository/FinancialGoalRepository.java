package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.FinancialGoal;
import com.data.personalfinanceinsightai.entity.enums.GoalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    List<FinancialGoal> findByUserIdOrderByDeadlineAsc(Long userId);

    List<FinancialGoal> findByUserIdAndStatus(Long userId, GoalStatus status);

    List<FinancialGoal> findByStatus(GoalStatus status);

    Optional<FinancialGoal> findByIdAndUserId(Long id, Long userId);

    List<FinancialGoal> findByLinkedAccountId(Long accountId);
}
