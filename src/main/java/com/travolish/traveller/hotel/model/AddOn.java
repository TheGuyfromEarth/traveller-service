package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Double price;

    @Column(name = "hotel_id")
    private Long hotelId;

    private OffsetDateTime createdAt = OffsetDateTime.now();
}
