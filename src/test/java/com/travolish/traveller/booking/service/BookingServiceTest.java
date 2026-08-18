package com.travolish.traveller.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.travolish.traveller.booking.dto.BookingPriceDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.booking.service.impl.BookingServiceImpl;
import com.travolish.traveller.inventory.exception.InsufficientAvailabilityException;
import com.travolish.traveller.inventory.exception.OverbookingException;
import com.travolish.traveller.inventory.service.AvailabilityService;
import com.travolish.traveller.inventory.service.DynamicPricingService;
import com.travolish.traveller.inventory.service.SeasonalPricingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests — Hotel Booking Flow")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private SeasonalPricingService seasonalPricingService;
    @Mock private DynamicPricingService dynamicPricingService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private static final Long ROOM_ID   = 10L;
    private static final Long HOTEL_ID  = 1L;
    private static final Double BASE    = 100.0;
    private static final LocalDate CHECK_IN  = LocalDate.now().plusDays(5);
    private static final LocalDate CHECK_OUT = LocalDate.now().plusDays(9); // 4 nights

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Booking buildBooking(String guest, LocalDate in, LocalDate out) {
        Booking b = new Booking();
        b.setRoomId(ROOM_ID);
        b.setHotelId(HOTEL_ID);
        b.setGuestName(guest);
        b.setGuestEmail(guest.toLowerCase().replace(" ", ".") + "@test.com");
        b.setGuestPhone("9999999999");
        b.setCheckInDate(in);
        b.setCheckOutDate(out);
        b.setBasePrice(BASE);
        b.setStatus(Booking.BookingStatus.PENDING);
        return b;
    }

    private Booking savedVersion(Booking b, Long id) {
        Booking saved = new Booking();
        saved.setId(id);
        saved.setRoomId(b.getRoomId());
        saved.setHotelId(b.getHotelId());
        saved.setGuestName(b.getGuestName());
        saved.setGuestEmail(b.getGuestEmail());
        saved.setGuestPhone(b.getGuestPhone());
        saved.setCheckInDate(b.getCheckInDate());
        saved.setCheckOutDate(b.getCheckOutDate());
        saved.setBasePrice(b.getBasePrice());
        saved.setTotalPrice(b.getTotalPrice() != null ? b.getTotalPrice() : BASE * 4);
        saved.setStatus(b.getStatus() != null ? b.getStatus() : Booking.BookingStatus.PENDING);
        saved.setSeasonalAdjustment(b.getSeasonalAdjustment() != null ? b.getSeasonalAdjustment() : 0.0);
        saved.setDynamicPricingAdjustment(b.getDynamicPricingAdjustment() != null ? b.getDynamicPricingAdjustment() : 0.0);
        return saved;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // create()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create() — booking creation")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CreateBooking {

        @BeforeEach
        void stubPricing() {
            // No seasonal adjustment by default: return base × nights so adjustment = 0.
            // Uses the effective checkout (checkIn + max(1, nights)) passed by the service.
            when(seasonalPricingService.calculatePriceForDateRange(anyLong(), any(), any(), anyDouble()))
                .thenAnswer(inv -> {
                    LocalDate in   = inv.getArgument(1);
                    LocalDate out  = inv.getArgument(2);
                    Double    base = inv.getArgument(3);
                    long nights = ChronoUnit.DAYS.between(in, out);
                    return base * Math.max(1, nights);
                });
            when(dynamicPricingService.calculateDynamicPrice(anyLong(), any()))
                .thenReturn(1.0);
        }

        @Test
        @DisplayName("TC-BK-01 Happy path: booking created with correct total price")
        void happyPath() {
            Booking booking = buildBooking("Alice Smith", CHECK_IN, CHECK_OUT);
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> savedVersion(inv.getArgument(0), 42L));

            Booking result = bookingService.create(booking);

            assertNotNull(result.getId(), "Saved booking must have a generated ID");
            assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
            assertEquals(400.0, result.getTotalPrice(), "4 nights × $100 = $400 (no adjustments)");
            verify(availabilityService).bookRoom(ROOM_ID, HOTEL_ID, CHECK_IN, CHECK_OUT);
        }

        @Test
        @DisplayName("TC-BK-02 Default status: null status becomes PENDING")
        void defaultStatusSetToPending() {
            Booking booking = buildBooking("Bob Jones", CHECK_IN, CHECK_OUT);
            booking.setStatus(null); // explicitly null
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);
            when(bookingRepository.save(any())).thenAnswer(inv -> savedVersion(inv.getArgument(0), 1L));

            Booking result = bookingService.create(booking);

            assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        }

        @Test
        @DisplayName("TC-BK-03 Seasonal premium: price raised by seasonal adjustment")
        void seasonalPremiumApplied() {
            Booking booking = buildBooking("Carol White", CHECK_IN, CHECK_OUT);
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);
            // 40% summer premium: $140/night × 4 nights = $560 total seasonal price
            when(seasonalPricingService.calculatePriceForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT, BASE)).thenReturn(560.0);
            when(bookingRepository.save(any())).thenAnswer(inv -> savedVersion(inv.getArgument(0), 2L));

            Booking result = bookingService.create(booking);

            // seasonalAdjustment = 560 - (100 × 4) = 160;  total = 400 + 160 = 560
            assertEquals(160.0, result.getSeasonalAdjustment(), 0.001);
            assertEquals(560.0, result.getTotalPrice(), 0.001);
        }

        @Test
        @DisplayName("TC-BK-04 Dynamic pricing multiplier: price raised by demand multiplier")
        void dynamicPricingApplied() {
            Booking booking = buildBooking("Dave Green", CHECK_IN, CHECK_OUT);
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);
            // Dynamic returns 1.3 (30% demand premium)
            when(dynamicPricingService.calculateDynamicPrice(ROOM_ID, CHECK_IN)).thenReturn(1.3);
            when(bookingRepository.save(any())).thenAnswer(inv -> savedVersion(inv.getArgument(0), 3L));

            Booking result = bookingService.create(booking);

            // dynamicAdjustment = 100 * (1.3 - 1.0) * 4 = 120;  total = 400 + 0 + 120 = 520
            assertEquals(120.0, result.getDynamicPricingAdjustment(), 0.001);
            assertEquals(520.0, result.getTotalPrice(), 0.001);
        }

        @Test
        @DisplayName("TC-BK-05 Room unavailable: throws InsufficientAvailabilityException")
        void roomUnavailableThrows() {
            Booking booking = buildBooking("Eve Black", CHECK_IN, CHECK_OUT);
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);

            assertThrows(InsufficientAvailabilityException.class, () -> bookingService.create(booking),
                "Must throw when room has no available inventory");
            verifyNoInteractions(bookingRepository);
        }

        @Test
        @DisplayName("TC-BK-06 Overbooking conflict: throws OverbookingException")
        void conflictThrows() {
            Booking booking = buildBooking("Frank Blue", CHECK_IN, CHECK_OUT);
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);

            assertThrows(OverbookingException.class, () -> bookingService.create(booking),
                "Must throw when a conflicting booking already exists");
            verifyNoInteractions(bookingRepository);
        }

        @Test
        @DisplayName("TC-BK-07 Single-night stay: same-day check-in/out treated as 1 night")
        void singleNightEdgeCase() {
            LocalDate sameDay = LocalDate.now().plusDays(10);
            Booking booking = buildBooking("Grace Gold", sameDay, sameDay); // 0 computed days → clamped to 1
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, sameDay, sameDay)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, sameDay, sameDay)).thenReturn(false);
            when(bookingRepository.save(any())).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                b.setId(5L);
                return b;
            });

            Booking result = bookingService.create(booking);

            // 0 nights clamped to 1 → total = $100
            assertEquals(100.0, result.getTotalPrice(), 0.001,
                "Zero-night stay (same-day) must be treated as 1 night");
        }

        @Test
        @DisplayName("TC-BK-08 ID reset: provided ID is nullified before save (no upsert)")
        void providedIdIsNullifiedBeforeSave() {
            Booking booking = buildBooking("Hank Silver", CHECK_IN, CHECK_OUT);
            booking.setId(999L); // caller attempts to set ID
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(availabilityService.hasBookingConflict(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            when(bookingRepository.save(captor.capture())).thenAnswer(inv -> savedVersion(inv.getArgument(0), 7L));

            bookingService.create(booking);

            assertNull(captor.getValue().getId(), "ID must be nullified before persisting to prevent unintended updates");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // cancelBooking()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking() — cancellation flow")
    class CancelBooking {

        @Test
        @DisplayName("TC-BK-09 Happy path: status set to CANCELLED and availability released; returns true")
        void cancelsAndReleasesAvailability() {
            Booking existing = savedVersion(buildBooking("Ian Purple", CHECK_IN, CHECK_OUT), 20L);
            existing.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findById(20L)).thenReturn(Optional.of(existing));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = bookingService.cancelBooking(20L);

            assertTrue(result, "cancelBooking() must return true when the booking exists");
            assertEquals(Booking.BookingStatus.CANCELLED, existing.getStatus());
            verify(availabilityService).cancelBooking(ROOM_ID, CHECK_IN, CHECK_OUT);
        }

        @Test
        @DisplayName("TC-BK-10 Already cancelled: availability NOT released a second time; returns true")
        void alreadyCancelledIsIdempotent() {
            Booking existing = savedVersion(buildBooking("Jane Red", CHECK_IN, CHECK_OUT), 21L);
            existing.setStatus(Booking.BookingStatus.CANCELLED);
            when(bookingRepository.findById(21L)).thenReturn(Optional.of(existing));

            boolean result = bookingService.cancelBooking(21L);

            assertTrue(result);
            verify(availabilityService, never()).cancelBooking(any(), any(), any());
        }

        @Test
        @DisplayName("TC-BK-11 Non-existent booking: returns false, availability untouched")
        void nonExistentBookingReturnsFalse() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            boolean result = bookingService.cancelBooking(999L);

            assertFalse(result, "cancelBooking() must return false when the booking does not exist");
            verifyNoInteractions(availabilityService);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update() — booking updates")
    class UpdateBooking {

        @Test
        @DisplayName("TC-BK-12 Status change to CANCELLED: releases availability")
        void updateToCancelledReleasesRooms() {
            Booking existing = savedVersion(buildBooking("Karl Yellow", CHECK_IN, CHECK_OUT), 30L);
            existing.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findById(30L)).thenReturn(Optional.of(existing));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking patch = buildBooking("Karl Yellow", CHECK_IN, CHECK_OUT);
            patch.setStatus(Booking.BookingStatus.CANCELLED);

            bookingService.update(30L, patch);

            verify(availabilityService).cancelBooking(ROOM_ID, CHECK_IN, CHECK_OUT);
        }

        @Test
        @DisplayName("TC-BK-13 Status change to CONFIRMED: does NOT release availability")
        void updateToConfirmedDoesNotReleaseRooms() {
            Booking existing = savedVersion(buildBooking("Lena Cyan", CHECK_IN, CHECK_OUT), 31L);
            existing.setStatus(Booking.BookingStatus.PENDING);
            when(bookingRepository.findById(31L)).thenReturn(Optional.of(existing));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking patch = buildBooking("Lena Cyan", CHECK_IN, CHECK_OUT);
            patch.setStatus(Booking.BookingStatus.CONFIRMED);

            bookingService.update(31L, patch);

            verify(availabilityService, never()).cancelBooking(any(), any(), any());
        }

        @Test
        @DisplayName("TC-BK-14 Update on non-existent booking returns empty Optional")
        void updateNonExistentReturnsEmpty() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());
            Booking patch = buildBooking("Ghost", CHECK_IN, CHECK_OUT);

            Optional<Booking> result = bookingService.update(999L, patch);

            assertTrue(result.isEmpty());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // calculateBookingPrice()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateBookingPrice() — price calculation")
    class CalculatePrice {

        @Test
        @DisplayName("TC-BK-15 No adjustments: total equals base × nights")
        void noAdjustments() {
            // No seasonal adjustment: range total equals base × 4 nights
            when(seasonalPricingService.calculatePriceForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT, BASE)).thenReturn(400.0);
            when(dynamicPricingService.calculateDynamicPrice(ROOM_ID, CHECK_IN)).thenReturn(1.0);

            BookingPriceDTO dto = bookingService.calculateBookingPrice(ROOM_ID, BASE, CHECK_IN, CHECK_OUT);

            assertEquals(BASE,   dto.getBasePrice());
            assertEquals(4,      dto.getNumberOfNights());
            assertEquals(400.0,  dto.getBasePriceTotal());
            assertEquals(0.0,    dto.getSeasonalAdjustment(), 0.001);
            assertEquals(0.0,    dto.getDynamicPricingAdjustment(), 0.001);
            assertEquals(400.0,  dto.getTotalPrice(), 0.001);
        }

        @Test
        @DisplayName("TC-BK-16 Seasonal discount: total reduced for off-season")
        void seasonalDiscount() {
            // $80/night × 4 nights = $320 total
            when(seasonalPricingService.calculatePriceForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT, BASE)).thenReturn(320.0);
            when(dynamicPricingService.calculateDynamicPrice(ROOM_ID, CHECK_IN)).thenReturn(1.0);

            BookingPriceDTO dto = bookingService.calculateBookingPrice(ROOM_ID, BASE, CHECK_IN, CHECK_OUT);

            // seasonalAdj = 320 - 400 = -80 → total = 320
            assertEquals(-80.0, dto.getSeasonalAdjustment(), 0.001);
            assertEquals(320.0, dto.getTotalPrice(), 0.001);
        }

        @Test
        @DisplayName("TC-BK-17 Combined adjustments: seasonal + dynamic stack correctly")
        void combinedAdjustments() {
            // $140/night × 4 nights = $560 total
            when(seasonalPricingService.calculatePriceForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT, BASE)).thenReturn(560.0);
            when(dynamicPricingService.calculateDynamicPrice(ROOM_ID, CHECK_IN)).thenReturn(1.3); // +30%

            BookingPriceDTO dto = bookingService.calculateBookingPrice(ROOM_ID, BASE, CHECK_IN, CHECK_OUT);

            // seasonalAdj = 560 - 400 = 160; dynamicAdj = 100 × 0.3 × 4 = 120
            assertEquals(160.0, dto.getSeasonalAdjustment(), 0.001);
            assertEquals(120.0, dto.getDynamicPricingAdjustment(), 0.001);
            assertEquals(680.0, dto.getTotalPrice(), 0.001, "400 + 160 + 120 = 680");
        }

        @Test
        @DisplayName("TC-BK-18 One-night stay: nights counted correctly")
        void oneNight() {
            LocalDate in  = LocalDate.now().plusDays(10);
            LocalDate out = in.plusDays(1);
            // No seasonal adjustment for one night
            when(seasonalPricingService.calculatePriceForDateRange(ROOM_ID, in, out, BASE)).thenReturn(100.0);
            when(dynamicPricingService.calculateDynamicPrice(ROOM_ID, in)).thenReturn(1.0);

            BookingPriceDTO dto = bookingService.calculateBookingPrice(ROOM_ID, BASE, in, out);

            assertEquals(1,     dto.getNumberOfNights());
            assertEquals(100.0, dto.getTotalPrice(), 0.001);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // checkAvailability()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAvailability() — availability delegation")
    class CheckAvailability {

        @Test
        @DisplayName("TC-BK-19 Available: delegates to AvailabilityService and returns true")
        void available() {
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(true);

            assertTrue(bookingService.checkAvailability(ROOM_ID, CHECK_IN, CHECK_OUT));
        }

        @Test
        @DisplayName("TC-BK-20 Not available: returns false")
        void notAvailable() {
            when(availabilityService.isRoomAvailableForDateRange(ROOM_ID, CHECK_IN, CHECK_OUT)).thenReturn(false);

            assertFalse(bookingService.checkAvailability(ROOM_ID, CHECK_IN, CHECK_OUT));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Query methods
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Query methods — find by room / hotel / email")
    class QueryMethods {

        @Test
        @DisplayName("TC-BK-21 findByRoomId: delegates to repository")
        void findByRoomId() {
            Booking b = savedVersion(buildBooking("Mike", CHECK_IN, CHECK_OUT), 50L);
            when(bookingRepository.findByRoomId(ROOM_ID)).thenReturn(List.of(b));

            List<Booking> result = bookingService.findByRoomId(ROOM_ID);

            assertEquals(1, result.size());
            assertEquals(50L, result.get(0).getId());
        }

        @Test
        @DisplayName("TC-BK-22 findByHotelId: delegates to repository")
        void findByHotelId() {
            Booking b = savedVersion(buildBooking("Nina", CHECK_IN, CHECK_OUT), 51L);
            when(bookingRepository.findByHotelId(HOTEL_ID)).thenReturn(List.of(b));

            List<Booking> result = bookingService.findByHotelId(HOTEL_ID);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("TC-BK-23 findByGuestEmail: case-insensitive lookup")
        void findByGuestEmail() {
            Booking b = savedVersion(buildBooking("Oscar", CHECK_IN, CHECK_OUT), 52L);
            when(bookingRepository.findByGuestEmailIgnoreCase("OSCAR@TEST.COM")).thenReturn(List.of(b));

            List<Booking> result = bookingService.findByGuestEmail("OSCAR@TEST.COM");

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("TC-BK-24 findConfirmedBookingsInDateRange: delegates to repository")
        void findConfirmedInRange() {
            LocalDate s = LocalDate.now().plusDays(1);
            LocalDate e = LocalDate.now().plusDays(30);
            Booking b = savedVersion(buildBooking("Paula", CHECK_IN, CHECK_OUT), 53L);
            b.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findConfirmedBookingsInDateRange(ROOM_ID, s, e)).thenReturn(List.of(b));

            List<Booking> result = bookingService.findConfirmedBookingsInDateRange(ROOM_ID, s, e);

            assertEquals(1, result.size());
            assertEquals(Booking.BookingStatus.CONFIRMED, result.get(0).getStatus());
        }
    }
}
