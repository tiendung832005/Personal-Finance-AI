package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIdAndUser_Id(Long id, Long userId);

    Optional<Account> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    List<Account> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Account> findByUser_IdAndFamilyIdIsNullAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId, AccountScope scope);

    List<Account> findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long familyId, AccountScope scope);

    @Query(
            value =
                    """
            SELECT
                a.balance
                + COALESCE(SUM(CASE WHEN t.type = 'INCOME'  THEN t.amount ELSE 0 END), 0)
                - COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
            FROM accounts a
            LEFT JOIN transactions t ON t.account_id = a.id
                AND t.deleted_at IS NULL
            WHERE a.id = :accountId
              AND a.family_id IS NOT NULL
              AND a.deleted_at IS NULL
            GROUP BY a.id, a.balance
            """,
            nativeQuery = true)
    BigDecimal calculateGroupAccountCurrentBalance(@Param("accountId") Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.defaultAccount = false WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    void clearDefaultAccountsForUser(@Param("userId") Long userId);

    @Query(
            "SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId "
                    + "AND a.familyId IS NULL AND a.scope = com.data.personalfinanceinsightai.entity.enums.AccountScope.PERSONAL "
                    + "AND a.deletedAt IS NULL")
    BigDecimal sumBalanceForUser(@Param("userId") Long userId);
}
