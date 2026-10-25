package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false, unique = true)
    private Long hotelId;

    // §12 — Policies
    @Column(length = 2000)
    private String cancellationPolicy;

    @Column(length = 2000)
    private String refundPolicy;

    @Column(length = 1000)
    private String childPolicy;

    @Column(length = 1000)
    private String petPolicy;

    @Column(length = 1000)
    private String smokingPolicy;

    @Column(length = 1000)
    private String visitorPolicy;

    @Column(length = 1000)
    private String damagePolicy;

    private String quietHours;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
