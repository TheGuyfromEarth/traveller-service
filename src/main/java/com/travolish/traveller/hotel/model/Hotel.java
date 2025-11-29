package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private OffsetDateTime createdAt = OffsetDateTime.now();

}
