package com.travolish.traveller.booking.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.booking.dto.BookingPriceDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.booking.service.BookingService;
import com.travolish.traveller.inventory.exception.InsufficientAvailabilityException;
import com.travolish.traveller.inventory.exception.OverbookingException;
import com.travolish.traveller.inventory.service.AvailabilityService;
import com.travolish.traveller.inventory.service.SeasonalPricingService;
import com.travolish.traveller.inventory.service.DynamicPricingService;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.service.NotificationService;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final SeasonalPricingService seasonalPricingService;
    private final DynamicPricingService dynamicPricingService;
    private final NotificationService notificationService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                           AvailabilityService availabilityService,
                           SeasonalPricingService seasonalPricingService,
                           DynamicPricingService dynamicPricingService,
                           NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.seasonalPricingService = seasonalPricingService;
        this.dynamicPricingService = dynamicPricingService;
        this.notificationService = notificationService;
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
    @Transactional
    public Booking create(Booking booking) {
        booking.setId(null);
        if (booking.getStatus() == null) {
            booking.setStatus(Booking.BookingStatus.PENDING);
        }

        // 1. Check availability for the date range
        Boolean isAvailable = availabilityService.isRoomAvailableForDateRange(
            booking.getRoomId(), 
            booking.getCheckInDate(), 
            booking.getCheckOutDate()
        );
        
        if (!isAvailable) {
            throw new InsufficientAvailabilityException(
                "Room " + booking.getRoomId() + " is not available for the requested dates"
            );
        }
        
        // 2. Check for conflicts/overbooking
        Boolean hasConflict = availabilityService.hasBookingConflict(
            booking.getRoomId(),
            booking.getCheckInDate(),
            booking.getCheckOutDate()
        );
        
        if (hasConflict) {
            throw new OverbookingException(
                "Booking conflict detected for room " + booking.getRoomId()
            );
        }
        
        // 3. Calculate price components
        Double basePrice = booking.getBasePrice();
        long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (numberOfNights <= 0) numberOfNights = 1;
        // Ensure the pricing range covers at least one night even for same-day bookings,
        // because calculatePriceForDateRange iterates [checkIn, checkOut) exclusively.
        LocalDate effectiveCheckOut = booking.getCheckInDate().plusDays(numberOfNights);

        // Seasonal adjustment: price every night in the stay individually so
        // stays that span a season boundary are billed correctly.
        Double seasonalTotal = seasonalPricingService.calculatePriceForDateRange(
            booking.getRoomId(), booking.getCheckInDate(), effectiveCheckOut, basePrice);
        Double seasonalAdjustment = seasonalTotal - (basePrice * numberOfNights);
        booking.setSeasonalAdjustment(seasonalAdjustment);

        // Dynamic pricing returns a multiplier (1.0 = no change)
        Double dynamicMultiplier = dynamicPricingService.calculateDynamicPrice(
            booking.getRoomId(), booking.getCheckInDate());
        Double dynamicAdjustment = basePrice * (dynamicMultiplier - 1.0) * numberOfNights;
        booking.setDynamicPricingAdjustment(dynamicAdjustment);

        Double calculatedTotalPrice = (basePrice * numberOfNights) + seasonalAdjustment + dynamicAdjustment;
        booking.setTotalPrice(calculatedTotalPrice);
        
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setUpdatedAt(OffsetDateTime.now());
        
        // 4. Save booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // 5. Update availability for all dates in range
        availabilityService.bookRoom(
            booking.getRoomId(),
            booking.getCheckInDate(),
            booking.getCheckOutDate()
        );

        try {
            notifyBookingConfirmation(savedBooking);
        } catch (Exception e) {
            log.warn("Booking confirmation email failed (booking saved): {}", e.getMessage());
        }

        return savedBooking;
    }

    @Override
    @Transactional
    public Optional<Booking> update(Long id, Booking booking) {
        return bookingRepository.findById(id).map(existing -> {
            Booking.BookingStatus previousStatus = existing.getStatus();
            Booking.BookingStatus newStatus = booking.getStatus();

            if (newStatus == Booking.BookingStatus.CANCELLED &&
                    previousStatus != Booking.BookingStatus.CANCELLED) {
                availabilityService.cancelBooking(
                    existing.getRoomId(),
                    existing.getCheckInDate(),
                    existing.getCheckOutDate()
                );
            }

            existing.setRoomId(booking.getRoomId());
            existing.setHotelId(booking.getHotelId());
            existing.setGuestName(booking.getGuestName());
            existing.setGuestEmail(booking.getGuestEmail());
            existing.setGuestPhone(booking.getGuestPhone());
            existing.setCheckInDate(booking.getCheckInDate());
            existing.setCheckOutDate(booking.getCheckOutDate());
            existing.setBasePrice(booking.getBasePrice());
            existing.setSeasonalAdjustment(booking.getSeasonalAdjustment());
            existing.setDynamicPricingAdjustment(booking.getDynamicPricingAdjustment());
            existing.setPromotionalDiscount(booking.getPromotionalDiscount());
            existing.setTotalPrice(booking.getTotalPrice());
            existing.setStatus(newStatus);
            existing.setNotes(booking.getNotes());
            existing.setUpdatedAt(OffsetDateTime.now());
            Booking saved = bookingRepository.save(existing);

            try {
                if (newStatus == Booking.BookingStatus.CANCELLED &&
                        previousStatus != Booking.BookingStatus.CANCELLED) {
                    notifyBookingCancellation(saved);
                } else if (newStatus == Booking.BookingStatus.CONFIRMED &&
                        previousStatus != Booking.BookingStatus.CONFIRMED) {
                    notifyBookingConfirmation(saved);
                }
            } catch (Exception e) {
                log.warn("Booking status-change email failed (booking saved): {}", e.getMessage());
            }

            return saved;
        });
    }

    @Override
    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }

    @Override
    public Boolean checkAvailability(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return availabilityService.isRoomAvailableForDateRange(roomId, checkInDate, checkOutDate);
    }

    @Override
    public BookingPriceDTO calculateBookingPrice(Long roomId, Double basePrice, LocalDate checkInDate, LocalDate checkOutDate) {
        // Calculate number of nights
        long numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (numberOfNights <= 0) {
            numberOfNights = 1;
        }
        // Ensure the pricing range covers at least one night (see create() for rationale).
        LocalDate effectiveCheckOut = checkInDate.plusDays(numberOfNights);

        Double basePriceTotal = basePrice * numberOfNights;

        // Calculate seasonal adjustment: price each night individually so stays
        // spanning a season boundary are calculated correctly.
        Double seasonalTotal = seasonalPricingService.calculatePriceForDateRange(
            roomId, checkInDate, effectiveCheckOut, basePrice);
        Double seasonalAdjustment = seasonalTotal - basePriceTotal;
        
        // Calculate dynamic pricing adjustment (returns a multiplier: 1.0 = no change)
        Double dynamicMultiplier = dynamicPricingService.calculateDynamicPrice(
            roomId,
            checkInDate
        );
        Double dynamicAdjustment = basePrice * (dynamicMultiplier - 1.0) * numberOfNights;
        
        // Calculate promotional discount (default 0)
        Double promotionalDiscount = 0.0;
        
        // Calculate total price
        Double totalPrice = basePriceTotal + seasonalAdjustment + dynamicAdjustment - promotionalDiscount;
        
        return BookingPriceDTO.builder()
            .basePrice(basePrice)
            .numberOfNights((int) numberOfNights)
            .basePriceTotal(basePriceTotal)
            .seasonalAdjustment(seasonalAdjustment)
            .dynamicPricingAdjustment(dynamicAdjustment)
            .promotionalDiscount(promotionalDiscount)
            .totalPrice(totalPrice)
            .build();
    }

    @Override
    public List<Booking> findByRoomId(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    @Override
    public List<Booking> findByHotelId(Long hotelId) {
        return bookingRepository.findByHotelId(hotelId);
    }

    @Override
    public List<Booking> findConfirmedBookingsInDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return bookingRepository.findConfirmedBookingsInDateRange(roomId, checkInDate, checkOutDate);
    }

    @Override
    public List<Booking> findByGuestEmail(String guestEmail) {
        return bookingRepository.findByGuestEmailIgnoreCase(guestEmail);
    }

    private void notifyBookingConfirmation(Booking booking) {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setType(NotificationType.BOOKING_CONFIRMATION);
        req.setChannel(NotificationChannel.EMAIL);
        req.setRecipientEmail(booking.getGuestEmail());
        req.setBookingId(booking.getId());
        req.setHotelId(booking.getHotelId());
        req.setSendImmediately(true);
        req.setSubject("Booking Confirmed – Check-in " + booking.getCheckInDate());
        req.setMessage("Hi " + booking.getGuestName() + ",\n\n"
                + "Your booking has been confirmed.\n\n"
                + "Check-in:  " + booking.getCheckInDate() + "\n"
                + "Check-out: " + booking.getCheckOutDate() + "\n"
                + "Total:     $" + String.format("%.2f", booking.getTotalPrice()) + "\n\n"
                + "We look forward to welcoming you!");
        notificationService.sendNotificationAsync(req);
    }

    private void notifyBookingCancellation(Booking booking) {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setType(NotificationType.BOOKING_CANCELLATION);
        req.setChannel(NotificationChannel.EMAIL);
        req.setRecipientEmail(booking.getGuestEmail());
        req.setBookingId(booking.getId());
        req.setHotelId(booking.getHotelId());
        req.setSendImmediately(true);
        req.setSubject("Booking Cancellation Confirmed");
        req.setMessage("Hi " + booking.getGuestName() + ",\n\n"
                + "Your booking has been cancelled.\n\n"
                + "Original check-in:  " + booking.getCheckInDate() + "\n"
                + "Original check-out: " + booking.getCheckOutDate() + "\n\n"
                + "If you did not request this cancellation, please contact support.");
        notificationService.sendNotificationAsync(req);
    }

    @Override
    @Transactional
    public boolean cancelBooking(Long bookingId) {
        Optional<Booking> found = bookingRepository.findById(bookingId);
        if (found.isEmpty()) {
            return false;
        }
        Booking booking = found.get();
        if (booking.getStatus() != Booking.BookingStatus.CANCELLED) {
            availabilityService.cancelBooking(
                booking.getRoomId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
            );
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking.setUpdatedAt(OffsetDateTime.now());
            bookingRepository.save(booking);
            try {
                notifyBookingCancellation(booking);
            } catch (Exception e) {
                log.warn("Booking cancellation email failed (booking saved): {}", e.getMessage());
            }
        }
        return true;
    }
}
