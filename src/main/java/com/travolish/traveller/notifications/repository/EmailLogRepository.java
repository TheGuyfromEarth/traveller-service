package com.travolish.traveller.notifications.repository;

import com.travolish.traveller.notifications.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByStatus(String status, Pageable pageable);

    Page<EmailLog> findByRecipientContainingIgnoreCase(String recipient, Pageable pageable);

    Page<EmailLog> findByStatusAndRecipientContainingIgnoreCase(String status, String recipient, Pageable pageable);

    long countByStatus(String status);
}
