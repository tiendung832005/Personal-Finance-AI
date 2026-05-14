package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    long countByAccount_Id(Long accountId);

    @Query(
            "SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL "
                    + "AND (:categoryId IS NULL OR t.category.id = :categoryId) "
                    + "AND (:type IS NULL OR t.type = :type) "
                    + "AND (:monthStart IS NULL OR (t.transactionDate >= :monthStart AND t.transactionDate < :monthEnd)) "
                    + "ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findForUserWithFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    @Query(
            value =
                    """
            SELECT type, COALESCE(SUM(amount), 0) AS total
            FROM transactions
            WHERE user_id = :userId
              AND deleted_at IS NULL
              AND family_id IS NULL
              AND DATE_FORMAT(transaction_date, '%Y-%m') = :month
            GROUP BY type
            """,
            nativeQuery = true)
    List<Object[]> sumAmountByTypeForUserAndMonth(@Param("userId") Long userId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT c.id, c.name, c.type,
                   COALESCE(SUM(t.amount), 0) AS total,
                   COUNT(t.id) AS txn_count
            FROM categories c
            INNER JOIN transactions t ON t.category_id = c.id
                AND t.user_id = :userId
                AND t.deleted_at IS NULL
                AND t.family_id IS NULL
                AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
            GROUP BY c.id, c.name, c.type
            ORDER BY total DESC
            """,
            nativeQuery = true)
    List<Object[]> categoryBreakdownForUserAndMonth(@Param("userId") Long userId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT DATE_FORMAT(transaction_date, '%Y-%m') AS ym,
                   type,
                   COALESCE(SUM(amount), 0) AS total
            FROM transactions
            WHERE user_id = :userId
              AND deleted_at IS NULL
              AND family_id IS NULL
              AND transaction_date >= :fromInclusive
              AND transaction_date <= :toInclusive
            GROUP BY DATE_FORMAT(transaction_date, '%Y-%m'), type
            """,
            nativeQuery = true)
    List<Object[]> sumAmountByTypeGroupedByMonthRange(
            @Param("userId") Long userId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);
}
