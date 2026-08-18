package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * §18 — Contact details visible to admin/host only.
 * Per doc: contact must remain hidden until a confirmed booking.
 * No public API endpoint exposes these fields directly.
 */
@Entity
@Table(name = "property_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false, unique = true)
    private Long hotelId;

    private String contactPerson;

    private String phone;

    private String email;

    private String website;

    private String emergencyContact;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
