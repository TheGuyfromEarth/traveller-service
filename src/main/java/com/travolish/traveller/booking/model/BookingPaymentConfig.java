package com.travolish.traveller.booking.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * §24 — Booking & Payment configuration per property.
 * Defines advance payment %, accepted methods, and payment flow.
 * When advance payment is received, booking status auto-changes to CONFIRMED.
 */
@Entity
@Table(name = "booking_payment_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPaymentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false, unique = true)
    private Long hotelId;

    // §24 — Payment method options
    private Boolean payFullAtBooking = true;

    private Boolean payAtProperty = false;

    private Boolean secureWithPartialPayment = false;

    // §24 — Advance payment percent (0, 10, 20, 25, 30, or custom)
    @Builder.Default
    private Integer advancePaymentPercent = 0;

    // §24 — Accepted payment methods (e.g. "CARD", "UPI", "NET_BANKING", "WALLET")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_payment_methods", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "payment_method")
    @Builder.Default
    private List<String> acceptedPaymentMethods = new ArrayList<>();

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
