package com.travolish.traveller.booking.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.booking.dto.BookingPriceDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.service.BookingService;
import com.travolish.traveller.hotel.repository.HotelRepository;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final com.travolish.traveller.booking.service.BookingStatusScheduler bookingStatusScheduler;
    private final HotelRepository hotelRepository;

    public BookingController(BookingService bookingService,
                             com.travolish.traveller.booking.service.BookingStatusScheduler bookingStatusScheduler,
                             HotelRepository hotelRepository) {
        this.bookingService = bookingService;
        this.bookingStatusScheduler = bookingStatusScheduler;
        this.hotelRepository = hotelRepository;
    }

    /**
     * Admin-enriched booking list — paginated, server-side search and status filter.
     * Uses scalar hotel name lookup to avoid loading Hotel entity with 10 eager collections.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> listAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String searchPattern = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";

        Booking.BookingStatus statusEnum = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try { statusEnum = Booking.BookingStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<Booking> bookingPage = bookingService.findAdminBookings(
                searchPattern, statusEnum,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));

        Set<Long> hotelIds = bookingPage.getContent().stream()
                .map(Booking::getHotelId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> nameMap = hotelIds.isEmpty() ? Collections.emptyMap()
                : hotelRepository.findIdAndNameByIdIn(hotelIds).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (String) r[1]));

        List<Map<String, Object>> content = bookingPage.getContent().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          b.getId());
            m.put("hotelId",     b.getHotelId());
            m.put("hotelName",   b.getHotelId() != null ? nameMap.getOrDefault(b.getHotelId(), "Hotel #" + b.getHotelId()) : "—");
            m.put("userId",      b.getUserId());
            m.put("guestName",   b.getGuestName());
            m.put("guestEmail",  b.getGuestEmail());
            m.put("checkInDate", b.getCheckInDate());
            m.put("checkOutDate",b.getCheckOutDate());
            m.put("totalPrice",  b.getTotalPrice());
            m.put("status",      b.getStatus() != null ? b.getStatus().name() : null);
            m.put("notes",       b.getNotes());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       content);
        result.put("totalElements", bookingPage.getTotalElements());
        result.put("totalPages",    bookingPage.getTotalPages());
        result.put("page",          page);
        result.put("size",          size);
        return result;
    }

    /** On-demand status refresh — called by trips page on load so users never see stale PENDING for past stays. */
    @PostMapping("/refresh-statuses")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> refreshStatuses() {
        int updated = bookingStatusScheduler.runStatusTransition("on-demand");
        return org.springframework.http.ResponseEntity.ok(
            java.util.Map.of("transitioned", updated, "message",
                updated > 0 ? updated + " booking(s) marked COMPLETED" : "All statuses up to date"));
    }

    @GetMapping
    public List<Booking> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestEmail) {
        if (userId != null) {
            return bookingService.findByUserId(userId);
        }
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
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        boolean found = bookingService.cancelBooking(id);
        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Confirm a PENDING booking (admin action).
     * POST /api/bookings/1/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(b -> {
                    b.setStatus(Booking.BookingStatus.CONFIRMED);
                    return bookingService.update(id, b).orElse(b);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
