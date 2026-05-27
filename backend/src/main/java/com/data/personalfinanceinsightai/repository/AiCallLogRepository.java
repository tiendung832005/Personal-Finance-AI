package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.data.personalfinanceinsightai.entity.User;
import java.time.LocalDateTime;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {
    long countByUserAndFeatureAndCreatedAtAfter(User user, String feature, LocalDateTime after);
}

