package com.travolish.traveller.notifications.whatsapp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_opt_ins", indexes = {
        @Index(name = "idx_whatsapp_opt_in_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppOptIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;

    @Column(name = "opted_in_at", nullable = false)
    private LocalDateTime optedInAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private WhatsAppOptInSource source;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
