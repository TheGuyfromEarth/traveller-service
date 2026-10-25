package com.travolish.traveller.hosttools.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_reply_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoReplyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateCategory category;

    @Column(nullable = false)
    private String triggerKeyword;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String templateText;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer usageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastUsedAt;

    private String language;

    private Boolean isGlobal;

    private Integer displayOrder;

    public enum TemplateCategory {
        BOOKING_CONFIRMATION,
        CHECK_IN_INSTRUCTIONS,
        CHECK_OUT_REMINDER,
        PAYMENT_INQUIRY,
        CANCELLATION,
        HOUSE_RULES,
        WIFI_PASSWORD,
        LOCAL_ATTRACTIONS,
        EMERGENCY_CONTACT,
        CLEANING_INFO,
        CHECKOUT_PROCEDURE,
        GUEST_FEEDBACK,
        COMPLAINT_RESPONSE,
        GENERAL_INQUIRY,
        CUSTOM
    }

    public enum TemplateStatus {
        DRAFT,
        ACTIVE,
        ARCHIVED,
        INACTIVE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        usageCount = 0;
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
