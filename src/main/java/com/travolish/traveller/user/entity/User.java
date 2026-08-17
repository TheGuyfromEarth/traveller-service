package com.travolish.traveller.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email",    columnList = "email"),
    @Index(name = "idx_users_provider", columnList = "provider,providerId"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String supabaseId;
    
    private String firstName;
    private String lastName;
    private String preferredName;
    private String email;
    private String password;
    private String phone;
    private String city;
    private String timeZone;
    private String travelStyle;
    @Column(columnDefinition = "TEXT")
    private String bio;
    // OAuth2 provider name (google, github, etc.)
    private String provider;
    // Provider-specific id (sub for Google)
    private String providerId;
    // S3 object key for profile image
    private String imageKey;

    // Admin fields — nullable; null means GUEST / ACTIVE
    private String role;    // ADMIN, HOST, GUEST
    private String status;  // ACTIVE, SUSPENDED, PENDING, BLACKLISTED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
