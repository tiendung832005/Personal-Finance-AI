package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.GoalMonthlySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalMonthlySnapshotRepository extends JpaRepository<GoalMonthlySnapshot, Long> {

    List<GoalMonthlySnapshot> findByGoalIdOrderByMonthAsc(Long goalId);

    Optional<GoalMonthlySnapshot> findByGoalIdAndMonth(Long goalId, LocalDate month);
}
