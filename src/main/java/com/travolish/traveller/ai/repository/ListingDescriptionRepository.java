package com.travolish.traveller.ai.repository;

import com.travolish.traveller.ai.entity.ListingDescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListingDescriptionRepository extends JpaRepository<ListingDescription, Long> {
    
    List<ListingDescription> findByHotelId(Long hotelId);
    
    List<ListingDescription> findByRoomId(Long roomId);
    
    Page<ListingDescription> findByHotelId(Long hotelId, Pageable pageable);
    
    Page<ListingDescription> findByRoomId(Long roomId, Pageable pageable);
    
    @Query("SELECT ld FROM ListingDescription ld WHERE ld.hotelId = :hotelId AND ld.isActive = true")
    List<ListingDescription> findActiveDescriptionsForHotel(Long hotelId);
    
    @Query("SELECT ld FROM ListingDescription ld WHERE ld.roomId = :roomId AND ld.isActive = true")
    List<ListingDescription> findActiveDescriptionsForRoom(Long roomId);
    
    @Query("SELECT ld FROM ListingDescription ld WHERE ld.hotelId = :hotelId AND ld.descriptionType = :type AND ld.isActive = true")
    List<ListingDescription> findActiveByTypeAndHotel(Long hotelId, String type);
    
    @Query("SELECT ld FROM ListingDescription ld WHERE ld.status = 'PENDING' ORDER BY ld.createdAt ASC")
    Page<ListingDescription> findPendingApprovals(Pageable pageable);
}
