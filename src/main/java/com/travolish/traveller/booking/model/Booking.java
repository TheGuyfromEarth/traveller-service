package com.travolish.traveller.booking.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_room_id",       columnList = "roomId"),
    @Index(name = "idx_bookings_hotel_id",      columnList = "hotelId"),
    @Index(name = "idx_bookings_user_id",       columnList = "userId"),
    @Index(name = "idx_bookings_status",        columnList = "status"),
    @Index(name = "idx_bookings_check_in",      columnList = "checkInDate"),
    @Index(name = "idx_bookings_check_out",     columnList = "checkOutDate"),
    // Composite index: covers the availability conflict query
    // (roomId + status + checkInDate + checkOutDate) as a single index seek
    @Index(name = "idx_bookings_avail",         columnList = "roomId,status,checkInDate,checkOutDate"),
    // Composite index: covers findByUserIdOrderByCreatedAtDesc without a sort step
    @Index(name = "idx_bookings_user_created",  columnList = "userId,createdAt"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long roomId;

    @NotNull
    private Long hotelId;

    // nullable — only set when the booker is an authenticated user
    private Long userId;

    @NotNull
    private String guestName;

    private String guestEmail;

    private String guestPhone;

    @NotNull
    private LocalDate checkInDate;

    @NotNull
    private LocalDate checkOutDate;

    @NotNull
    private Double basePrice;

    private Double seasonalAdjustment = 0.0;

    private Double dynamicPricingAdjustment = 0.0;

    private Double promotionalDiscount = 0.0;

    @NotNull
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    private String notes;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

}
