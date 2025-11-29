package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel_change_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null for new hotel creations
    private Long hotelId;

    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    // Snapshot of hotel fields proposed
    private String name;
    private String address;
    private String city;
    private Double rating;
    private String phone;
    private String email;
    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private OffsetDateTime requestedAt = OffsetDateTime.now();
    private OffsetDateTime processedAt;
    private String adminComment;

    public enum RequestType {CREATE, UPDATE}
    public enum RequestStatus {PENDING, APPROVED, REJECTED}
}
