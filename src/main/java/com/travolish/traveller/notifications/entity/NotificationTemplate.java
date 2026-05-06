package com.travolish.traveller.notifications.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_type", columnList = "template_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "template_type", nullable = false, unique = true, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(name = "template_name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "subject_template", length = 200)
    private String subjectTemplate; // e.g., "Booking Confirmation - {bookingId}"
    
    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate; // e.g., "Dear {guestName}, your booking..."
    
    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate; // HTML version for emails
    
    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "retry_enabled", nullable = false)
    private Boolean retryEnabled = true;
    
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
