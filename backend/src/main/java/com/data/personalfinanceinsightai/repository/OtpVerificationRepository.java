package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.OtpVerification;
import com.data.personalfinanceinsightai.entity.enums.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}
