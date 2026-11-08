package com.travolish.traveller.inventory.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.inventory.dto.PricingRuleDTO;
import com.travolish.traveller.inventory.exception.InvalidPricingRuleException;
import com.travolish.traveller.inventory.model.PricingRule;
import com.travolish.traveller.inventory.model.PricingRule.PricingType;
import com.travolish.traveller.inventory.model.PricingRule.RuleType;
import com.travolish.traveller.inventory.repository.PricingRuleRepository;
import com.travolish.traveller.inventory.service.SeasonalPricingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeasonalPricingServiceImpl implements SeasonalPricingService {

    private final PricingRuleRepository pricingRuleRepository;

    @Override
    @Transactional
    public PricingRuleDTO createPricingRule(PricingRuleDTO ruleDTO) {
        validatePricingRule(ruleDTO);

        PricingRule rule = PricingRule.builder()
            .roomId(ruleDTO.getRoomId())
            .hotelId(ruleDTO.getHotelId())
            .startDate(ruleDTO.getStartDate())
            .endDate(ruleDTO.getEndDate())
            .pricingType(PricingType.valueOf(ruleDTO.getPricingType()))
            .basePrice(ruleDTO.getBasePrice())
            .adjustedPrice(ruleDTO.getAdjustedPrice())
            .multiplier(ruleDTO.getMultiplier())
            .fixedDiscount(ruleDTO.getFixedDiscount())
            .ruleType(RuleType.valueOf(ruleDTO.getRuleType()))
            .priority(ruleDTO.getPriority())
            .description(ruleDTO.getDescription())
            .season(ruleDTO.getSeason())
            .isActive(ruleDTO.getIsActive())
            .build();

        rule.calculatePrice();
        PricingRule saved = pricingRuleRepository.save(rule);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public PricingRuleDTO updatePricingRule(Long ruleId, PricingRuleDTO ruleDTO) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
            .orElseThrow(() -> InvalidPricingRuleException.ruleNotFound(ruleId));

        rule.setStartDate(ruleDTO.getStartDate());
        rule.setEndDate(ruleDTO.getEndDate());
        rule.setPricingType(PricingType.valueOf(ruleDTO.getPricingType()));
        rule.setBasePrice(ruleDTO.getBasePrice());
        rule.setMultiplier(ruleDTO.getMultiplier());
        rule.setFixedDiscount(ruleDTO.getFixedDiscount());
        rule.setPriority(ruleDTO.getPriority());
        rule.setDescription(ruleDTO.getDescription());
        if (ruleDTO.getPromoCode() != null) rule.setPromoCode(ruleDTO.getPromoCode());
        rule.setUpdatedAt(OffsetDateTime.now());

        rule.calculatePrice();
        PricingRule saved = pricingRuleRepository.save(rule);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deletePricingRule(Long ruleId) {
        pricingRuleRepository.deleteById(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRuleDTO getPricingRule(Long ruleId) {
        return pricingRuleRepository.findById(ruleId)
            .map(this::convertToDTO)
            .orElseThrow(() -> InvalidPricingRuleException.ruleNotFound(ruleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getActivePricingRulesForRoom(Long roomId) {
        return pricingRuleRepository.findByRoomIdAndIsActiveTrue(roomId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getActivePricingRulesForHotel(Long hotelId) {
        return pricingRuleRepository.findByHotelIdAndIsActiveTrue(hotelId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getApplicableRulesForDate(Long roomId, LocalDate date) {
        return pricingRuleRepository.findApplicableRulesForDate(roomId, date)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getApplicableRulesForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        
        return pricingRuleRepository.findRulesForDateRange(roomId, checkInDate, checkOutDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateFinalPrice(Long roomId, LocalDate date, Double basePrice) {
        List<PricingRule> rules = pricingRuleRepository.findApplicableRulesForDate(roomId, date);

        if (rules.isEmpty()) {
            return basePrice;
        }

        // Sort ascending by priority so the highest-priority rule is evaluated last
        // and its result is the one that takes effect (higher number = more specific/important).
        rules.sort(Comparator.comparingInt(PricingRule::getPriority));

        double finalPrice = basePrice;
        for (PricingRule rule : rules) {
            switch (rule.getPricingType()) {
                case FLAT:
                    finalPrice = rule.getBasePrice();
                    break;
                case PERCENTAGE:
                    finalPrice = basePrice * (rule.getMultiplier() != null ? rule.getMultiplier() : 1.0);
                    break;
                case DISCOUNT:
                    finalPrice = Math.max(0, basePrice - (rule.getFixedDiscount() != null ? rule.getFixedDiscount() : 0));
                    break;
            }
        }

        return finalPrice;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculatePriceForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice) {

        // Fetch ALL applicable rules for the whole stay in ONE query instead of one per night
        List<PricingRule> rangeRules =
            pricingRuleRepository.findRulesForDateRange(roomId, checkInDate, checkOutDate);
        rangeRules.sort(Comparator.comparingInt(PricingRule::getPriority));

        double totalPrice = 0;
        LocalDate current = checkInDate;
        while (current.isBefore(checkOutDate)) {
            totalPrice += applyRulesInMemory(current, basePrice, rangeRules);
            current = current.plusDays(1);
        }
        return totalPrice;
    }

    /** Apply pre-fetched, priority-sorted rules to a single date, entirely in-memory. */
    private double applyRulesInMemory(LocalDate date, double basePrice, List<PricingRule> sortedRules) {
        double price = basePrice;
        for (PricingRule rule : sortedRules) {
            if (!rule.getIsActive()) continue;
            if (date.isBefore(rule.getStartDate()) || date.isAfter(rule.getEndDate())) continue;
            price = switch (rule.getPricingType()) {
                case FLAT       -> rule.getBasePrice();
                case PERCENTAGE -> basePrice * (rule.getMultiplier() != null ? rule.getMultiplier() : 1.0);
                case DISCOUNT   -> Math.max(0, basePrice - (rule.getFixedDiscount() != null ? rule.getFixedDiscount() : 0));
            };
        }
        return price;
    }

    @Override
    @Transactional
    public void applyPricingRule(Long roomId, LocalDate startDate, LocalDate endDate,
                               String ruleType, Double value, String description) {
        
        PricingRule rule = PricingRule.builder()
            .roomId(roomId)
            .startDate(startDate)
            .endDate(endDate)
            .ruleType(RuleType.valueOf(ruleType))
            .pricingType(PricingType.PERCENTAGE)
            .basePrice(0.0)
            .multiplier(value)
            .priority(1)
            .description(description)
            .isActive(true)
            .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void createSeasonalRules(Long hotelId, String season, LocalDate startDate, LocalDate endDate) {
        // Seasonal pricing multipliers
        double multiplier = switch (season) {
            case "SUMMER" -> 1.4;     // 40% premium
            case "WINTER" -> 0.8;     // 20% discount
            case "HOLIDAY" -> 1.6;    // 60% premium
            case "SPRING" -> 1.1;     // 10% premium
            case "FALL" -> 1.0;       // Base price
            default -> 1.0;
        };

        PricingRule rule = PricingRule.builder()
            .hotelId(hotelId)
            .startDate(startDate)
            .endDate(endDate)
            .pricingType(PricingType.PERCENTAGE)
            .multiplier(multiplier)
            .ruleType(RuleType.SEASONAL)
            .season(season)
            .priority(1)
            .description(season + " season pricing")
            .isActive(true)
            .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void createEarlyBirdRules(Long roomId, LocalDate checkInDate, Integer advanceDaysRequired,
                                   Double discountPercentage) {
        
        LocalDate ruleStartDate = checkInDate.minusDays(advanceDaysRequired);

        PricingRule rule = PricingRule.builder()
            .roomId(roomId)
            .startDate(ruleStartDate)
            .endDate(checkInDate.minusDays(1))
            .pricingType(PricingType.PERCENTAGE)
            .multiplier(1.0 - (discountPercentage / 100))
            .ruleType(RuleType.EARLY_BIRD)
            .priority(2)
            .description("Early bird discount - " + advanceDaysRequired + " days in advance")
            .isActive(true)
            .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void createLastMinutePricing(Long roomId, Integer daysBeforeCheckIn, Double discountPercentage) {
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = today.plusDays(daysBeforeCheckIn);

        PricingRule rule = PricingRule.builder()
            .roomId(roomId)
            .startDate(today)
            .endDate(checkInDate.minusDays(1))
            .pricingType(PricingType.PERCENTAGE)
            .multiplier(1.0 - (discountPercentage / 100))
            .ruleType(RuleType.LAST_MINUTE)
            .priority(3)
            .description("Last minute pricing - " + daysBeforeCheckIn + " days before")
            .isActive(true)
            .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void createPromotionalRule(Long roomId, LocalDate startDate, LocalDate endDate,
                                    Double discountPercentage, String description) {
        
        PricingRule rule = PricingRule.builder()
            .roomId(roomId)
            .startDate(startDate)
            .endDate(endDate)
            .pricingType(PricingType.PERCENTAGE)
            .multiplier(1.0 - (discountPercentage / 100))
            .ruleType(RuleType.PROMOTIONAL)
            .priority(2)
            .description(description)
            .isActive(true)
            .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void togglePricingRule(Long ruleId, Boolean isActive) {
        var rule = pricingRuleRepository.findById(ruleId)
            .orElseThrow(() -> InvalidPricingRuleException.ruleNotFound(ruleId));
        
        rule.setIsActive(isActive);
        rule.setUpdatedAt(OffsetDateTime.now());
        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean hasOverlappingRules(Long roomId, LocalDate startDate, LocalDate endDate) {
        return !pricingRuleRepository.findOverlappingRules(roomId, RuleType.SEASONAL, startDate, endDate).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getOverlappingRules(Long roomId, LocalDate startDate, LocalDate endDate) {
        return pricingRuleRepository.findOverlappingRules(roomId, RuleType.SEASONAL, startDate, endDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long cleanupExpiredRules(LocalDate beforeDate) {
        return pricingRuleRepository.deleteByIsActiveFalseAndEndDateBefore(beforeDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDTO> getPricingRulesByType(String ruleType) {
        return pricingRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.valueOf(ruleType))
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateFinalPriceWithPriority(Long roomId, LocalDate date, Double basePrice) {
        var highestPriorityRule = pricingRuleRepository.findHighestPriorityRuleForDate(roomId, date);
        
        if (highestPriorityRule.isEmpty()) {
            return basePrice;
        }

        PricingRule rule = highestPriorityRule.get();
        switch (rule.getPricingType()) {
            case FLAT:
                return rule.getBasePrice();
            case PERCENTAGE:
                return basePrice * (rule.getMultiplier() != null ? rule.getMultiplier() : 1.0);
            case DISCOUNT:
                return Math.max(0, basePrice - (rule.getFixedDiscount() != null ? rule.getFixedDiscount() : 0));
            default:
                return basePrice;
        }
    }

    /**
     * Validate pricing rule
     */
    private void validatePricingRule(PricingRuleDTO ruleDTO) {
        if (ruleDTO.getStartDate().isAfter(ruleDTO.getEndDate())) {
            throw InvalidPricingRuleException.invalidDateRange();
        }

        if (ruleDTO.getBasePrice() != null && ruleDTO.getBasePrice() < 0) {
            throw InvalidPricingRuleException.invalidPrice(ruleDTO.getBasePrice());
        }

        if (ruleDTO.getMultiplier() != null && ruleDTO.getMultiplier() < 0) {
            throw InvalidPricingRuleException.invalidMultiplier(ruleDTO.getMultiplier());
        }
    }

    /**
     * Convert entity to DTO
     */
    private PricingRuleDTO convertToDTO(PricingRule rule) {
        return PricingRuleDTO.builder()
            .id(rule.getId())
            .roomId(rule.getRoomId())
            .hotelId(rule.getHotelId())
            .startDate(rule.getStartDate())
            .endDate(rule.getEndDate())
            .pricingType(rule.getPricingType().toString())
            .basePrice(rule.getBasePrice())
            .adjustedPrice(rule.getAdjustedPrice())
            .multiplier(rule.getMultiplier())
            .fixedDiscount(rule.getFixedDiscount())
            .ruleType(rule.getRuleType().toString())
            .priority(rule.getPriority())
            .description(rule.getDescription())
            .promoCode(rule.getPromoCode())
            .season(rule.getSeason())
            .isActive(rule.getIsActive())
            .createdAt(rule.getCreatedAt())
            .updatedAt(rule.getUpdatedAt())
            .build();
    }
}
