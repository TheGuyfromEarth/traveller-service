package com.travolish.traveller.auth.repository;

import com.travolish.traveller.auth.entity.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, Long> {

    @Query("SELECT c FROM PhoneVerificationCode c WHERE c.userId = :userId AND c.consumed = false ORDER BY c.createdAt DESC")
    Optional<PhoneVerificationCode> findLatestActiveByUserId(@Param("userId") Long userId);
}
