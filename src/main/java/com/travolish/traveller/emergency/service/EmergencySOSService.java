package com.travolish.traveller.emergency.service;

import com.travolish.traveller.emergency.dto.EmergencySOSDTO;
import com.travolish.traveller.emergency.dto.ActivateSOSRequest;
import com.travolish.traveller.emergency.dto.EmergencyContactDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface EmergencySOSService {
    EmergencySOSDTO activateSOS(ActivateSOSRequest request);
    
    EmergencySOSDTO getSOSById(Long sosId);
    
    Page<EmergencySOSDTO> getUserSOSHistory(Long userId, Pageable pageable);
    
    List<EmergencySOSDTO> getActiveSOSCalls();
    
    List<EmergencySOSDTO> getActiveSOSCallsForHotel(Long hotelId);
    
    EmergencySOSDTO updateSOSStatus(Long sosId, String newStatus);
    
    EmergencySOSDTO assignSOSToSupport(Long sosId, Long supportId);
    
    EmergencySOSDTO resolveSOS(Long sosId, String resolutionNotes);
    
    List<EmergencyContactDTO> getNearestEmergencyContacts(String country, String city);

    List<EmergencyContactDTO> getContactsForHotel(Long hotelId);

    EmergencyContactDTO createEmergencyContact(EmergencyContactDTO dto);

    void deleteEmergencyContact(Long contactId);

    EmergencyContactDTO getEmergencyContactById(Long contactId);
    
    void notifyEmergencyContacts(Long sosId);
    
    void enableLiveLocationSharing(Long sosId);
    
    void disableLiveLocationSharing(Long sosId);
}
