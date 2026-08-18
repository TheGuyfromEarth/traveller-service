package com.travolish.traveller.hotel.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nearby_attractions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyAttraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    // §6 — Nearby attraction details
    @Column(nullable = false)
    private String name;

    private String distanceText;

    @Enumerated(EnumType.STRING)
    private AttractionType attractionType;

    public enum AttractionType {
        AIRPORT,
        TRAIN_STATION,
        METRO,
        BEACH,
        CITY_CENTRE,
        LANDMARK,
        RESTAURANT,
        SHOPPING,
        HOSPITAL,
        PARK,
        OTHER
    }
}
