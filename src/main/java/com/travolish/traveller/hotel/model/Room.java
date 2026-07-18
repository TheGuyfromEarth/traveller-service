package com.travolish.traveller.hotel.model;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.travolish.traveller.review.model.Review;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;

    // §8 — Room type (SINGLE, DOUBLE, SUITE, etc.)
    private String type;

    // §8 — Room name (e.g. "Deluxe King Room")
    private String name;

    @Column(length = 2000)
    private String description;

    // §8 — Room size in square metres
    private Double size;

    // §8 — Max guests
    private Integer capacity = 2;

    // §8 — Bed configuration
    @Enumerated(EnumType.STRING)
    private BedType bedType;

    private Integer numberOfBeds = 1;

    // §8 — View type (Sea, City, Garden, Pool, Mountain, etc.)
    private String view;

    // §8 — Room flags
    private Boolean smokingAllowed = false;

    private Boolean accessibleRoom = false;

    private Boolean privateBathroom = true;

    // §8 — Photos
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_photos", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    // §23 — Room-level amenities
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    // §9 — Pricing
    private Double pricePerNight;

    private Double weekendPrice;

    private Double seasonalPrice;

    private Double holidayPrice;

    private Double weeklyDiscount;

    private Double monthlyDiscount;

    private Double taxes;

    private Double serviceCharges;

    private Double securityDeposit;

    private String currency = "USD";

    private Boolean available = true;

    @Column(name = "hotel_id")
    private Long hotelId;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum BedType {
        KING,
        QUEEN,
        DOUBLE,
        SINGLE,
        TWIN,
        BUNK
    }

    @JsonIgnore
    @OneToMany(mappedBy = "roomId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews;

}
