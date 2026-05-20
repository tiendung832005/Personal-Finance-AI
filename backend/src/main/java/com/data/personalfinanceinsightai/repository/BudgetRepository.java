package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser_IdAndFamilyIdIsNullAndMonthOrderByIdAsc(Long userId, String month);

    boolean existsByUser_IdAndFamilyIdIsNullAndCategory_IdAndMonth(Long userId, Long categoryId, String month);

    Optional<Budget> findByIdAndUser_IdAndFamilyIdIsNull(Long id, Long userId);

    List<Budget> findByFamilyIdAndMonthOrderByIdAsc(Long familyId, String month);

    boolean existsByFamilyIdAndCategory_IdAndMonth(Long familyId, Long categoryId, String month);

    Optional<Budget> findByIdAndFamilyId(Long id, Long familyId);

    @Query(
            value =
                    """
            SELECT b.id, b.category_id, c.name AS category_name,
                   b.amount AS budget_amount,
                   COALESCE(SUM(t.amount), 0) AS actual_amount
            FROM budgets b
            JOIN categories c ON c.id = b.category_id
            LEFT JOIN transactions t ON t.category_id = b.category_id
                AND t.user_id = :userId
                AND t.type = 'EXPENSE'
                AND t.deleted_at IS NULL
                AND t.family_id IS NULL
                AND t.scope = 'PERSONAL'
                AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
            WHERE b.user_id = :userId
              AND b.family_id IS NULL
              AND b.month = :month
            GROUP BY b.id, b.category_id, c.name, b.amount
            ORDER BY b.id
            """,
            nativeQuery = true)
    List<Object[]> aggregateBudgetSpendingForMonth(@Param("userId") Long userId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT b.id, b.category_id, c.name AS category_name,
                   b.amount AS budget_amount,
                   COALESCE(SUM(t.amount), 0) AS actual_amount
            FROM budgets b
            JOIN categories c ON c.id = b.category_id
            LEFT JOIN transactions t ON t.category_id = b.category_id
                AND t.family_id = :groupId
                AND t.scope = 'SHARED'
                AND t.type = 'EXPENSE'
                AND t.deleted_at IS NULL
                AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
            WHERE b.family_id = :groupId
              AND b.month = :month
            GROUP BY b.id, b.category_id, c.name, b.amount
            ORDER BY b.id
            """,
            nativeQuery = true)
    List<Object[]> aggregateGroupBudgetSpendingForMonth(@Param("groupId") Long groupId, @Param("month") String month);
}
