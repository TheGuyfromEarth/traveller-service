package com.travolish.traveller.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.booking.dto.BookingPriceDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> list(@RequestParam(required = false) String guestEmail) {
        if (guestEmail != null && !guestEmail.isBlank()) {
            return bookingService.findByGuestEmail(guestEmail);
        }
        return bookingService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> get(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Validated @RequestBody Booking booking) {
        return bookingService.create(booking);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @Validated @RequestBody Booking booking) {
        return bookingService.update(id, booking)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }

    /**
     * Check room availability for a date range
     * GET /api/bookings/check-availability?roomId=1&checkInDate=2025-12-01&checkOutDate=2025-12-05
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        Boolean isAvailable = bookingService.checkAvailability(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * Calculate booking price without creating the booking
     * GET /api/bookings/calculate-price?roomId=1&basePrice=100&checkInDate=2025-12-01&checkOutDate=2025-12-05
     */
    @GetMapping("/calculate-price")
    public ResponseEntity<BookingPriceDTO> calculatePrice(
            @RequestParam Long roomId,
            @RequestParam Double basePrice,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        BookingPriceDTO priceDTO = bookingService.calculateBookingPrice(roomId, basePrice, checkInDate, checkOutDate);
        return ResponseEntity.ok(priceDTO);
    }

    /**
     * Get all bookings for a room
     * GET /api/bookings/room/1
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Booking>> getBookingsByRoom(@PathVariable Long roomId) {
        List<Booking> bookings = bookingService.findByRoomId(roomId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get all bookings for a hotel
     * GET /api/bookings/hotel/1
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<Booking>> getBookingsByHotel(@PathVariable Long hotelId) {
        List<Booking> bookings = bookingService.findByHotelId(hotelId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get confirmed bookings for a room in a date range
     * GET /api/bookings/room/1/confirmed?startDate=2025-12-01&endDate=2025-12-31
     */
    @GetMapping("/room/{roomId}/confirmed")
    public ResponseEntity<List<Booking>> getConfirmedBookings(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Booking> bookings = bookingService.findConfirmedBookingsInDateRange(roomId, startDate, endDate);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Cancel a booking by ID
     * POST /api/bookings/1/cancel
     */
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }
}
