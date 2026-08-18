package com.travolish.traveller.inventory.service;

import java.time.LocalDate;
import java.util.List;

import com.travolish.traveller.inventory.dto.PricingRuleDTO;

public interface SeasonalPricingService {

    /**
     * Create new seasonal pricing rule
     */
    PricingRuleDTO createPricingRule(PricingRuleDTO ruleDTO);

    /**
     * Update existing pricing rule
     */
    PricingRuleDTO updatePricingRule(Long ruleId, PricingRuleDTO ruleDTO);

    /**
     * Delete pricing rule
     */
    void deletePricingRule(Long ruleId);

    /**
     * Get pricing rule by ID
     */
    PricingRuleDTO getPricingRule(Long ruleId);

    /**
     * Get all active pricing rules for room
     */
    List<PricingRuleDTO> getActivePricingRulesForRoom(Long roomId);

    /**
     * Get all active pricing rules for hotel
     */
    List<PricingRuleDTO> getActivePricingRulesForHotel(Long hotelId);

    /**
     * Get applicable pricing rules for specific date
     */
    List<PricingRuleDTO> getApplicableRulesForDate(Long roomId, LocalDate date);

    /**
     * Get applicable pricing rules for date range
     */
    List<PricingRuleDTO> getApplicableRulesForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate
    );

    /**
     * Calculate final price based on applicable rules
     */
    Double calculateFinalPrice(Long roomId, LocalDate date, Double basePrice);

    /**
     * Calculate price for date range with all applicable rules
     */
    Double calculatePriceForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice
    );

    /**
     * Apply pricing rule for specific date range
     */
    void applyPricingRule(Long roomId, LocalDate startDate, LocalDate endDate, 
                         String ruleType, Double value, String description);

    /**
     * Create seasonal pricing rules (e.g., SUMMER, WINTER)
     */
    void createSeasonalRules(Long hotelId, String season, LocalDate startDate, LocalDate endDate);

    /**
     * Create early bird discount rules
     */
    void createEarlyBirdRules(Long roomId, LocalDate checkInDate, Integer advanceDaysRequired, 
                             Double discountPercentage);

    /**
     * Create last minute pricing rules
     */
    void createLastMinutePricing(Long roomId, Integer daysBeforeCheckIn, Double discountPercentage);

    /**
     * Create promotional pricing rules
     */
    void createPromotionalRule(Long roomId, LocalDate startDate, LocalDate endDate, 
                              Double discountPercentage, String description);

    /**
     * Enable/disable pricing rule
     */
    void togglePricingRule(Long ruleId, Boolean isActive);

    /**
     * Check for overlapping rules
     */
    Boolean hasOverlappingRules(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Get overlapping rules
     */
    List<PricingRuleDTO> getOverlappingRules(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Clean up expired pricing rules
     */
    Long cleanupExpiredRules(LocalDate beforeDate);

    /**
     * Get pricing rules by type
     */
    List<PricingRuleDTO> getPricingRulesByType(String ruleType);

    /**
     * Apply multiple rules with priority
     */
    Double calculateFinalPriceWithPriority(Long roomId, LocalDate date, Double basePrice);
}
