package com.travolish.traveller.hotel.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    public DataSeeder(HotelRepository hotelRepository, BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (hotelRepository.count() == 0) {
            var h1 = new Hotel(null, "Grand Plaza", "123 Main St", "Metropolis", "USA", 4.5, "+1-555-0100", "info@grandplaza.com", "A luxury downtown hotel", null);
            var h2 = new Hotel(null, "Seaside Resort", "45 Ocean Ave", "Bay City", "USA", 4.2, "+1-555-0200", "hello@seasideresort.com", "Relaxing ocean views and pool", null);
            var h3 = new Hotel(null, "Eiffel Tower Hotel", "10 Rue de Rivoli", "Paris", "France", 4.7, "+33-1-5555-0100", "info@eiffeltower.com", "Iconic Paris hotel near Eiffel Tower", null);
            var h4 = new Hotel(null, "Big Ben Hotel", "100 Westminster", "London", "UK", 4.4, "+44-20-5555-0100", "info@bigbenhotel.com", "Historic London accommodation", null);
            var saved = hotelRepository.saveAll(List.of(h1, h2, h3, h4));

            Long h1Id = saved.get(0).getId();
            Long h2Id = saved.get(1).getId();

            // Seed sample bookings
            var b1 = new Booking(null, 1L, h1Id, "John Doe", "john@example.com", "+1-555-1111", 
                LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 5), 400.0, 
                Booking.BookingStatus.CONFIRMED, "VIP guest", null, null);
            var b2 = new Booking(null, 2L, h1Id, "Jane Smith", "jane@example.com", "+1-555-2222", 
                LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 12), 240.0, 
                Booking.BookingStatus.PENDING, "Early check-in requested", null, null);
            var b3 = new Booking(null, 3L, h2Id, "Alice Johnson", "alice@example.com", "+1-555-3333", 
                LocalDate.of(2025, 12, 15), LocalDate.of(2025, 12, 18), 900.0, 
                Booking.BookingStatus.CONFIRMED, "Honeymoon package", null, null);
            bookingRepository.saveAll(List.of(b1, b2, b3));

            System.out.println("Seeded hotels and bookings");
        }
    }
}
