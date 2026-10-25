package com.travolish.traveller.notifications.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WhatsAppOptInRepository extends JpaRepository<WhatsAppOptIn, Long> {

    @Query("SELECT o FROM WhatsAppOptIn o WHERE o.userId = :userId AND o.revokedAt IS NULL ORDER BY o.optedInAt DESC")
    Optional<WhatsAppOptIn> findActiveByUserId(@Param("userId") Long userId);
}
