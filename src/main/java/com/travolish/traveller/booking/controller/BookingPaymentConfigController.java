package com.travolish.traveller.booking.controller;

import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.booking.dto.BookingPaymentConfigDTO;
import com.travolish.traveller.booking.model.BookingPaymentConfig;
import com.travolish.traveller.booking.repository.BookingPaymentConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * §24 — Booking & payment configuration per property.
 * Hosts configure advance payment %, payment methods, and payment flow.
 */
@RestController
@RequestMapping("/api/hotels/{hotelId}/payment-config")
@RequiredArgsConstructor
public class BookingPaymentConfigController {

    private final BookingPaymentConfigRepository configRepository;

    @GetMapping
    public ResponseEntity<BookingPaymentConfigDTO> getConfig(@PathVariable Long hotelId) {
        return configRepository.findByHotelId(hotelId)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<BookingPaymentConfigDTO> upsertConfig(
            @PathVariable Long hotelId,
            @RequestBody BookingPaymentConfigDTO dto) {

        BookingPaymentConfig config = configRepository.findByHotelId(hotelId)
            .orElse(BookingPaymentConfig.builder().hotelId(hotelId).build());

        config.setPayFullAtBooking(dto.getPayFullAtBooking());
        config.setPayAtProperty(dto.getPayAtProperty());
        config.setSecureWithPartialPayment(dto.getSecureWithPartialPayment());
        config.setAdvancePaymentPercent(dto.getAdvancePaymentPercent());
        if (dto.getAcceptedPaymentMethods() != null)
            config.setAcceptedPaymentMethods(dto.getAcceptedPaymentMethods());
        config.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(toDTO(configRepository.save(config)));
    }

    private BookingPaymentConfigDTO toDTO(BookingPaymentConfig c) {
        return BookingPaymentConfigDTO.builder()
            .hotelId(c.getHotelId())
            .payFullAtBooking(c.getPayFullAtBooking())
            .payAtProperty(c.getPayAtProperty())
            .secureWithPartialPayment(c.getSecureWithPartialPayment())
            .advancePaymentPercent(c.getAdvancePaymentPercent())
            .acceptedPaymentMethods(c.getAcceptedPaymentMethods())
            .build();
    }
}
