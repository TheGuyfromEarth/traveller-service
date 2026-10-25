package com.travolish.traveller.hotel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.hotel.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    /**
     * Returns [hotelId, minPricePerNight] rows for the given hotel IDs in a
     * single GROUP BY query — used by HotelSearchService to populate
     * cheapestRoomPrice without loading full Room entities.
     */
    @Query("SELECT r.hotelId, MIN(r.pricePerNight) FROM Room r " +
           "WHERE r.hotelId IN :hotelIds AND r.available = true " +
           "GROUP BY r.hotelId")
    List<Object[]> findCheapestPriceByHotelIds(@Param("hotelIds") List<Long> hotelIds);

}
