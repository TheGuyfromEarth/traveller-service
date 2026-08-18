package com.travolish.traveller.inventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.inventory.dto.PricingRuleDTO;
import com.travolish.traveller.inventory.service.SeasonalPricingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory/pricing")
@RequiredArgsConstructor
public class PricingManagementController {

    private final SeasonalPricingService seasonalPricingService;

    /**
     * Create new pricing rule
     */
    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PricingRuleDTO> createPricingRule(@RequestBody PricingRuleDTO ruleDTO) {
        PricingRuleDTO created = seasonalPricingService.createPricingRule(ruleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update pricing rule
     */
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<PricingRuleDTO> updatePricingRule(
        @PathVariable Long ruleId,
        @RequestBody PricingRuleDTO ruleDTO) {
        
        PricingRuleDTO updated = seasonalPricingService.updatePricingRule(ruleId, ruleDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete pricing rule
     */
    @DeleteMapping("/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePricingRule(@PathVariable Long ruleId) {
        seasonalPricingService.deletePricingRule(ruleId);
    }

    /**
     * Clone a pricing rule — creates a Draft copy with the same parameters
     */
    @PostMapping("/rules/{ruleId}/clone")
    public ResponseEntity<PricingRuleDTO> clonePricingRule(@PathVariable Long ruleId) {
        PricingRuleDTO original = seasonalPricingService.getPricingRule(ruleId);
        PricingRuleDTO clone = PricingRuleDTO.builder()
            .roomId(original.getRoomId())
            .hotelId(original.getHotelId())
            .startDate(original.getStartDate())
            .endDate(original.getEndDate())
            .pricingType(original.getPricingType())
            .basePrice(original.getBasePrice())
            .adjustedPrice(original.getAdjustedPrice())
            .multiplier(original.getMultiplier())
            .fixedDiscount(original.getFixedDiscount())
            .ruleType(original.getRuleType())
            .priority(original.getPriority())
            .description("Copy of " + (original.getDescription() != null ? original.getDescription() : "Rule #" + ruleId))
            .season(original.getSeason())
            .isActive(false)
            .build();
        PricingRuleDTO created = seasonalPricingService.createPricingRule(clone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get pricing rule by ID
     */
    @GetMapping("/rules/{ruleId}")
    public ResponseEntity<PricingRuleDTO> getPricingRule(@PathVariable Long ruleId) {
        PricingRuleDTO rule = seasonalPricingService.getPricingRule(ruleId);
        return ResponseEntity.ok(rule);
    }

    /**
     * Get all active pricing rules for room
     */
    @GetMapping("/rules/room/{roomId}")
    public ResponseEntity<List<PricingRuleDTO>> getActivePricingRulesForRoom(@PathVariable Long roomId) {
        List<PricingRuleDTO> rules = seasonalPricingService.getActivePricingRulesForRoom(roomId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get all active pricing rules for hotel
     */
    @GetMapping("/rules/hotel/{hotelId}")
    public ResponseEntity<List<PricingRuleDTO>> getActivePricingRulesForHotel(@PathVariable Long hotelId) {
        List<PricingRuleDTO> rules = seasonalPricingService.getActivePricingRulesForHotel(hotelId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get applicable rules for specific date
     */
    @GetMapping("/rules/room/{roomId}/date")
    public ResponseEntity<List<PricingRuleDTO>> getApplicableRulesForDate(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<PricingRuleDTO> rules = seasonalPricingService.getApplicableRulesForDate(roomId, date);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get applicable rules for date range
     */
    @GetMapping("/rules/room/{roomId}/range")
    public ResponseEntity<List<PricingRuleDTO>> getApplicableRulesForDateRange(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        List<PricingRuleDTO> rules = seasonalPricingService
            .getApplicableRulesForDateRange(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(rules);
    }

    /**
     * Calculate final price for specific date
     */
    @GetMapping("/calculate/date")
    public ResponseEntity<Double> calculateFinalPrice(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Double basePrice) {
        
        Double finalPrice = seasonalPricingService.calculateFinalPrice(roomId, date, basePrice);
        return ResponseEntity.ok(finalPrice);
    }

    /**
     * Calculate price for date range
     */
    @GetMapping("/calculate/range")
    public ResponseEntity<Double> calculatePriceForDateRange(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
        @RequestParam Double basePrice) {
        
        Double totalPrice = seasonalPricingService
            .calculatePriceForDateRange(roomId, checkInDate, checkOutDate, basePrice);
        return ResponseEntity.ok(totalPrice);
    }

    /**
     * Create seasonal pricing rules
     */
    @PostMapping("/seasonal")
    @ResponseStatus(HttpStatus.CREATED)
    public void createSeasonalRules(
        @RequestParam Long hotelId,
        @RequestParam String season,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        seasonalPricingService.createSeasonalRules(hotelId, season, startDate, endDate);
    }

    /**
     * Create early bird discount rules
     */
    @PostMapping("/early-bird")
    @ResponseStatus(HttpStatus.CREATED)
    public void createEarlyBirdRules(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam Integer advanceDaysRequired,
        @RequestParam Double discountPercentage) {
        
        seasonalPricingService.createEarlyBirdRules(roomId, checkInDate, advanceDaysRequired, discountPercentage);
    }

    /**
     * Create last minute pricing rules
     */
    @PostMapping("/last-minute")
    @ResponseStatus(HttpStatus.CREATED)
    public void createLastMinutePricing(
        @RequestParam Long roomId,
        @RequestParam Integer daysBeforeCheckIn,
        @RequestParam Double discountPercentage) {
        
        seasonalPricingService.createLastMinutePricing(roomId, daysBeforeCheckIn, discountPercentage);
    }

    /**
     * Create promotional pricing rules
     */
    @PostMapping("/promotional")
    @ResponseStatus(HttpStatus.CREATED)
    public void createPromotionalRule(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam Double discountPercentage,
        @RequestParam String description) {
        
        seasonalPricingService.createPromotionalRule(roomId, startDate, endDate, discountPercentage, description);
    }

    /**
     * Toggle pricing rule active/inactive
     */
    @PutMapping("/rules/{ruleId}/toggle")
    @ResponseStatus(HttpStatus.OK)
    public void togglePricingRule(
        @PathVariable Long ruleId,
        @RequestParam Boolean isActive) {
        
        seasonalPricingService.togglePricingRule(ruleId, isActive);
    }

    /**
     * Check for overlapping rules
     */
    @GetMapping("/rules/overlap")
    public ResponseEntity<Boolean> hasOverlappingRules(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Boolean hasOverlap = seasonalPricingService.hasOverlappingRules(roomId, startDate, endDate);
        return ResponseEntity.ok(hasOverlap);
    }

    /**
     * Get overlapping rules
     */
    @GetMapping("/rules/overlapping")
    public ResponseEntity<List<PricingRuleDTO>> getOverlappingRules(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PricingRuleDTO> rules = seasonalPricingService.getOverlappingRules(roomId, startDate, endDate);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get pricing rules by type
     */
    @GetMapping("/rules/type/{ruleType}")
    public ResponseEntity<List<PricingRuleDTO>> getPricingRulesByType(@PathVariable String ruleType) {
        List<PricingRuleDTO> rules = seasonalPricingService.getPricingRulesByType(ruleType);
        return ResponseEntity.ok(rules);
    }

    /**
     * Calculate price with priority-based rules
     */
    @GetMapping("/calculate/priority")
    public ResponseEntity<Double> calculateFinalPriceWithPriority(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Double basePrice) {
        
        Double finalPrice = seasonalPricingService.calculateFinalPriceWithPriority(roomId, date, basePrice);
        return ResponseEntity.ok(finalPrice);
    }
}
