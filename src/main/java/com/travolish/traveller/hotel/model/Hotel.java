package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.travolish.traveller.review.model.Review;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hostId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private String country;

    private Double rating;

    private String phone;

    private String email;

    @Column(length = 2000)
    private String description;

    private String imageUrl;    // cover / primary image

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_gallery_images", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "image_url")
    private List<String> galleryImages = new ArrayList<>();

    private String videoUrl;

    private Double latitude;

    private Double longitude;

    @Column(length = 1000)
    private String houseRules;

    private Boolean instantBooking = true;

    private Integer minimumStay = 1;

    private String checkInTime;

    private String checkOutTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column // nullable so Hibernate can ADD COLUMN on existing tables; SchemaMigrationRunner backfills LIVE
    private HotelStatus status;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum HotelStatus {
        DRAFT,   // Created but not yet published
        LIVE,    // Published and bookable
        PAUSED   // Temporarily hidden from search
    }

    @JsonIgnore
    @OneToMany(mappedBy = "hotelId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews;

}
