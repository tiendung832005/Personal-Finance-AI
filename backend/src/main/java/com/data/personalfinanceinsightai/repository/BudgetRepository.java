package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser_IdAndFamilyIdIsNullAndMonthOrderByIdAsc(Long userId, String month);

    boolean existsByUser_IdAndFamilyIdIsNullAndCategory_IdAndMonth(Long userId, Long categoryId, String month);

    Optional<Budget> findByIdAndUser_IdAndFamilyIdIsNull(Long id, Long userId);
}
