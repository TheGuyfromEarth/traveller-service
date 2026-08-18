package com.travolish.traveller.pricing.controller;

import com.travolish.traveller.pricing.dto.BoostListingDTO;
import com.travolish.traveller.pricing.dto.BoostListingRequest;
import com.travolish.traveller.pricing.service.BoostListingService;
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
@RequestMapping("/api/listings/boost")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class BoostListingController {

    private final BoostListingService boostListingService;

    /**
     * Purchase listing boost
     * POST /api/listings/boost/purchase
     */
    @PostMapping("/purchase")
    public ResponseEntity<BoostListingDTO> purchaseBoost(
            @Valid @RequestBody BoostListingRequest request) {
        try {
            log.info("Purchasing boost for room: {}", request.getRoomId());
            BoostListingDTO boost = boostListingService.purchaseBoost(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(boost);
        } catch (Exception e) {
            log.error("Error purchasing boost", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get boost by ID
     * GET /api/listings/boost/{boostId}
     */
    @GetMapping("/{boostId}")
    public ResponseEntity<BoostListingDTO> getBoost(@PathVariable Long boostId) {
        try {
            BoostListingDTO boost = boostListingService.getBoostById(boostId);
            return ResponseEntity.ok(boost);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get active boosts for hotel
     * GET /api/listings/boost/hotel/{hotelId}/active
     */
    @GetMapping("/hotel/{hotelId}/active")
    public ResponseEntity<List<BoostListingDTO>> getActiveBoostsForHotel(
            @PathVariable Long hotelId) {
        List<BoostListingDTO> boosts = boostListingService.getActiveBoostsForHotel(hotelId);
        return ResponseEntity.ok(boosts);
    }

    /**
     * Get all boosts for hotel (paginated)
     * GET /api/listings/boost/hotel/{hotelId}
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<Page<BoostListingDTO>> getBoostsForHotel(
            @PathVariable Long hotelId,
            Pageable pageable) {
        Page<BoostListingDTO> boosts = boostListingService.getBoostsForHotel(hotelId, pageable);
        return ResponseEntity.ok(boosts);
    }

    /**
     * Cancel boost
     * POST /api/listings/boost/{boostId}/cancel
     */
    @PostMapping("/{boostId}/cancel")
    public ResponseEntity<BoostListingDTO> cancelBoost(
            @PathVariable Long boostId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("Cancelling boost: {}", boostId);
            BoostListingDTO boost = boostListingService.cancelBoost(boostId, reason);
            return ResponseEntity.ok(boost);
        } catch (Exception e) {
            log.error("Error cancelling boost", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Find expired boosts
     * GET /api/listings/boost/expired
     */
    @GetMapping("/expired")
    public ResponseEntity<List<BoostListingDTO>> findExpiredBoosts() {
        List<BoostListingDTO> boosts = boostListingService.findExpiredBoosts();
        return ResponseEntity.ok(boosts);
    }

    /**
     * Check if listing is boosted
     * GET /api/listings/boost/room/{roomId}/is-boosted
     */
    @GetMapping("/room/{roomId}/is-boosted")
    public ResponseEntity<Boolean> isListingBoosted(@PathVariable Long roomId) {
        boolean isBoosted = boostListingService.isListingBoosted(roomId);
        return ResponseEntity.ok(isBoosted);
    }

    /**
     * Get boost analytics
     * GET /api/listings/boost/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<List<BoostListingDTO>> getBoostAnalytics(
            @RequestParam Long hotelId) {
        List<BoostListingDTO> analytics = boostListingService.getBoostAnalytics(hotelId);
        return ResponseEntity.ok(analytics);
    }
}
