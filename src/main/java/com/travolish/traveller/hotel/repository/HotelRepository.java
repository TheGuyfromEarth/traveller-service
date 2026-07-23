package com.travolish.traveller.hotel.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.hotel.model.Hotel;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    List<Hotel> findByHostId(Long hostId);

    List<Hotel> findByStatus(Hotel.HotelStatus status);

    long countByStatus(Hotel.HotelStatus status);

    @Query("SELECT h.id, COUNT(r) FROM Hotel h LEFT JOIN h.reviews r WHERE h.id IN :ids GROUP BY h.id")
    List<Object[]> countReviewsByHotelIds(@Param("ids") List<Long> ids);

    /** Scalar-only query — returns id and name without loading any @ElementCollection fields. */
    @Query("SELECT h.id, h.name FROM Hotel h WHERE h.id IN :ids")
    List<Object[]> findIdAndNameByIdIn(@Param("ids") Collection<Long> ids);

}
