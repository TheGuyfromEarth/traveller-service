package com.travolish.traveller.hotel.model;

import java.time.*;
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

    private String type; // SINGLE, DOUBLE, SUITE

    private Double pricePerNight;

    private Integer capacity = 2;  // max guests this room sleeps; defaults to 2

    private Boolean available = true;

    // store only the hotel's primary key
    @Column(name = "hotel_id")
    private Long hotelId;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "roomId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews;

}
