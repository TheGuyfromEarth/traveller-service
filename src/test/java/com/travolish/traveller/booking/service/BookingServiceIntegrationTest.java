package com.travolish.traveller.booking.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.booking.dto.BookingPriceDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.inventory.exception.InsufficientAvailabilityException;
import com.travolish.traveller.inventory.service.AvailabilityService;

/**
 * Integration tests for BookingService with Inventory Management System.
 * Tests end-to-end booking flow including availability checks and pricing.
 */
@SpringBootTest
@Transactional
@DisplayName("BookingService Integration Tests")
public class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AvailabilityService availabilityService;

    private static final Long TEST_HOTEL_ID = 1L;
    private static final Long TEST_ROOM_ID = 1L;
    private static final Double BASE_PRICE = 100.0;
    private static final LocalDate TEST_CHECK_IN = LocalDate.of(2025, 12, 1);
    private static final LocalDate TEST_CHECK_OUT = LocalDate.of(2025, 12, 5);

    @BeforeEach
    void setup() {
        // Initialize room availability for the test period
        availabilityService.initializeRoomAvailability(
            TEST_HOTEL_ID,
            TEST_ROOM_ID,
            5,  // 5 rooms
            30  // 30 days ahead
        );
    }

    @Test
    @DisplayName("Should calculate booking price correctly")
    void testCalculateBookingPrice() {
        // When
        BookingPriceDTO priceDTO = bookingService.calculateBookingPrice(
            TEST_ROOM_ID,
            BASE_PRICE,
            TEST_CHECK_IN,
            TEST_CHECK_OUT
        );

        // Then
        assertNotNull(priceDTO);
        assertEquals(BASE_PRICE, priceDTO.getBasePrice());
        assertEquals(4, priceDTO.getNumberOfNights()); // Dec 1-5 = 4 nights
        assertEquals(400.0, priceDTO.getBasePriceTotal()); // 100 * 4
        assertNotNull(priceDTO.getTotalPrice());
        assertTrue(priceDTO.getTotalPrice() > 0);
        
        // Print the summary
        System.out.println("Price Summary: " + priceDTO.getPriceSummary());
    }

    @Test
    @DisplayName("Should create booking with availability checks")
    void testCreateBookingWithAvailabilityCheck() {
        // Given
        Booking booking = new Booking();
        booking.setRoomId(TEST_ROOM_ID);
        booking.setHotelId(TEST_HOTEL_ID);
        booking.setGuestName("John Doe");
        booking.setGuestEmail("john@example.com");
        booking.setGuestPhone("1234567890");
        booking.setCheckInDate(TEST_CHECK_IN);
        booking.setCheckOutDate(TEST_CHECK_OUT);
        booking.setBasePrice(BASE_PRICE);
        booking.setStatus(Booking.BookingStatus.PENDING);

        // When
        Booking createdBooking = bookingService.create(booking);

        // Then
        assertNotNull(createdBooking.getId());
        assertEquals(Booking.BookingStatus.PENDING, createdBooking.getStatus());
        assertNotNull(createdBooking.getTotalPrice());
        assertTrue(createdBooking.getTotalPrice() > 0);
        assertEquals(BASE_PRICE, createdBooking.getBasePrice());
    }

    @Test
    @DisplayName("Should throw exception when room not available")
    void testCreateBookingWhenRoomNotAvailable() {
        // Given - Create a booking that takes up all availability
        Booking firstBooking = new Booking();
        firstBooking.setRoomId(TEST_ROOM_ID);
        firstBooking.setHotelId(TEST_HOTEL_ID);
        firstBooking.setGuestName("Guest 1");
        firstBooking.setCheckInDate(TEST_CHECK_IN);
        firstBooking.setCheckOutDate(TEST_CHECK_OUT);
        firstBooking.setBasePrice(BASE_PRICE);

        // Block rooms for the test period
        LocalDate maintenanceDate = TEST_CHECK_IN;
        availabilityService.blockRoomsForMaintenance(
            TEST_ROOM_ID,
            maintenanceDate,
            5,  // 5 rooms
            "Maintenance"
        );

        // When & Then
        Booking blockingBooking = new Booking();
        blockingBooking.setRoomId(TEST_ROOM_ID);
        blockingBooking.setHotelId(TEST_HOTEL_ID);
        blockingBooking.setGuestName("Guest 2");
        blockingBooking.setCheckInDate(TEST_CHECK_IN);
        blockingBooking.setCheckOutDate(TEST_CHECK_OUT);
        blockingBooking.setBasePrice(BASE_PRICE);

        assertThrows(InsufficientAvailabilityException.class,
            () -> bookingService.create(blockingBooking));
    }

    @Test
    @DisplayName("Should cancel booking and release availability")
    void testCancelBooking() {
        // Given - Create a booking
        Booking booking = new Booking();
        booking.setRoomId(TEST_ROOM_ID);
        booking.setHotelId(TEST_HOTEL_ID);
        booking.setGuestName("John Doe");
        booking.setCheckInDate(TEST_CHECK_IN);
        booking.setCheckOutDate(TEST_CHECK_OUT);
        booking.setBasePrice(BASE_PRICE);

        Booking createdBooking = bookingService.create(booking);
        assertNotNull(createdBooking.getId());

        // When - Cancel the booking
        bookingService.cancelBooking(createdBooking.getId());

        // Then - Verify booking is cancelled
        Booking cancelledBooking = bookingService.findById(createdBooking.getId()).orElse(null);
        assertNotNull(cancelledBooking);
        assertEquals(Booking.BookingStatus.CANCELLED, cancelledBooking.getStatus());

        // And verify room is available again
        Boolean isAvailable = bookingService.checkAvailability(
            TEST_ROOM_ID,
            TEST_CHECK_IN,
            TEST_CHECK_OUT
        );
        assertTrue(isAvailable);
    }

    @Test
    @DisplayName("Should find bookings by room ID")
    void testFindByRoomId() {
        // Given - Create multiple bookings for the same room
        Booking booking1 = createTestBooking("Guest 1", LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 3));
        Booking booking2 = createTestBooking("Guest 2", LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 12));

        bookingService.create(booking1);
        bookingService.create(booking2);

        // When
        var bookings = bookingService.findByRoomId(TEST_ROOM_ID);

        // Then
        assertNotNull(bookings);
        assertTrue(bookings.size() >= 2);
    }

    @Test
    @DisplayName("Should find bookings by hotel ID")
    void testFindByHotelId() {
        // Given - Create bookings
        Booking booking = createTestBooking("Guest 1", TEST_CHECK_IN, TEST_CHECK_OUT);
        bookingService.create(booking);

        // When
        var bookings = bookingService.findByHotelId(TEST_HOTEL_ID);

        // Then
        assertNotNull(bookings);
        assertTrue(bookings.size() > 0);
    }

    @Test
    @DisplayName("Should check availability correctly")
    void testCheckAvailability() {
        // Given
        LocalDate checkIn = LocalDate.of(2025, 12, 1);
        LocalDate checkOut = LocalDate.of(2025, 12, 5);

        // When
        Boolean isAvailable = bookingService.checkAvailability(TEST_ROOM_ID, checkIn, checkOut);

        // Then
        assertTrue(isAvailable);
    }

    @Test
    @DisplayName("Should prevent double booking")
    void testPreventDoubleBooking() {
        // Given - Create first booking
        Booking firstBooking = createTestBooking("Guest 1", TEST_CHECK_IN, TEST_CHECK_OUT);
        Booking createdFirstBooking = bookingService.create(firstBooking);
        assertNotNull(createdFirstBooking.getId());

        // Initialize availability with just 1 room
        availabilityService.initializeRoomAvailability(
            TEST_HOTEL_ID,
            TEST_ROOM_ID,
            1,  // Only 1 room
            30
        );

        // When - Try to book overlapping dates with only 1 room
        Booking secondBooking = createTestBooking("Guest 2", TEST_CHECK_IN, TEST_CHECK_OUT);

        // Then - Should throw exception (room fully booked)
        assertThrows(InsufficientAvailabilityException.class,
            () -> bookingService.create(secondBooking));
    }

    @Test
    @DisplayName("Should handle partial date range bookings")
    void testPartialDateRangeBooking() {
        // Given - Book part of the range
        LocalDate checkIn = LocalDate.of(2025, 12, 2);
        LocalDate checkOut = LocalDate.of(2025, 12, 4);
        
        Booking booking = createTestBooking("Guest 1", checkIn, checkOut);

        // When
        Booking createdBooking = bookingService.create(booking);

        // Then
        assertNotNull(createdBooking.getId());
        assertEquals(checkIn, createdBooking.getCheckInDate());
        assertEquals(checkOut, createdBooking.getCheckOutDate());
    }

    @Test
    @DisplayName("Should update booking status")
    void testUpdateBookingStatus() {
        // Given - Create a booking
        Booking booking = createTestBooking("John Doe", TEST_CHECK_IN, TEST_CHECK_OUT);
        Booking createdBooking = bookingService.create(booking);

        // When - Update status to CONFIRMED
        Booking updatedBooking = new Booking();
        updatedBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        updatedBooking.setRoomId(createdBooking.getRoomId());
        updatedBooking.setHotelId(createdBooking.getHotelId());
        updatedBooking.setGuestName(createdBooking.getGuestName());
        updatedBooking.setCheckInDate(createdBooking.getCheckInDate());
        updatedBooking.setCheckOutDate(createdBooking.getCheckOutDate());
        updatedBooking.setBasePrice(createdBooking.getBasePrice());
        updatedBooking.setTotalPrice(createdBooking.getTotalPrice());

        var result = bookingService.update(createdBooking.getId(), updatedBooking);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Booking.BookingStatus.CONFIRMED, result.get().getStatus());
    }

    @Test
    @DisplayName("Should calculate prices for different night counts")
    void testPriceCalculationForMultipleNights() {
        // Test different date ranges
        testPriceForDateRange(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 2),
            1,
            "1 night"
        );

        testPriceForDateRange(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 4),
            3,
            "3 nights"
        );

        testPriceForDateRange(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 8),
            7,
            "1 week"
        );
    }

    // Helper methods

    private Booking createTestBooking(String guestName, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setRoomId(TEST_ROOM_ID);
        booking.setHotelId(TEST_HOTEL_ID);
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestName.toLowerCase().replace(" ", ".") + "@example.com");
        booking.setGuestPhone("1234567890");
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setBasePrice(BASE_PRICE);
        booking.setStatus(Booking.BookingStatus.PENDING);
        return booking;
    }

    private void testPriceForDateRange(LocalDate checkIn, LocalDate checkOut, int expectedNights, String description) {
        BookingPriceDTO priceDTO = bookingService.calculateBookingPrice(
            TEST_ROOM_ID,
            BASE_PRICE,
            checkIn,
            checkOut
        );

        assertEquals(expectedNights, priceDTO.getNumberOfNights(), description);
        assertEquals(BASE_PRICE * expectedNights, priceDTO.getBasePriceTotal(), description);
        System.out.println(description + ": " + priceDTO.getPriceSummary());
    }
}
