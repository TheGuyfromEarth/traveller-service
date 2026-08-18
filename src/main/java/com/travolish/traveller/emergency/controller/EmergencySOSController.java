package com.travolish.traveller.emergency.controller;

import com.travolish.traveller.emergency.dto.EmergencySOSDTO;
import com.travolish.traveller.emergency.dto.ActivateSOSRequest;
import com.travolish.traveller.emergency.dto.EmergencyContactDTO;
import com.travolish.traveller.emergency.service.EmergencySOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmergencySOSController {

    private final EmergencySOSService emergencySOSService;

    /**
     * Activate emergency SOS
     * POST /api/emergency/sos/activate
     */
    @PostMapping("/sos/activate")
    public ResponseEntity<EmergencySOSDTO> activateSOS(
            @Valid @RequestBody ActivateSOSRequest request) {
        try {
            log.warn("Emergency SOS activation request for user: {}", request.getUserId());
            EmergencySOSDTO sos = emergencySOSService.activateSOS(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(sos);
        } catch (Exception e) {
            log.error("Error activating SOS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get SOS by ID
     * GET /api/emergency/sos/{sosId}
     */
    @GetMapping("/sos/{sosId}")
    public ResponseEntity<EmergencySOSDTO> getSOSById(@PathVariable Long sosId) {
        try {
            EmergencySOSDTO sos = emergencySOSService.getSOSById(sosId);
            return ResponseEntity.ok(sos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get SOS history for user
     * GET /api/emergency/sos/user/{userId}
     */
    @GetMapping("/sos/user/{userId}")
    public ResponseEntity<Page<EmergencySOSDTO>> getUserSOSHistory(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<EmergencySOSDTO> sosHistory = emergencySOSService.getUserSOSHistory(userId, pageable);
        return ResponseEntity.ok(sosHistory);
    }

    /**
     * Get all active SOS calls
     * GET /api/emergency/sos/active
     */
    @GetMapping("/sos/active")
    public ResponseEntity<List<EmergencySOSDTO>> getActiveSOSCalls() {
        List<EmergencySOSDTO> activeCalls = emergencySOSService.getActiveSOSCalls();
        return ResponseEntity.ok(activeCalls);
    }

    /**
     * Get active SOS calls for hotel
     * GET /api/emergency/sos/hotel/{hotelId}/active
     */
    @GetMapping("/sos/hotel/{hotelId}/active")
    public ResponseEntity<List<EmergencySOSDTO>> getActiveSOSCallsForHotel(
            @PathVariable Long hotelId) {
        List<EmergencySOSDTO> activeCalls = emergencySOSService.getActiveSOSCallsForHotel(hotelId);
        return ResponseEntity.ok(activeCalls);
    }

    /**
     * Update SOS status
     * PUT /api/emergency/sos/{sosId}/status
     */
    @PutMapping("/sos/{sosId}/status")
    public ResponseEntity<EmergencySOSDTO> updateSOSStatus(
            @PathVariable Long sosId,
            @RequestParam String status) {
        try {
            EmergencySOSDTO sos = emergencySOSService.updateSOSStatus(sosId, status);
            return ResponseEntity.ok(sos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign SOS to support staff
     * POST /api/emergency/sos/{sosId}/assign
     */
    @PostMapping("/sos/{sosId}/assign")
    public ResponseEntity<EmergencySOSDTO> assignSOSToSupport(
            @PathVariable Long sosId,
            @RequestParam Long supportId) {
        try {
            EmergencySOSDTO sos = emergencySOSService.assignSOSToSupport(sosId, supportId);
            return ResponseEntity.ok(sos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolve SOS
     * POST /api/emergency/sos/{sosId}/resolve
     */
    @PostMapping("/sos/{sosId}/resolve")
    public ResponseEntity<EmergencySOSDTO> resolveSOS(
            @PathVariable Long sosId,
            @RequestParam(required = false) String notes) {
        try {
            log.info("Resolving SOS: {}", sosId);
            EmergencySOSDTO sos = emergencySOSService.resolveSOS(sosId, notes);
            return ResponseEntity.ok(sos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get emergency contacts for location
     * GET /api/emergency/contacts
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<EmergencyContactDTO>> getNearestEmergencyContacts(
            @RequestParam String country,
            @RequestParam String city) {
        List<EmergencyContactDTO> contacts = emergencySOSService.getNearestEmergencyContacts(country, city);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get emergency contacts for a hotel (host-managed chain)
     * GET /api/emergency/contacts/hotel/{hotelId}
     */
    @GetMapping("/contacts/hotel/{hotelId}")
    public ResponseEntity<List<EmergencyContactDTO>> getContactsForHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(emergencySOSService.getContactsForHotel(hotelId));
    }

    /**
     * Create a new emergency contact
     * POST /api/emergency/contacts
     */
    @PostMapping("/contacts")
    public ResponseEntity<EmergencyContactDTO> createEmergencyContact(
            @RequestBody EmergencyContactDTO dto) {
        try {
            EmergencyContactDTO created = emergencySOSService.createEmergencyContact(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating emergency contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Soft-delete an emergency contact
     * DELETE /api/emergency/contacts/{contactId}
     */
    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Void> deleteEmergencyContact(@PathVariable Long contactId) {
        try {
            emergencySOSService.deleteEmergencyContact(contactId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get emergency contact by ID
     * GET /api/emergency/contacts/{contactId}
     */
    @GetMapping("/contacts/{contactId}")
    public ResponseEntity<EmergencyContactDTO> getEmergencyContactById(
            @PathVariable Long contactId) {
        try {
            EmergencyContactDTO contact = emergencySOSService.getEmergencyContactById(contactId);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Enable live location sharing
     * POST /api/emergency/sos/{sosId}/location/enable
     */
    @PostMapping("/sos/{sosId}/location/enable")
    public ResponseEntity<?> enableLiveLocationSharing(@PathVariable Long sosId) {
        try {
            emergencySOSService.enableLiveLocationSharing(sosId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Disable live location sharing
     * POST /api/emergency/sos/{sosId}/location/disable
     */
    @PostMapping("/sos/{sosId}/location/disable")
    public ResponseEntity<?> disableLiveLocationSharing(@PathVariable Long sosId) {
        try {
            emergencySOSService.disableLiveLocationSharing(sosId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
