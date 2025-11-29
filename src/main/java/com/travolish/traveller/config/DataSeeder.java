package com.travolish.traveller.config;

import java.util.List;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public DataSeeder(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (hotelRepository.count() == 0) {
            var h1 = new Hotel(null, "Grand Plaza", "123 Main St", "Metropolis", 4.5, "+1-555-0100", "info@grandplaza.com", "A luxury downtown hotel", null);
            var h2 = new Hotel(null, "Seaside Resort", "45 Ocean Ave", "Bay City", 4.2, "+1-555-0200", "hello@seasideresort.com", "Relaxing ocean views and pool", null);
            hotelRepository.saveAll(List.of(h1, h2));

            // Seed rooms
            /*var r1 = new Room(1, "101", "SINGLE", 79.0, true, h1);
            var r2 = new Room(null, "102", "DOUBLE", 119.0, true, h1);
            var r3 = new Room(null, "201", "SUITE", 299.0, true, h2);
            roomRepository.saveAll(List.of(r1, r2, r3));*/

            System.out.println("Seeded hotels and rooms");
        }
    }
}
