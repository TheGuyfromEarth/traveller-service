package com.travolish.traveller.booking.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final SeasonalPricingService seasonalPricingService;
    private final DynamicPricingService dynamicPricingService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                           AvailabilityService availabilityService,
                           SeasonalPricingService seasonalPricingService,
                           DynamicPricingService dynamicPricingService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.seasonalPricingService = seasonalPricingService;
        this.dynamicPricingService = dynamicPricingService;
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

        // Seasonal adjustment for check-in night (price delta vs base)
        Double seasonalNightPrice = seasonalPricingService.calculateFinalPrice(
            booking.getRoomId(), booking.getCheckInDate(), basePrice);
        Double seasonalAdjustment = (seasonalNightPrice - basePrice) * numberOfNights;
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
        
        return savedBooking;
    }

    @Override
    @Transactional
    public Optional<Booking> update(Long id, Booking booking) {
        return bookingRepository.findById(id).map(existing -> {
            // Check if status is changing to CANCELLED
            if (booking.getStatus() == Booking.BookingStatus.CANCELLED && 
                existing.getStatus() != Booking.BookingStatus.CANCELLED) {
                // Release rooms from availability
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
        
        Double basePriceTotal = basePrice * numberOfNights;
        
        // Calculate seasonal adjustment
        Double seasonalPrice = seasonalPricingService.calculateFinalPrice(
            roomId,
            checkInDate,
            basePrice
        );
        Double seasonalAdjustment = (seasonalPrice - basePrice) * numberOfNights;
        
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

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            if (booking.getStatus() != Booking.BookingStatus.CANCELLED) {
                // Release rooms from availability
                availabilityService.cancelBooking(
                    booking.getRoomId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
                );
                
                // Update booking status
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                booking.setUpdatedAt(OffsetDateTime.now());
                bookingRepository.save(booking);
            }
        });
    }
}
