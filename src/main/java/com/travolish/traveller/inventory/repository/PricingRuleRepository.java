package com.travolish.traveller.inventory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.inventory.model.PricingRule;
import com.travolish.traveller.inventory.model.PricingRule.RuleType;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    /**
     * Find all active pricing rules for a room
     */
    List<PricingRule> findByRoomIdAndIsActiveTrue(Long roomId);

    /**
     * Find all active pricing rules for a hotel
     */
    List<PricingRule> findByHotelIdAndIsActiveTrue(Long hotelId);

    /**
     * Find pricing rules applicable for a specific date
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.startDate <= :date AND pr.endDate >= :date AND pr.isActive = true " +
           "ORDER BY pr.priority DESC")
    List<PricingRule> findApplicableRulesForDate(
        @Param("roomId") Long roomId,
        @Param("date") LocalDate date
    );

    /**
     * Find pricing rules for date range (overlapping rules)
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.startDate <= :endDate AND pr.endDate >= :startDate AND pr.isActive = true " +
           "ORDER BY pr.priority DESC")
    List<PricingRule> findRulesForDateRange(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find pricing rules by type
     */
    List<PricingRule> findByRuleTypeAndIsActiveTrue(RuleType ruleType);

    /**
     * Find seasonal pricing rules for a specific season
     */
    List<PricingRule> findBySeasonAndRuleTypeAndIsActiveTrue(String season, RuleType ruleType);

    /**
     * Find all pricing rules for a hotel and rule type
     */
    List<PricingRule> findByHotelIdAndRuleTypeAndIsActiveTrue(Long hotelId, RuleType ruleType);

    /**
     * Find overlapping rules for conflict detection
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.ruleType = :ruleType AND pr.isActive = true " +
           "AND pr.startDate <= :endDate AND pr.endDate >= :startDate")
    List<PricingRule> findOverlappingRules(
        @Param("roomId") Long roomId,
        @Param("ruleType") RuleType ruleType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find highest priority rule for a date
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.startDate <= :date AND pr.endDate >= :date AND pr.isActive = true " +
           "ORDER BY pr.priority DESC LIMIT 1")
    Optional<PricingRule> findHighestPriorityRuleForDate(
        @Param("roomId") Long roomId,
        @Param("date") LocalDate date
    );

    /**
     * Find early bird pricing rules
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.ruleType = com.travolish.traveller.inventory.model.PricingRule$RuleType.EARLY_BIRD " +
           "AND pr.isActive = true AND pr.startDate <= :bookingDate")
    List<PricingRule> findEarlyBirdRules(
        @Param("roomId") Long roomId,
        @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * Find promotional pricing rules currently active
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.ruleType = com.travolish.traveller.inventory.model.PricingRule$RuleType.PROMOTIONAL " +
           "AND pr.isActive = true AND pr.startDate <= CURRENT_DATE AND pr.endDate >= CURRENT_DATE")
    List<PricingRule> findActivePromotionalRules();

    /**
     * Find all pricing rules for a hotel with pagination
     */
    List<PricingRule> findByHotelId(Long hotelId);

    /**
     * Find inactive rules for cleanup/archiving
     */
    List<PricingRule> findByIsActiveFalseAndEndDateBefore(LocalDate date);

    /**
     * Delete expired rules
     */
    Long deleteByIsActiveFalseAndEndDateBefore(LocalDate date);

    /**
     * Get average multiplier for hotel room in date range
     */
    @Query("SELECT AVG(pr.multiplier) FROM PricingRule pr WHERE pr.roomId = :roomId " +
           "AND pr.startDate <= :endDate AND pr.endDate >= :startDate AND pr.isActive = true")
    Optional<Double> getAverageMultiplierForDateRange(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
