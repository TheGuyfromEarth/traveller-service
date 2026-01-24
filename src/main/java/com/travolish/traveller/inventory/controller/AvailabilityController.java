package com.travolish.traveller.inventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.inventory.dto.AvailabilityCheckDTO;
import com.travolish.traveller.inventory.service.AvailabilityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * Check if room is available on specific date
     */
    @GetMapping("/check/date")
    public ResponseEntity<Boolean> isAvailableOnDate(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Boolean available = availabilityService.isRoomAvailableOnDate(roomId, date);
        return ResponseEntity.ok(available);
    }

    /**
     * Check if room is available for date range
     */
    @GetMapping("/check/range")
    public ResponseEntity<Boolean> isAvailableForDateRange(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        Boolean available = availabilityService.isRoomAvailableForDateRange(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(available);
    }

    /**
     * Get availability for specific date
     */
    @GetMapping("/{roomId}/date")
    public ResponseEntity<AvailabilityCheckDTO> getAvailabilityForDate(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        AvailabilityCheckDTO availability = availabilityService.getAvailabilityForDate(roomId, date);
        return ResponseEntity.ok(availability);
    }

    /**
     * Get availability for date range
     */
    @GetMapping("/{roomId}/range")
    public ResponseEntity<List<AvailabilityCheckDTO>> getAvailabilityForDateRange(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        List<AvailabilityCheckDTO> availabilities = availabilityService
            .getAvailabilityForDateRange(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(availabilities);
    }

    /**
     * Find available rooms for hotel on specific date
     */
    @GetMapping("/hotel/{hotelId}/date")
    public ResponseEntity<List<AvailabilityCheckDTO>> findAvailableRoomsOnDate(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AvailabilityCheckDTO> rooms = availabilityService.findAvailableRoomsOnDate(hotelId, date);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Find available rooms for hotel in date range
     */
    @GetMapping("/hotel/{hotelId}/range")
    public ResponseEntity<List<Long>> findAvailableRoomsInDateRange(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        List<Long> roomIds = availabilityService
            .findAvailableRoomsInDateRange(hotelId, checkInDate, checkOutDate);
        return ResponseEntity.ok(roomIds);
    }

    /**
     * Book room (reduce available count)
     */
    @PostMapping("/{roomId}/book")
    @ResponseStatus(HttpStatus.OK)
    public void bookRoom(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        availabilityService.bookRoom(roomId, checkInDate, checkOutDate);
    }

    /**
     * Cancel booking (increase available count)
     */
    @PostMapping("/{roomId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public void cancelBooking(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        availabilityService.cancelBooking(roomId, checkInDate, checkOutDate);
    }

    /**
     * Block rooms for maintenance
     */
    @PostMapping("/{roomId}/block")
    @ResponseStatus(HttpStatus.OK)
    public void blockRoomsForMaintenance(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Integer count,
        @RequestParam(required = false) String reason) {
        
        availabilityService.blockRoomsForMaintenance(roomId, date, count, reason);
    }

    /**
     * Unblock rooms
     */
    @PostMapping("/{roomId}/unblock")
    @ResponseStatus(HttpStatus.OK)
    public void unblockRooms(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Integer count) {
        
        availabilityService.unblockRooms(roomId, date, count);
    }

    /**
     * Get hotel occupancy on specific date
     */
    @GetMapping("/hotel/{hotelId}/occupancy/date")
    public ResponseEntity<AvailabilityCheckDTO> getHotelOccupancyOnDate(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        AvailabilityCheckDTO occupancy = availabilityService.getHotelOccupancyOnDate(hotelId, date);
        return ResponseEntity.ok(occupancy);
    }

    /**
     * Get hotel occupancy for date range
     */
    @GetMapping("/hotel/{hotelId}/occupancy/range")
    public ResponseEntity<List<AvailabilityCheckDTO>> getHotelOccupancyForDateRange(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<AvailabilityCheckDTO> occupancies = availabilityService
            .getHotelOccupancyForDateRange(hotelId, startDate, endDate);
        return ResponseEntity.ok(occupancies);
    }

    /**
     * Calculate average occupancy for hotel
     */
    @GetMapping("/hotel/{hotelId}/average-occupancy")
    public ResponseEntity<Double> calculateAverageOccupancy(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Double avgOccupancy = availabilityService.calculateAverageOccupancy(hotelId, startDate, endDate);
        return ResponseEntity.ok(avgOccupancy);
    }

    /**
     * Check for booking conflict
     */
    @GetMapping("/{roomId}/conflict")
    public ResponseEntity<Boolean> hasBookingConflict(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        Boolean conflict = availabilityService.hasBookingConflict(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(conflict);
    }

    /**
     * Get rooms with limited availability
     */
    @GetMapping("/hotel/{hotelId}/limited-availability")
    public ResponseEntity<List<AvailabilityCheckDTO>> getRoomsWithLimitedAvailability(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AvailabilityCheckDTO> rooms = availabilityService
            .getRoomsWithLimitedAvailability(hotelId, date);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Initialize room availability
     */
    @PostMapping("/{roomId}/initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public void initializeRoomAvailability(
        @PathVariable Long roomId,
        @RequestParam Long hotelId,
        @RequestParam Integer roomCount,
        @RequestParam(defaultValue = "365") Integer daysAhead) {
        
        availabilityService.initializeRoomAvailability(hotelId, roomId, roomCount, daysAhead);
    }
}
