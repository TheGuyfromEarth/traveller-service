package com.travolish.traveller.admin.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_audit_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", length = 50)
    private String entityType;   // USER, KYC, HOTEL, REVIEW, CATALOG

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 100)
    private String action;       // e.g. "KYC_APPROVED", "USER_SUSPENDED"

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
