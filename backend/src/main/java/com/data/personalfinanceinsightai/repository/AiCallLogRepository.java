package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {
}
