package com.travolish.traveller.notifications.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs", indexes = {
    @Index(name = "idx_email_log_sent_at",    columnList = "sent_at"),
    @Index(name = "idx_email_log_status",     columnList = "status"),
    @Index(name = "idx_email_log_recipient",  columnList = "recipient")
})
@Data @NoArgsConstructor @AllArgsConstructor
public class EmailLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Recipient email address */
    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 300)
    private String subject;

    /** Message body — truncated to 5 000 chars for storage */
    @Column(columnDefinition = "TEXT")
    private String body;

    /** PLAIN | HTML | ATTACHMENT | BATCH */
    @Column(name = "email_type", nullable = false, length = 20)
    private String emailType;

    /** SENT | FAILED | SKIPPED (SMTP not configured) */
    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
