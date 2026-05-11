package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.defaultAccount = false WHERE a.user.id = :userId")
    void clearDefaultAccountsForUser(@Param("userId") Long userId);
}
