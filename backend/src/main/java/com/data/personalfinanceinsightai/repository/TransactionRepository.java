package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
