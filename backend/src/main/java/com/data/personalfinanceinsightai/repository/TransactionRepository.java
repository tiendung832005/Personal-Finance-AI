package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    long countByAccount_Id(Long accountId);

    Optional<Transaction> findByIdAndFamilyIdAndScopeAndDeletedAtIsNull(
            Long id, Long familyId, TransactionScope scope);

    @Query(
            "SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL "
                    + "AND t.familyId IS NULL AND t.scope = com.data.personalfinanceinsightai.entity.enums.TransactionScope.PERSONAL "
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
            """
            SELECT t FROM Transaction t
            JOIN FETCH t.user
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category c
            WHERE t.familyId = :groupId
              AND t.scope = com.data.personalfinanceinsightai.entity.enums.TransactionScope.SHARED
              AND t.deletedAt IS NULL
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:type IS NULL OR t.type = :type)
              AND (:monthStart IS NULL OR (t.transactionDate >= :monthStart AND t.transactionDate < :monthEnd))
            ORDER BY t.transactionDate DESC, t.createdAt DESC
            """)
    List<Transaction> findSharedTransactionsWithFilters(
            @Param("groupId") Long groupId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            Pageable pageable);

    @Query(
            """
            SELECT COUNT(t) FROM Transaction t
            LEFT JOIN t.category c
            WHERE t.familyId = :groupId
              AND t.scope = com.data.personalfinanceinsightai.entity.enums.TransactionScope.SHARED
              AND t.deletedAt IS NULL
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:type IS NULL OR t.type = :type)
              AND (:monthStart IS NULL OR (t.transactionDate >= :monthStart AND t.transactionDate < :monthEnd))
            """)
    long countSharedTransactionsWithFilters(
            @Param("groupId") Long groupId,
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
              AND scope = 'PERSONAL'
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
                AND t.scope = 'PERSONAL'
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
              AND scope = 'PERSONAL'
              AND transaction_date >= :fromInclusive
              AND transaction_date <= :toInclusive
            GROUP BY DATE_FORMAT(transaction_date, '%Y-%m'), type
            """,
            nativeQuery = true)
    List<Object[]> sumAmountByTypeGroupedByMonthRange(
            @Param("userId") Long userId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);

    @Query(
            value =
                    """
            SELECT type, COALESCE(SUM(amount), 0) AS total
            FROM transactions
            WHERE family_id = :groupId
              AND scope = 'SHARED'
              AND deleted_at IS NULL
              AND DATE_FORMAT(transaction_date, '%Y-%m') = :month
            GROUP BY type
            """,
            nativeQuery = true)
    List<Object[]> sumAmountByTypeForGroupAndMonth(@Param("groupId") Long groupId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT c.id, c.name, c.type,
                   COALESCE(SUM(t.amount), 0) AS total,
                   COUNT(t.id) AS txn_count
            FROM categories c
            INNER JOIN transactions t ON t.category_id = c.id
                AND t.family_id = :groupId
                AND t.scope = 'SHARED'
                AND t.deleted_at IS NULL
                AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
            GROUP BY c.id, c.name, c.type
            ORDER BY total DESC
            """,
            nativeQuery = true)
    List<Object[]> categoryBreakdownForGroupAndMonth(@Param("groupId") Long groupId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT
                t.user_id,
                u.full_name,
                COALESCE(SUM(CASE WHEN t.type = 'INCOME'  THEN t.amount ELSE 0 END), 0) AS total_income,
                COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS total_expense,
                COUNT(t.id) AS txn_count
            FROM transactions t
            JOIN users u ON u.id = t.user_id
            WHERE t.family_id = :groupId
              AND t.scope = 'SHARED'
              AND t.deleted_at IS NULL
              AND DATE_FORMAT(t.transaction_date, '%Y-%m') = :month
            GROUP BY t.user_id, u.full_name
            ORDER BY total_expense DESC
            """,
            nativeQuery = true)
    List<Object[]> memberBreakdownForGroupAndMonth(@Param("groupId") Long groupId, @Param("month") String month);

    @Query(
            value =
                    """
            SELECT DATE_FORMAT(transaction_date, '%Y-%m') AS ym,
                   type,
                   COALESCE(SUM(amount), 0) AS total
            FROM transactions
            WHERE family_id = :groupId
              AND scope = 'SHARED'
              AND deleted_at IS NULL
              AND transaction_date >= :fromInclusive
              AND transaction_date <= :toInclusive
            GROUP BY DATE_FORMAT(transaction_date, '%Y-%m'), type
            """,
            nativeQuery = true)
    List<Object[]> sumAmountByTypeGroupedByMonthRangeForGroup(
            @Param("groupId") Long groupId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);
}
