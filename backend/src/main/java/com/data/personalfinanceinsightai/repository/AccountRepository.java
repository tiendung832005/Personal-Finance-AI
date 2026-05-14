package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Account;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.defaultAccount = false WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    void clearDefaultAccountsForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    BigDecimal sumBalanceForUser(@Param("userId") Long userId);
}
