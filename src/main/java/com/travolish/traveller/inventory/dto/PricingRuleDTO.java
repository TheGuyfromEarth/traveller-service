package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleDTO {

    private Long id;

    private Long roomId;

    private Long hotelId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String pricingType; // FLAT, PERCENTAGE, DISCOUNT

    private Double basePrice;

    private Double adjustedPrice;

    private Double multiplier;

    private Double fixedDiscount;

    private String ruleType; // SEASONAL, PROMOTIONAL, DYNAMIC, EARLY_BIRD, LAST_MINUTE, BULK, LOYALTY

    private Integer priority;

    private String description;

    private String promoCode;

    private String season;

    private Boolean isActive;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    /**
     * Calculate price based on rule configuration
     */
    public Double calculatePrice() {
        if (adjustedPrice != null) {
            return adjustedPrice;
        }

        switch (pricingType) {
            case "FLAT":
                return basePrice;
            case "PERCENTAGE":
                return basePrice * (multiplier != null ? multiplier : 1.0);
            case "DISCOUNT":
                return Math.max(0, basePrice - (fixedDiscount != null ? fixedDiscount : 0));
            default:
                return basePrice;
        }
    }

    /**
     * Get rule summary
     */
    public String getSummary() {
        return String.format(
            "%s Rule: %s (%.2f) from %s to %s",
            ruleType, pricingType, calculatePrice(), startDate, endDate
        );
    }
}
