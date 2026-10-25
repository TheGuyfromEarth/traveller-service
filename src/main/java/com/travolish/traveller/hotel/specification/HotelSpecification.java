package com.travolish.traveller.hotel.specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.Room;

public class HotelSpecification {

    public static Specification<Hotel> withCountry(String country) {
        return (root, query, criteriaBuilder) ->
                country == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("country")),
                        country.toLowerCase());
    }

    public static Specification<Hotel> withCity(String city) {
        return (root, query, criteriaBuilder) ->
                city == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("city")),
                        "%" + city.toLowerCase() + "%");
    }

    public static Specification<Hotel> withName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%");
    }

    public static Specification<Hotel> withMinRating(Double minRating) {
        return (root, query, criteriaBuilder) ->
                minRating == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Hotel> withMaxRating(Double maxRating) {
        return (root, query, criteriaBuilder) ->
                maxRating == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating);
    }

    public static Specification<Hotel> withBbox(Double latMin, Double latMax, Double lngMin, Double lngMax) {
        return (root, query, cb) -> {
            if (latMin == null || latMax == null || lngMin == null || lngMax == null)
                return cb.conjunction();
            return cb.and(
                cb.greaterThanOrEqualTo(root.get("latitude"), latMin),
                cb.lessThanOrEqualTo(root.get("latitude"), latMax),
                cb.greaterThanOrEqualTo(root.get("longitude"), lngMin),
                cb.lessThanOrEqualTo(root.get("longitude"), lngMax)
            );
        };
    }

    public static Specification<Hotel> withQuery(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) return cb.conjunction();
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("city")), pattern),
                cb.like(cb.lower(root.get("country")), pattern)
            );
        };
    }

    public static Specification<Hotel> withStatus(Hotel.HotelStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    /**
     * Filters hotels to those that have at least one room satisfying ALL of:
     *   - {@code room.available = true}   (not blocked by the host)
     *   - {@code room.capacity >= guests} (when guests is provided)
     *   - no CONFIRMED or PENDING booking whose dates overlap [checkIn, checkOut)
     *     (when both dates are provided)
     *
     * <p>The three conditions are collapsed into a single correlated EXISTS so the
     * planner can evaluate them in one index seek on {@code idx_bookings_avail}
     * rather than running separate sub-plans.
     *
     * <p>Overlap rule (standard half-open intervals): a booking conflicts when
     * {@code booking.checkInDate < requestedCheckOut AND booking.checkOutDate > requestedCheckIn}.
     */
    public static Specification<Hotel> withAvailability(LocalDate checkIn, LocalDate checkOut, Integer guests) {
        boolean hasDateFilter  = checkIn != null && checkOut != null;
        boolean hasGuestFilter = guests != null && guests > 0;

        if (!hasDateFilter && !hasGuestFilter) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            // Outer subquery: "does this hotel have a qualifying room?"
            Subquery<Long> roomSub = query.subquery(Long.class);
            Root<Room> room = roomSub.from(Room.class);
            roomSub.select(cb.literal(1L));

            List<Predicate> roomPreds = new ArrayList<>();
            // Correlated: room must belong to the current hotel row
            roomPreds.add(cb.equal(room.get("hotelId"), root.get("id")));
            // Host must not have manually blocked the room
            roomPreds.add(cb.equal(room.get("available"), true));

            if (hasGuestFilter) {
                roomPreds.add(cb.greaterThanOrEqualTo(room.get("capacity"), guests));
            }

            if (hasDateFilter) {
                // Inner subquery: "does this room have a conflicting booking?"
                // Created from roomSub so it is scoped inside the outer subquery.
                Subquery<Long> bookingSub = roomSub.subquery(Long.class);
                Root<Booking> booking = bookingSub.from(Booking.class);
                bookingSub.select(cb.literal(1L));
                bookingSub.where(cb.and(
                    // Correlated: booking must be for the current room
                    cb.equal(booking.get("roomId"), room.get("id")),
                    // Only active bookings block availability
                    booking.get("status").in(
                        Booking.BookingStatus.CONFIRMED,
                        Booking.BookingStatus.PENDING
                    ),
                    // Standard half-open overlap: [checkIn, checkOut) ∩ [bIn, bOut) ≠ ∅
                    cb.lessThan(booking.<LocalDate>get("checkInDate"), checkOut),
                    cb.greaterThan(booking.<LocalDate>get("checkOutDate"), checkIn)
                ));
                // Room is available only when NO such conflicting booking exists
                roomPreds.add(cb.not(cb.exists(bookingSub)));
            }

            roomSub.where(cb.and(roomPreds.toArray(new Predicate[0])));
            return cb.exists(roomSub);
        };
    }

}
