package com.travolish.traveller.booking.service;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.booking.model.Booking;

public interface BookingService {
    List<Booking> findAll();

    Optional<Booking> findById(Long id);

    Booking create(Booking booking);

    Optional<Booking> update(Long id, Booking booking);

    void delete(Long id);
}
