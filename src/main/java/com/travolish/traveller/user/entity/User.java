package com.travolish.traveller.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
}
