package com.travolish.traveller.hotel.model;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

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

    private Boolean available = true;

    // store only the hotel's primary key
    @Column(name = "hotel_id")
    private Long hotelId;

    private OffsetDateTime createdAt = OffsetDateTime.now();

}
