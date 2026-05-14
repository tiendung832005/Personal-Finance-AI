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
}
