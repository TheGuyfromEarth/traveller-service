package com.travolish.traveller.emergency.repository;

import com.travolish.traveller.emergency.entity.EmergencySOS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmergencySOSRepository extends JpaRepository<EmergencySOS, Long> {
    
    List<EmergencySOS> findByUserId(Long userId);
    
    List<EmergencySOS> findByBookingId(Long bookingId);
    
    Page<EmergencySOS> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT es FROM EmergencySOS es WHERE es.status IN ('ACTIVATED', 'ACKNOWLEDGED', 'IN_PROGRESS')")
    List<EmergencySOS> findActiveSOSCalls();
    
    @Query("SELECT es FROM EmergencySOS es WHERE es.hotelId = :hotelId AND es.status IN ('ACTIVATED', 'ACKNOWLEDGED', 'IN_PROGRESS')")
    List<EmergencySOS> findActiveSOSCallsForHotel(Long hotelId);
    
    @Query("SELECT es FROM EmergencySOS es WHERE es.activatedAt >= :startTime ORDER BY es.activatedAt DESC")
    List<EmergencySOS> findSOSCallsAfter(LocalDateTime startTime);
}
