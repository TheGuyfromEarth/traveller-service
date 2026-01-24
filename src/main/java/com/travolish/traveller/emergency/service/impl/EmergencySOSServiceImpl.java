package com.travolish.traveller.emergency.service.impl;

import com.travolish.traveller.emergency.dto.EmergencySOSDTO;
import com.travolish.traveller.emergency.dto.ActivateSOSRequest;
import com.travolish.traveller.emergency.dto.EmergencyContactDTO;
import com.travolish.traveller.emergency.entity.EmergencySOS;
import com.travolish.traveller.emergency.entity.EmergencyContact;
import com.travolish.traveller.emergency.repository.EmergencySOSRepository;
import com.travolish.traveller.emergency.repository.EmergencyContactRepository;
import com.travolish.traveller.emergency.service.EmergencySOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmergencySOSServiceImpl implements EmergencySOSService {

    private final EmergencySOSRepository emergencySOSRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    @Override
    public EmergencySOSDTO activateSOS(ActivateSOSRequest request) {
        log.warn("Emergency SOS activated for user: {} in hotel: {}", request.getUserId(), request.getHotelId());

        EmergencySOS sos = EmergencySOS.builder()
                .userId(request.getUserId())
                .bookingId(request.getBookingId())
                .hotelId(request.getHotelId())
                .sosType(EmergencySOS.SOSType.valueOf(request.getSosType()))
                .status(EmergencySOS.SOSStatus.ACTIVATED)
                .emergencyDescription(request.getEmergencyDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .userPhoneNumber(request.getPhoneNumber())
                .userCountry(request.getCountry())
                .userCity(request.getCity())
                .liveLocationSharing(true)
                .emergencyContactsNotified(0)
                .localAuthoritiesContacted(0)
                .build();

        EmergencySOS saved = emergencySOSRepository.save(sos);
        
        // Notify emergency contacts
        notifyEmergencyContacts(saved.getId());
        
        log.info("SOS activated with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Override
    public EmergencySOSDTO getSOSById(Long sosId) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));
        return mapToDTO(sos);
    }

    @Override
    public Page<EmergencySOSDTO> getUserSOSHistory(Long userId, Pageable pageable) {
        return emergencySOSRepository.findByUserId(userId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<EmergencySOSDTO> getActiveSOSCalls() {
        return emergencySOSRepository.findActiveSOSCalls()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmergencySOSDTO> getActiveSOSCallsForHotel(Long hotelId) {
        return emergencySOSRepository.findActiveSOSCallsForHotel(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencySOSDTO updateSOSStatus(Long sosId, String newStatus) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        sos.setStatus(EmergencySOS.SOSStatus.valueOf(newStatus));
        EmergencySOS saved = emergencySOSRepository.save(sos);

        log.info("SOS {} status updated to: {}", sosId, newStatus);
        return mapToDTO(saved);
    }

    @Override
    public EmergencySOSDTO assignSOSToSupport(Long sosId, Long supportId) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        sos.setAssignedToSupportId(supportId);
        sos.setStatus(EmergencySOS.SOSStatus.ACKNOWLEDGED);
        
        EmergencySOS saved = emergencySOSRepository.save(sos);
        log.info("SOS {} assigned to support: {}", sosId, supportId);

        return mapToDTO(saved);
    }

    @Override
    public EmergencySOSDTO resolveSOS(Long sosId, String resolutionNotes) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        sos.setStatus(EmergencySOS.SOSStatus.RESOLVED);
        sos.setResolvedAt(LocalDateTime.now());
        sos.setResolutionNotes(resolutionNotes);
        sos.setLiveLocationSharing(false);

        EmergencySOS saved = emergencySOSRepository.save(sos);
        log.info("SOS {} resolved", sosId);

        return mapToDTO(saved);
    }

    @Override
    public List<EmergencyContactDTO> getNearestEmergencyContacts(String country, String city) {
        return emergencyContactRepository.findActiveByLocation(country, city)
                .stream()
                .map(this::mapContactToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencyContactDTO getEmergencyContactById(Long contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        return mapContactToDTO(contact);
    }

    @Override
    public void notifyEmergencyContacts(Long sosId) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        List<EmergencyContact> contacts = emergencyContactRepository.findActiveByLocation(sos.getUserCountry(), sos.getUserCity());
        
        log.info("Notifying {} emergency contacts for SOS: {}", contacts.size(), sosId);
        sos.setEmergencyContactsNotified(contacts.size());
        emergencySOSRepository.save(sos);
    }

    @Override
    public void enableLiveLocationSharing(Long sosId) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        sos.setLiveLocationSharing(true);
        emergencySOSRepository.save(sos);
        log.info("Live location sharing enabled for SOS: {}", sosId);
    }

    @Override
    public void disableLiveLocationSharing(Long sosId) {
        EmergencySOS sos = emergencySOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("SOS record not found"));

        sos.setLiveLocationSharing(false);
        emergencySOSRepository.save(sos);
        log.info("Live location sharing disabled for SOS: {}", sosId);
    }

    private EmergencySOSDTO mapToDTO(EmergencySOS sos) {
        return EmergencySOSDTO.builder()
                .id(sos.getId())
                .userId(sos.getUserId())
                .bookingId(sos.getBookingId())
                .hotelId(sos.getHotelId())
                .status(sos.getStatus().toString())
                .sosType(sos.getSosType().toString())
                .emergencyDescription(sos.getEmergencyDescription())
                .latitude(sos.getLatitude())
                .longitude(sos.getLongitude())
                .userPhoneNumber(sos.getUserPhoneNumber())
                .userCountry(sos.getUserCountry())
                .userCity(sos.getUserCity())
                .activatedAt(sos.getActivatedAt())
                .emergencyContactsNotified(sos.getEmergencyContactsNotified())
                .localAuthoritiesContacted(sos.getLocalAuthoritiesContacted())
                .liveLocationSharing(sos.getLiveLocationSharing())
                .build();
    }

    private EmergencyContactDTO mapContactToDTO(EmergencyContact contact) {
        return EmergencyContactDTO.builder()
                .id(contact.getId())
                .country(contact.getCountry())
                .city(contact.getCity())
                .contactType(contact.getContactType().toString())
                .contactNumber(contact.getContactNumber())
                .contactName(contact.getContactName())
                .description(contact.getDescription())
                .address(contact.getAddress())
                .latitude(contact.getLatitude())
                .longitude(contact.getLongitude())
                .email(contact.getEmail())
                .operatingHours(contact.getOperatingHours())
                .responseTimeMinutes(contact.getResponseTimeMinutes())
                .isActive(contact.getIsActive())
                .build();
    }
}
