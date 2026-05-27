package com.travolish.traveller.config;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.HotelChangeRequest;
import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.repository.HotelChangeRequestRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.repository.RoomRepository;
import com.travolish.traveller.inventory.model.PricingRule;
import com.travolish.traveller.inventory.repository.PricingRuleRepository;
import com.travolish.traveller.kyc.entity.HostKYC;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds realistic admin test data. Only runs when app.init-test-data=true.
 * Safe to re-run — skips if marker user already exists.
 */
@Component
@ConditionalOnProperty(name = "app.init-test-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AdminTestDataInitializer implements CommandLineRunner {

    private static final String SEED_MARKER = "test.host1@travolish.dev";

    private final UserRepository          userRepository;
    private final HotelRepository         hotelRepository;
    private final RoomRepository          roomRepository;
    private final HotelChangeRequestRepository changeRequestRepository;
    private final ReviewRepository        reviewRepository;
    private final HostKYCRepository       kycRepository;
    private final BookingRepository       bookingRepository;
    private final PricingRuleRepository   pricingRuleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(SEED_MARKER).isPresent()) {
            log.info("Admin test data already present — skipping.");
            return;
        }
        log.info("Seeding admin test data...");

        List<User> users   = seedUsers();
        List<Hotel> hotels = seedHotels(users);
        List<Room> rooms   = seedRooms(hotels);
                           seedKYC(users);
                           seedChangeRequests(users, hotels);
                           seedReviews(users, hotels);
                           seedBookings(rooms, hotels);
                           seedPricingRules(rooms, hotels);

        log.info("Admin test data seeded: {} users, {} hotels, {} rooms.",
                users.size(), hotels.size(), rooms.size());
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private List<User> seedUsers() {
        List<User> users = userRepository.saveAll(List.of(
            user("Maya",   "Chen",    "test.host1@travolish.dev",  "google", "google-sub-t1", "HOST",  "ACTIVE"),
            user("Kenji",  "Sato",    "test.host2@travolish.dev",  "google", "google-sub-t2", "HOST",  "ACTIVE"),
            user("Luca",   "Bianchi", "test.host3@travolish.dev",  "google", "google-sub-t3", "HOST",  "SUSPENDED"),
            user("Sana",   "Malik",   "test.host4@travolish.dev",  "google", "google-sub-t4", "HOST",  "PENDING"),
            user("Sofia",  "Rossi",   "test.guest1@travolish.dev", null,     null,             "GUEST", "ACTIVE"),
            user("Aarav",  "Mehta",   "test.guest2@travolish.dev", null,     null,             "GUEST", "ACTIVE"),
            user("Nolan",  "Park",    "test.guest3@travolish.dev", null,     null,             "GUEST", "SUSPENDED"),
            user("Admin",  "User",    "test.admin@travolish.dev",  null,     null,             "ADMIN", "ACTIVE")
        ));
        log.info("  → {} users", users.size());
        return users;
    }

    private User user(String first, String last, String email,
                      String provider, String providerId,
                      String role, String status) {
        return User.builder()
                .firstName(first).lastName(last).email(email)
                .provider(provider).providerId(providerId)
                .role(role).status(status)
                .build();
    }

    // ── Hotels ────────────────────────────────────────────────────────────────

    private List<Hotel> seedHotels(List<User> users) {
        Long h1 = users.get(0).getId(); // Maya
        Long h2 = users.get(1).getId(); // Kenji
        Long h3 = users.get(2).getId(); // Luca
        Long h4 = users.get(3).getId(); // Sana

        List<Hotel> hotels = hotelRepository.saveAll(List.of(
            hotel(h1, "Lagoon Suite",       "Coral Rd 12",      "Malé",    "Maldives", 4.9, "lagoon@travolish.dev"),
            hotel(h2, "Tokyo Design Loft",  "Shibuya 4-2",      "Tokyo",   "Japan",    4.6, "tokyo@travolish.dev"),
            hotel(h3, "Lake Como Retreat",  "Via Lungolago 88", "Como",    "Italy",    4.8, "como@travolish.dev"),
            hotel(h4, "Desert Dunes Camp",  "Sahara Route 7",   "Merzouga","Morocco",  4.5, "dunes@travolish.dev")
        ));
        log.info("  → {} hotels", hotels.size());
        return hotels;
    }

    private Hotel hotel(Long hostId, String name, String address,
                        String city, String country, double rating, String email) {
        Hotel h = new Hotel();
        h.setHostId(hostId);
        h.setName(name);
        h.setAddress(address);
        h.setCity(city);
        h.setCountry(country);
        h.setRating(rating);
        h.setEmail(email);
        h.setDescription("Test hotel for admin panel — " + name);
        return h;
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    private List<Room> seedRooms(List<Hotel> hotels) {
        List<Room> rooms = new ArrayList<>();
        // Two rooms per hotel for realistic booking distribution
        for (Hotel h : hotels) {
            rooms.add(room(h.getId(), "101", "SUITE",  h.getRating() * 250));
            rooms.add(room(h.getId(), "102", "DOUBLE", h.getRating() * 150));
        }
        rooms = roomRepository.saveAll(rooms);
        log.info("  → {} rooms", rooms.size());
        return rooms;
    }

    private Room room(Long hotelId, String number, String type, double price) {
        Room r = new Room();
        r.setHotelId(hotelId);
        r.setNumber(number);
        r.setType(type);
        r.setPricePerNight(Math.round(price * 100.0) / 100.0);
        r.setAvailable(true);
        return r;
    }

    // ── KYC ──────────────────────────────────────────────────────────────────

    private void seedKYC(List<User> users) {
        List<HostKYC> records = new ArrayList<>();
        // host users are users 0–3
        String[][] data = {
            {"PENDING",            "BASIC",    null,        null},
            {"UNDER_REVIEW",       "STANDARD", null,        null},
            {"VERIFIED",           "PREMIUM",  null,        null},
            {"RESUBMIT_REQUESTED", "BASIC",    null,        "Bank statement unreadable"},
        };
        for (int i = 0; i < data.length; i++) {
            User host = users.get(i);
            String[] d = data[i];
            HostKYC kyc = new HostKYC();
            kyc.setHostId(host.getId());
            kyc.setFirstName(host.getFirstName());
            kyc.setLastName(host.getLastName());
            kyc.setKycStatus(d[0]);
            kyc.setVerificationLevel(d[1]);
            if ("VERIFIED".equals(d[0])) {
                kyc.setVerificationDate(LocalDateTime.now().minusDays(10));
            }
            if (d[3] != null) kyc.setNotes(d[3]);
            kyc.setNationality("Test");
            kyc.setBusinessType("Individual");
            records.add(kyc);
        }
        kycRepository.saveAll(records);
        log.info("  → {} KYC records", records.size());
    }

    // ── Hotel Change Requests ─────────────────────────────────────────────────

    private void seedChangeRequests(List<User> users, List<Hotel> hotels) {
        List<HotelChangeRequest> reqs = List.of(
            changeReq(hotels.get(0).getId(), "CREATE", "Lagoon Suite",     "Malé",       4.9, "PENDING",  null),
            changeReq(hotels.get(1).getId(), "UPDATE", "Tokyo Design Loft","Tokyo",      4.6, "PENDING",  null),
            changeReq(null,                  "CREATE", "Sunset Villa",     "Bali",       4.7, "PENDING",  null),
            changeReq(hotels.get(2).getId(), "UPDATE", "Lake Como Retreat","Como",       4.8, "APPROVED", "Looks great, approved."),
            changeReq(null,                  "CREATE", "Budget Inn",       "Mumbai",     2.1, "REJECTED", "Insufficient photos and description.")
        );
        changeRequestRepository.saveAll(reqs);
        log.info("  → {} hotel change requests", reqs.size());
    }

    private HotelChangeRequest changeReq(Long hotelId, String type, String name,
                                         String city, double rating,
                                         String status, String comment) {
        HotelChangeRequest r = new HotelChangeRequest();
        r.setHotelId(hotelId);
        r.setRequestType(HotelChangeRequest.RequestType.valueOf(type));
        r.setName(name);
        r.setCity(city);
        r.setRating(rating);
        r.setEmail(name.toLowerCase().replace(" ", "") + "@test.dev");
        r.setStatus(HotelChangeRequest.RequestStatus.valueOf(status));
        r.setAdminComment(comment);
        if (!"PENDING".equals(status)) {
            r.setProcessedAt(OffsetDateTime.now().minusDays(2));
        }
        return r;
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    private void seedReviews(List<User> users, List<Hotel> hotels) {
        Long g1 = users.get(4).getId(); // Sofia
        Long g2 = users.get(5).getId(); // Aarav
        Long g3 = users.get(6).getId(); // Nolan

        List<Review> reviews = List.of(
            review(g1, hotels.get(0).getId(), null, "Misleading photos",    "The pool shown is shared, not private as advertised.",   2, "FLAGGED"),
            review(g2, hotels.get(1).getId(), null, "Wrong amenities",      "Workspace listed but there were no desks in rooms.",      3, "FLAGGED"),
            review(g3, hotels.get(2).getId(), null, "Safety concern",       "Fire exit was blocked on floor 3.",                       1, "ESCALATED"),
            review(g2, hotels.get(0).getId(), null, "Great stay!",          "Loved the water villa, breakfast was excellent.",         5, "PENDING"),
            review(g1, hotels.get(2).getId(), null, "Beautiful location",   "Lake views are stunning, staff very attentive.",          5, "PENDING"),
            review(g3, hotels.get(1).getId(), null, "Excellent design",     "Modern and minimalist. Perfect Tokyo experience.",        4, "PENDING"),
            review(g2, hotels.get(3).getId(), null, "Desert magic",         "Sleeping under the stars was incredible.",                5, "APPROVED"),
            review(g1, hotels.get(3).getId(), null, "Overpriced",           "Nice experience but not worth the premium.",              3, "REJECTED")
        );
        reviewRepository.saveAll(reviews);
        log.info("  → {} reviews", reviews.size());
    }

    private Review review(Long userId, Long hotelId, Long roomId,
                          String title, String content, int rating, String status) {
        return Review.builder()
                .userId(userId)
                .hotelId(hotelId)
                .roomId(roomId)
                .title(title)
                .content(content)
                .rating(rating)
                .reviewType(Review.ReviewType.HOTEL)
                .status(Review.ReviewStatus.valueOf(status))
                .build();
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    private void seedBookings(List<Room> rooms, List<Hotel> hotels) {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = new ArrayList<>();

        // Spread bookings across last 7 days — 2 per day
        String[][] guests = {
            {"Sofia Rossi",  "sofia@test.dev"},
            {"Aarav Mehta",  "aarav@test.dev"},
            {"Nolan Park",   "nolan@test.dev"},
            {"Claire Dubois","claire@test.dev"},
            {"James Kim",    "james@test.dev"},
            {"Priya Nair",   "priya@test.dev"},
            {"Marco Ricci",  "marco@test.dev"},
        };

        for (int i = 0; i < 7; i++) {
            LocalDate checkIn  = today.minusDays(i);
            LocalDate checkOut = checkIn.plusDays(3);
            Room room  = rooms.get(i % rooms.size());
            Hotel hotel = hotels.get(i % hotels.size());
            String[] g  = guests[i];
            String status = i < 2 ? "CONFIRMED" : i < 5 ? "PENDING" : "COMPLETED";

            Booking b = new Booking();
            b.setRoomId(room.getId());
            b.setHotelId(hotel.getId());
            b.setGuestName(g[0]);
            b.setGuestEmail(g[1]);
            b.setCheckInDate(checkIn);
            b.setCheckOutDate(checkOut);
            b.setBasePrice(room.getPricePerNight());
            b.setTotalPrice(room.getPricePerNight() * 3);
            b.setStatus(Booking.BookingStatus.valueOf(status));
            bookings.add(b);
        }

        // Add a second booking per day with different room
        for (int i = 0; i < 7; i++) {
            LocalDate checkIn  = today.minusDays(i);
            LocalDate checkOut = checkIn.plusDays(2);
            Room room  = rooms.get((i + 1) % rooms.size());
            Hotel hotel = hotels.get((i + 1) % hotels.size());

            Booking b = new Booking();
            b.setRoomId(room.getId());
            b.setHotelId(hotel.getId());
            b.setGuestName("Test Guest " + (i + 10));
            b.setGuestEmail("guest" + (i + 10) + "@test.dev");
            b.setCheckInDate(checkIn);
            b.setCheckOutDate(checkOut);
            b.setBasePrice(room.getPricePerNight());
            b.setTotalPrice(room.getPricePerNight() * 2);
            b.setStatus(Booking.BookingStatus.CONFIRMED);
            bookings.add(b);
        }

        bookingRepository.saveAll(bookings);
        log.info("  → {} bookings", bookings.size());
    }

    // ── Pricing Rules ─────────────────────────────────────────────────────────

    private void seedPricingRules(List<Room> rooms, List<Hotel> hotels) {
        LocalDate today = LocalDate.now();
        Room r1 = rooms.get(0); Hotel h1 = hotels.get(0);
        Room r2 = rooms.get(2); Hotel h2 = hotels.get(1);
        Room r3 = rooms.get(4); Hotel h3 = hotels.get(2);

        List<PricingRule> rules = new ArrayList<>();

        // SEASONAL — summer peak
        rules.add(rule(r1.getId(), h1.getId(), PricingRule.RuleType.SEASONAL,
                PricingRule.PricingType.PERCENTAGE,
                today.minusDays(30), today.plusDays(60),
                r1.getPricePerNight(), 1.35, null,
                "Summer peak season", 10, "SUMMER", "Summer Premium +35%", true));

        // PROMOTIONAL — flash sale
        rules.add(rule(r2.getId(), h2.getId(), PricingRule.RuleType.PROMOTIONAL,
                PricingRule.PricingType.PERCENTAGE,
                today, today.plusDays(14),
                r2.getPricePerNight(), 0.8, null,
                null, 30, null, "Flash Sale -20%", true));

        // DYNAMIC — weekend
        rules.add(rule(r3.getId(), h3.getId(), PricingRule.RuleType.DYNAMIC,
                PricingRule.PricingType.PERCENTAGE,
                today.minusDays(90), today.plusDays(90),
                r3.getPricePerNight(), 1.18, null,
                null, 20, null, "Weekend Dynamic +18%", true));

        // EARLY_BIRD — 45 days advance
        rules.add(rule(r1.getId(), h1.getId(), PricingRule.RuleType.EARLY_BIRD,
                PricingRule.PricingType.PERCENTAGE,
                today.plusDays(45), today.plusDays(120),
                r1.getPricePerNight(), 0.85, null,
                null, 25, null, "Early Bird -15%", true));

        // LAST_MINUTE — next 48h
        rules.add(rule(r2.getId(), h2.getId(), PricingRule.RuleType.LAST_MINUTE,
                PricingRule.PricingType.FLAT,
                today, today.plusDays(2),
                r2.getPricePerNight(), 1.0, 40.0,
                null, 40, null, "Last Minute -$40", true));

        // BULK — 7+ nights
        rules.add(rule(r3.getId(), h3.getId(), PricingRule.RuleType.BULK,
                PricingRule.PricingType.PERCENTAGE,
                today.minusDays(60), today.plusDays(120),
                r3.getPricePerNight(), 0.9, null,
                null, 15, null, "7-Night Stay -10%", true));

        // LOYALTY — members only (draft)
        rules.add(rule(r1.getId(), h1.getId(), PricingRule.RuleType.LOYALTY,
                PricingRule.PricingType.PERCENTAGE,
                today.minusDays(30), today.plusDays(180),
                r1.getPricePerNight(), 0.88, null,
                null, 35, null, "Loyalty Member -12%", false));

        pricingRuleRepository.saveAll(rules);
        log.info("  → {} pricing rules", rules.size());
    }

    private PricingRule rule(Long roomId, Long hotelId,
                             PricingRule.RuleType ruleType, PricingRule.PricingType pricingType,
                             LocalDate start, LocalDate end,
                             double basePrice, double multiplier, Double fixedDiscount,
                             String season, int priority,
                             String description, String ruleDesc, boolean active) {
        return PricingRule.builder()
                .roomId(roomId)
                .hotelId(hotelId)
                .ruleType(ruleType)
                .pricingType(pricingType)
                .startDate(start)
                .endDate(end)
                .basePrice(basePrice)
                .multiplier(multiplier)
                .fixedDiscount(fixedDiscount)
                .season(season)
                .priority(priority)
                .description(ruleDesc != null ? ruleDesc : description)
                .isActive(active)
                .build();
    }
}
