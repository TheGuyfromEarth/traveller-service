package com.travolish.traveller.auth.repository;

import com.travolish.traveller.auth.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    /**
     * Most recent unconsumed code for the given email — the one a verify attempt
     * should be checked against.
     */
    @Query("SELECT c FROM EmailVerificationCode c WHERE c.email = :email AND c.consumed = false ORDER BY c.id DESC LIMIT 1")
    Optional<EmailVerificationCode> findLatestActiveByEmail(@Param("email") String email);
}
