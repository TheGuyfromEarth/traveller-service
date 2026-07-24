package com.travolish.traveller.pricing.repository;

import com.travolish.traveller.pricing.entity.PricingSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
    
    List<PricingSuggestion> findByHotelId(Long hotelId);
    
    List<PricingSuggestion> findByRoomId(Long roomId);
    
    List<PricingSuggestion> findByStatus(String status);
    
    Page<PricingSuggestion> findByHotelIdAndStatus(Long hotelId, PricingSuggestion.SuggestionStatus status, Pageable pageable);
    
    Page<PricingSuggestion> findByRoomId(Long roomId, Pageable pageable);
    
    @Query("SELECT ps FROM PricingSuggestion ps WHERE ps.hotelId = :hotelId AND ps.suggestedFromDate <= :date AND ps.suggestedToDate >= :date")
    List<PricingSuggestion> findSuggestionsForDateRange(Long hotelId, LocalDate date);
    
    @Query("SELECT ps FROM PricingSuggestion ps WHERE ps.roomId = :roomId AND ps.status = 'PENDING' ORDER BY ps.confidenceScore DESC")
    List<PricingSuggestion> findPendingSuggestionsByConfidence(Long roomId);

    @Modifying
    @Query("UPDATE PricingSuggestion ps SET ps.status = com.travolish.traveller.pricing.entity.PricingSuggestion.SuggestionStatus.EXPIRED " +
           "WHERE ps.status = com.travolish.traveller.pricing.entity.PricingSuggestion.SuggestionStatus.PENDING " +
           "AND ps.suggestedToDate < :today")
    int expirePendingSuggestions(@Param("today") LocalDate today);
}
