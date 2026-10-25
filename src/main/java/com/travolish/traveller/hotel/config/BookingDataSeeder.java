package com.travolish.traveller.hotel.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;

@Component("bookingDataSeeder")
public class BookingDataSeeder implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    public BookingDataSeeder(HotelRepository hotelRepository, BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (hotelRepository.count() == 0) {
            // Create hotels using setters
            Hotel h1 = new Hotel();
            h1.setName("Grand Plaza");
            h1.setAddress("123 Main St");
            h1.setCity("Metropolis");
            h1.setCountry("USA");
            h1.setRating(4.5);
            h1.setPhone("+1-555-0100");
            h1.setEmail("info@grandplaza.com");
            h1.setState("NY");
            h1.setDescription("A luxury downtown hotel");

            Hotel h2 = new Hotel();
            h2.setName("Seaside Resort");
            h2.setAddress("45 Ocean Ave");
            h2.setCity("Bay City");
            h2.setCountry("USA");
            h2.setRating(4.2);
            h2.setPhone("+1-555-0200");
            h2.setEmail("hello@seasideresort.com");
            h2.setState("CA");
            h2.setDescription("Relaxing ocean views and pool");

            Hotel h3 = new Hotel();
            h3.setName("Eiffel Tower Hotel");
            h3.setAddress("10 Rue de Rivoli");
            h3.setCity("Paris");
            h3.setCountry("France");
            h3.setRating(4.7);
            h3.setPhone("+33-1-5555-0100");
            h3.setEmail("info@eiffeltower.com");
            h3.setState("Île-de-France");
            h3.setDescription("Iconic Paris hotel near Eiffel Tower");

            Hotel h4 = new Hotel();
            h4.setName("Big Ben Hotel");
            h4.setAddress("100 Westminster");
            h4.setCity("London");
            h4.setCountry("UK");
            h4.setRating(4.4);
            h4.setPhone("+44-20-5555-0100");
            h4.setEmail("info@bigbenhotel.com");
            h4.setState("England");
            h4.setDescription("Historic London accommodation");
            
            var saved = hotelRepository.saveAll(List.of(h1, h2, h3, h4));

            Long h1Id = saved.get(0).getId();
            Long h2Id = saved.get(1).getId();

            // Seed sample bookings using setters
            Booking b1 = new Booking();
            b1.setRoomId(1L);
            b1.setHotelId(h1Id);
            b1.setGuestName("John Doe");
            b1.setGuestEmail("john@example.com");
            b1.setGuestPhone("+1-555-1111");
            b1.setCheckInDate(LocalDate.of(2025, 12, 1));
            b1.setCheckOutDate(LocalDate.of(2025, 12, 5));
            b1.setBasePrice(100.0);
            b1.setSeasonalAdjustment(0.0);
            b1.setDynamicPricingAdjustment(0.0);
            b1.setPromotionalDiscount(0.0);
            b1.setTotalPrice(400.0);
            b1.setStatus(Booking.BookingStatus.CONFIRMED);
            b1.setNotes("VIP guest");
            
            Booking b2 = new Booking();
            b2.setRoomId(2L);
            b2.setHotelId(h1Id);
            b2.setGuestName("Jane Smith");
            b2.setGuestEmail("jane@example.com");
            b2.setGuestPhone("+1-555-2222");
            b2.setCheckInDate(LocalDate.of(2025, 12, 10));
            b2.setCheckOutDate(LocalDate.of(2025, 12, 12));
            b2.setBasePrice(120.0);
            b2.setSeasonalAdjustment(0.0);
            b2.setDynamicPricingAdjustment(0.0);
            b2.setPromotionalDiscount(0.0);
            b2.setTotalPrice(240.0);
            b2.setStatus(Booking.BookingStatus.PENDING);
            b2.setNotes("Early check-in requested");
            
            Booking b3 = new Booking();
            b3.setRoomId(3L);
            b3.setHotelId(h2Id);
            b3.setGuestName("Alice Johnson");
            b3.setGuestEmail("alice@example.com");
            b3.setGuestPhone("+1-555-3333");
            b3.setCheckInDate(LocalDate.of(2025, 12, 15));
            b3.setCheckOutDate(LocalDate.of(2025, 12, 18));
            b3.setBasePrice(300.0);
            b3.setSeasonalAdjustment(0.0);
            b3.setDynamicPricingAdjustment(0.0);
            b3.setPromotionalDiscount(0.0);
            b3.setTotalPrice(900.0);
            b3.setStatus(Booking.BookingStatus.CONFIRMED);
            b3.setNotes("Honeymoon package");
            
            bookingRepository.saveAll(List.of(b1, b2, b3));

            System.out.println("Seeded hotels and bookings");
        }
    }
}
