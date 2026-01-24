package com.travolish.traveller.ai.controller;

import com.travolish.traveller.ai.dto.ListingDescriptionDTO;
import com.travolish.traveller.ai.dto.GenerateDescriptionRequest;
import com.travolish.traveller.ai.service.DescriptionGeneratorService;
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
@RequestMapping("/api/ai/descriptions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class DescriptionGeneratorController {

    private final DescriptionGeneratorService descriptionGeneratorService;

    /**
     * Generate AI description
     * POST /api/ai/descriptions/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<ListingDescriptionDTO> generateDescription(
            @Valid @RequestBody GenerateDescriptionRequest request) {
        try {
            log.info("Generating AI description for room: {}", request.getRoomId());
            ListingDescriptionDTO description = descriptionGeneratorService.generateDescription(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(description);
        } catch (Exception e) {
            log.error("Error generating description", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get descriptions for hotel
     * GET /api/ai/descriptions/hotel/{hotelId}
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<ListingDescriptionDTO>> getDescriptionsForHotel(
            @PathVariable Long hotelId) {
        List<ListingDescriptionDTO> descriptions = descriptionGeneratorService.getDescriptionsForHotel(hotelId);
        return ResponseEntity.ok(descriptions);
    }

    /**
     * Get descriptions for room
     * GET /api/ai/descriptions/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ListingDescriptionDTO>> getDescriptionsForRoom(
            @PathVariable Long roomId) {
        List<ListingDescriptionDTO> descriptions = descriptionGeneratorService.getDescriptionsForRoom(roomId);
        return ResponseEntity.ok(descriptions);
    }

    /**
     * Get pending approvals (paginated)
     * GET /api/ai/descriptions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<ListingDescriptionDTO>> getPendingApprovals(Pageable pageable) {
        Page<ListingDescriptionDTO> descriptions = descriptionGeneratorService.getPendingApprovals(pageable);
        return ResponseEntity.ok(descriptions);
    }

    /**
     * Approve description
     * POST /api/ai/descriptions/{descriptionId}/approve
     */
    @PostMapping("/{descriptionId}/approve")
    public ResponseEntity<ListingDescriptionDTO> approveDescription(
            @PathVariable Long descriptionId,
            @RequestParam(required = false) String notes) {
        try {
            log.info("Approving description: {}", descriptionId);
            ListingDescriptionDTO description = descriptionGeneratorService.approveDescription(descriptionId, notes);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            log.error("Error approving description", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject description
     * POST /api/ai/descriptions/{descriptionId}/reject
     */
    @PostMapping("/{descriptionId}/reject")
    public ResponseEntity<ListingDescriptionDTO> rejectDescription(
            @PathVariable Long descriptionId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("Rejecting description: {}", descriptionId);
            ListingDescriptionDTO description = descriptionGeneratorService.rejectDescription(descriptionId, reason);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            log.error("Error rejecting description", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate description
     * POST /api/ai/descriptions/{descriptionId}/activate
     */
    @PostMapping("/{descriptionId}/activate")
    public ResponseEntity<ListingDescriptionDTO> activateDescription(
            @PathVariable Long descriptionId) {
        try {
            ListingDescriptionDTO description = descriptionGeneratorService.activateDescription(descriptionId);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deactivate description
     * POST /api/ai/descriptions/{descriptionId}/deactivate
     */
    @PostMapping("/{descriptionId}/deactivate")
    public ResponseEntity<ListingDescriptionDTO> deactivateDescription(
            @PathVariable Long descriptionId) {
        try {
            ListingDescriptionDTO description = descriptionGeneratorService.deactivateDescription(descriptionId);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active descriptions for hotel
     * GET /api/ai/descriptions/hotel/{hotelId}/active
     */
    @GetMapping("/hotel/{hotelId}/active")
    public ResponseEntity<List<ListingDescriptionDTO>> getActiveDescriptionsForHotel(
            @PathVariable Long hotelId) {
        List<ListingDescriptionDTO> descriptions = descriptionGeneratorService.getActiveDescriptionsForHotel(hotelId);
        return ResponseEntity.ok(descriptions);
    }
}
