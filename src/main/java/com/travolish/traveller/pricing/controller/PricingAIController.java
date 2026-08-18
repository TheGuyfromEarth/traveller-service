package com.travolish.traveller.pricing.controller;

import com.travolish.traveller.pricing.dto.CompetitorAnalysisDTO;
import com.travolish.traveller.pricing.dto.DemandAnalysisDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.dto.SeasonalPricingDTO;
import com.travolish.traveller.pricing.service.PricingAIService;
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
@RequestMapping("/api/pricing/suggestions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PricingAIController {

    private final PricingAIService pricingAIService;

    @PostMapping("/generate")
    public ResponseEntity<List<PricingSuggestionDTO>> generateSuggestion(
            @Valid @RequestBody PricingSuggestionRequest request) {
        try {
            if (request.getRoomId() != null) {
                log.info("Generating pricing suggestion for room: {}", request.getRoomId());
                PricingSuggestionDTO suggestion = pricingAIService.generateSuggestion(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(List.of(suggestion));
            }
            log.info("Generating pricing suggestions for hotel: {}", request.getHotelId());
            List<PricingSuggestionDTO> suggestions = pricingAIService.generateSuggestionsForHotel(request.getHotelId());
            return ResponseEntity.status(HttpStatus.CREATED).body(suggestions);
        } catch (Exception e) {
            log.error("Error generating pricing suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<PricingSuggestionDTO>> getSuggestionsForHotel(
            @PathVariable Long hotelId) {
        return ResponseEntity.ok(pricingAIService.getSuggestionsForHotel(hotelId));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PricingSuggestionDTO>> getSuggestionsForRoom(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(pricingAIService.getSuggestionsForRoom(roomId));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<PricingSuggestionDTO>> getPendingSuggestions(
            @RequestParam Long hotelId,
            Pageable pageable) {
        return ResponseEntity.ok(pricingAIService.getPendingSuggestionsForHotel(hotelId, pageable));
    }

    @PostMapping("/{suggestionId}/accept")
    public ResponseEntity<PricingSuggestionDTO> acceptSuggestion(
            @PathVariable Long suggestionId) {
        try {
            log.info("Accepting pricing suggestion: {}", suggestionId);
            return ResponseEntity.ok(pricingAIService.acceptSuggestion(suggestionId));
        } catch (RuntimeException e) {
            log.error("Error accepting suggestion {}: {}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error accepting suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{suggestionId}/reject")
    public ResponseEntity<PricingSuggestionDTO> rejectSuggestion(
            @PathVariable Long suggestionId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("Rejecting pricing suggestion: {}", suggestionId);
            return ResponseEntity.ok(pricingAIService.rejectSuggestion(suggestionId, reason));
        } catch (RuntimeException e) {
            log.error("Error rejecting suggestion {}: {}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error rejecting suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/analyze/demand")
    public ResponseEntity<DemandAnalysisDTO> analyzeDemandTrends(
            @RequestParam Long hotelId) {
        try {
            return ResponseEntity.ok(pricingAIService.analyzeDemandTrends(hotelId));
        } catch (Exception e) {
            log.error("Error analyzing demand trends for hotel {}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/analyze/competitors")
    public ResponseEntity<CompetitorAnalysisDTO> analyzeCompetitorPricing(
            @RequestParam Long hotelId) {
        try {
            return ResponseEntity.ok(pricingAIService.analyzeCompetitorPricing(hotelId));
        } catch (Exception e) {
            log.error("Error analyzing competitor pricing for hotel {}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/seasonal")
    public ResponseEntity<SeasonalPricingDTO> generateSeasonalSuggestions(
            @RequestParam Long hotelId) {
        try {
            return ResponseEntity.ok(pricingAIService.generateSeasonalPricingSuggestions(hotelId));
        } catch (Exception e) {
            log.error("Error generating seasonal suggestions for hotel {}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
