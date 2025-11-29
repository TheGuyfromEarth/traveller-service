package com.travolish.traveller.booking.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.booking.service.BookingService;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public Booking create(Booking booking) {
        booking.setId(null);
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setUpdatedAt(OffsetDateTime.now());
        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> update(Long id, Booking booking) {
        return bookingRepository.findById(id).map(existing -> {
            existing.setRoomId(booking.getRoomId());
            existing.setHotelId(booking.getHotelId());
            existing.setGuestName(booking.getGuestName());
            existing.setGuestEmail(booking.getGuestEmail());
            existing.setGuestPhone(booking.getGuestPhone());
            existing.setCheckInDate(booking.getCheckInDate());
            existing.setCheckOutDate(booking.getCheckOutDate());
            existing.setTotalPrice(booking.getTotalPrice());
            existing.setStatus(booking.getStatus());
            existing.setNotes(booking.getNotes());
            existing.setUpdatedAt(OffsetDateTime.now());
            return bookingRepository.save(existing);
        });
    }

    @Override
    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }
}
