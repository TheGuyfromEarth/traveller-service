package com.travolish.traveller.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travolish.traveller.booking.model.BookingPaymentConfig;

public interface BookingPaymentConfigRepository extends JpaRepository<BookingPaymentConfig, Long> {
    Optional<BookingPaymentConfig> findByHotelId(Long hotelId);
}
