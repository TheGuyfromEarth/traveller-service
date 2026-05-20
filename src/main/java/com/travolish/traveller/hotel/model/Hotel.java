package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

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

    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "hotelId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews;

}
