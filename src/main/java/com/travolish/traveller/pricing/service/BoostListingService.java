package com.travolish.traveller.pricing.service;

import com.travolish.traveller.pricing.dto.BoostListingDTO;
import com.travolish.traveller.pricing.dto.BoostListingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BoostListingService {
    BoostListingDTO purchaseBoost(BoostListingRequest request);
    
    BoostListingDTO getBoostById(Long boostId);
    
    List<BoostListingDTO> getActiveBoostsForHotel(Long hotelId);
    
    Page<BoostListingDTO> getBoostsForHotel(Long hotelId, Pageable pageable);
    
    BoostListingDTO cancelBoost(Long boostId, String reason);
    
    List<BoostListingDTO> findExpiredBoosts();
    
    BoostListingDTO updateBoostStatus(Long boostId, String newStatus);
    
    List<BoostListingDTO> getBoostAnalytics(Long hotelId);
    
    boolean isListingBoosted(Long roomId);
}
