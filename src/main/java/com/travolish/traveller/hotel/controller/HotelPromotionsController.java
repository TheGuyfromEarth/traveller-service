package com.travolish.traveller.hotel.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.inventory.dto.PricingRuleDTO;
import com.travolish.traveller.inventory.service.SeasonalPricingService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotels/{hotelId}/promotions")
@RequiredArgsConstructor
public class HotelPromotionsController {

    private final SeasonalPricingService seasonalPricingService;

    private static final Set<String> PROMO_RULE_TYPES = Set.of(
        "EARLY_BIRD", "LAST_MINUTE", "FLASH_SALE", "PROMOTIONAL",
        "LONG_STAY", "FREE_UPGRADE", "FREE_BREAKFAST", "LOYALTY"
    );

    @GetMapping
    public ResponseEntity<List<PricingRuleDTO>> getPromotions(@PathVariable Long hotelId) {
        List<PricingRuleDTO> all = seasonalPricingService.getActivePricingRulesForHotel(hotelId);
        List<PricingRuleDTO> promos = all.stream()
            .filter(r -> r.getRuleType() != null && PROMO_RULE_TYPES.contains(r.getRuleType()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(promos);
    }

    @PostMapping
    public ResponseEntity<PricingRuleDTO> savePromotion(
            @PathVariable Long hotelId,
            @RequestBody PromotionRequest req) {
        PricingRuleDTO dto = mapToRule(hotelId, req);
        PricingRuleDTO created = seasonalPricingService.createPricingRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private PricingRuleDTO mapToRule(Long hotelId, PromotionRequest req) {
        String ruleType = mapRuleType(req.getType());
        double discountPct = parseDouble(req.getDiscountPercent());

        if ("long_stay".equals(req.getType())) {
            double weekly = parseDouble(req.getWeeklyDiscountPercent());
            if (weekly > 0) discountPct = weekly;
        }

        PricingRuleDTO.PricingRuleDTOBuilder b = PricingRuleDTO.builder()
            .hotelId(hotelId)
            .ruleType(ruleType)
            .isActive(Boolean.TRUE.equals(req.getEnabled()))
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusYears(1))
            .priority(10);

        boolean isNonMonetary = "free_upgrade".equals(req.getType())
            || "free_breakfast".equals(req.getType());

        if (isNonMonetary) {
            b.pricingType("FLAT").basePrice(0.0);
            String condition = "free_upgrade".equals(req.getType())
                ? req.getUpgradeCondition() : req.getBreakfastCondition();
            b.description(condition != null && !condition.isBlank()
                ? condition : ruleType.toLowerCase().replace('_', ' '));
        } else if (discountPct > 0) {
            b.pricingType("PERCENTAGE")
             .basePrice(0.0)
             .multiplier(1.0 - (discountPct / 100.0))
             .description(buildDescription(req, discountPct));
        } else {
            b.pricingType("PERCENTAGE")
             .basePrice(0.0)
             .multiplier(1.0)
             .description(buildDescription(req, 0));
        }

        if (req.getCode() != null && !req.getCode().isBlank()) {
            b.promoCode(req.getCode());
        }

        return b.build();
    }

    private static String mapRuleType(String type) {
        if (type == null) return "PROMOTIONAL";
        return switch (type) {
            case "early_bird"     -> "EARLY_BIRD";
            case "last_minute"    -> "LAST_MINUTE";
            case "flash_sale"     -> "FLASH_SALE";
            case "coupon_code"    -> "PROMOTIONAL";
            case "long_stay"      -> "LONG_STAY";
            case "free_upgrade"   -> "FREE_UPGRADE";
            case "free_breakfast" -> "FREE_BREAKFAST";
            case "loyalty"        -> "LOYALTY";
            default               -> "PROMOTIONAL";
        };
    }

    private static String buildDescription(PromotionRequest req, double discountPct) {
        StringBuilder sb = new StringBuilder();
        if (discountPct > 0) sb.append(String.format("%.0f%% discount", discountPct));
        double advanceDays = parseDouble(req.getAdvanceDays());
        if (advanceDays > 0) sb.append(String.format(", %d days in advance", (int) advanceDays));
        double cutoffHours = parseDouble(req.getCutoffHours());
        if (cutoffHours > 0) sb.append(String.format(", %dh cutoff", (int) cutoffHours));
        double saleDuration = parseDouble(req.getSaleDurationHours());
        if (saleDuration > 0) sb.append(String.format(", %dh sale", (int) saleDuration));
        double minStays = parseDouble(req.getMinPreviousStays());
        if (minStays > 0) sb.append(String.format(", min %d prev stays", (int) minStays));
        return sb.length() > 0 ? sb.toString()
            : (req.getType() != null ? req.getType().replace('_', ' ') : "promotion");
    }

    private static double parseDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString().trim()); }
        catch (NumberFormatException ignored) { return 0.0; }
    }

    @Data
    static class PromotionRequest {
        private String type;
        private Boolean enabled;
        private Object discountPercent;
        private Object advanceDays;
        private Object cutoffHours;
        private Object saleDurationHours;
        private String code;
        private Object maxUses;
        private Object weeklyDiscountPercent;
        private Object monthlyDiscountPercent;
        private String upgradeCondition;
        private String breakfastCondition;
        private Object minPreviousStays;
    }
}
