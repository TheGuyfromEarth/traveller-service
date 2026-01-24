package com.travolish.traveller.pricing.repository;

import com.travolish.traveller.pricing.entity.BoostListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoostListingRepository extends JpaRepository<BoostListing, Long> {
    
    List<BoostListing> findByHotelId(Long hotelId);
    
    List<BoostListing> findByRoomId(Long roomId);
    
    List<BoostListing> findByStatus(String status);
    
    Page<BoostListing> findByHotelIdAndStatus(Long hotelId, String status, Pageable pageable);
    
    @Query("SELECT bl FROM BoostListing bl WHERE bl.status = 'ACTIVE' AND bl.endDate > CURRENT_TIMESTAMP")
    List<BoostListing> findActiveBoosts();
    
    @Query("SELECT bl FROM BoostListing bl WHERE bl.hotelId = :hotelId AND bl.status = 'ACTIVE' AND bl.endDate > CURRENT_TIMESTAMP")
    List<BoostListing> findActiveBoostsForHotel(Long hotelId);
    
    @Query("SELECT bl FROM BoostListing bl WHERE bl.status = 'ACTIVE' AND bl.endDate <= CURRENT_TIMESTAMP")
    List<BoostListing> findExpiredBoosts();
}
