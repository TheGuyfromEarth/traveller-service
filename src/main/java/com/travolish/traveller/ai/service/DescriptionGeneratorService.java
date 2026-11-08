package com.travolish.traveller.ai.service;

import com.travolish.traveller.ai.dto.ListingDescriptionDTO;
import com.travolish.traveller.ai.dto.GenerateDescriptionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DescriptionGeneratorService {
    ListingDescriptionDTO generateDescription(GenerateDescriptionRequest request);
    
    List<ListingDescriptionDTO> getDescriptionsForHotel(Long hotelId);
    
    List<ListingDescriptionDTO> getDescriptionsForRoom(Long roomId);
    
    Page<ListingDescriptionDTO> getPendingApprovals(Pageable pageable);
    
    ListingDescriptionDTO approveDescription(Long descriptionId, String approvalNotes);
    
    ListingDescriptionDTO rejectDescription(Long descriptionId, String rejectionReason);
    
    ListingDescriptionDTO activateDescription(Long descriptionId);
    
    ListingDescriptionDTO deactivateDescription(Long descriptionId);
    
    List<ListingDescriptionDTO> getActiveDescriptionsForHotel(Long hotelId);
    
    String translateDescription(String text, String fromLanguage, String toLanguage);
}
