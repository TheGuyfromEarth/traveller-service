package com.travolish.traveller.inventory.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "pricing_rules", indexes = {
    @Index(name = "idx_pricing_room_date", columnList = "room_id, start_date, end_date"),
    @Index(name = "idx_pricing_hotel_date", columnList = "hotel_id, start_date, end_date"),
    @Index(name = "idx_pricing_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id") // nullable — null means hotel-wide rule
    private Long roomId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingType pricingType;

    @Min(0)
    @Column(nullable = false)
    private Double basePrice;

    @Min(0)
    private Double adjustedPrice; // Final calculated price

    @Builder.Default
    private Double multiplier = 1.0; // For percentage-based adjustments

    @Min(0)
    private Double fixedDiscount; // Flat discount amount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RuleType ruleType = RuleType.SEASONAL; // SEASONAL, PROMOTIONAL, DYNAMIC, EARLY_BIRD

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1; // Higher priority applied first (1=highest)

    private String description;

    /** Traveller-facing promo code (e.g. WEEKEND18). Auto-generated from description when null. */
    private String promoCode;

    /** §21 — Display label shown to traveller (e.g. "Free Breakfast Included", "Flash Sale – 30% Off") */
    private String promoLabel;

    /** §21 — Whether this is a non-monetary perk (FREE_UPGRADE, FREE_BREAKFAST) vs a price discount */
    @Builder.Default
    private Boolean nonMonetary = false;

    private String season; // e.g., "SUMMER", "WINTER", "HOLIDAY"

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum PricingType {
        FLAT,           // Fixed price
        PERCENTAGE,     // Percentage multiplier (e.g., 1.5 = 150%)
        DISCOUNT        // Discount from base price
    }

    public enum RuleType {
        SEASONAL,       // Based on time of year
        PROMOTIONAL,    // Special promotions
        DYNAMIC,        // Based on demand/occupancy
        EARLY_BIRD,     // Early booking discounts — §21
        LAST_MINUTE,    // Last-minute deals — §21
        BULK,           // Group/bulk booking discounts
        LOYALTY,        // Loyalty program pricing — §21
        FLASH_SALE,     // Time-limited flash sale — §21
        LONG_STAY,      // Weekly / monthly discount — §21
        FREE_UPGRADE,   // Non-monetary: complimentary room upgrade — §21
        FREE_BREAKFAST  // Non-monetary: complimentary breakfast included — §21
    }

    /**
     * Calculate the price based on rule type and parameters
     */
    public Double calculatePrice() {
        if (adjustedPrice != null) {
            return adjustedPrice;
        }

        switch (pricingType) {
            case FLAT:
                adjustedPrice = basePrice;
                break;
            case PERCENTAGE:
                adjustedPrice = basePrice * multiplier;
                break;
            case DISCOUNT:
                adjustedPrice = Math.max(0, basePrice - (fixedDiscount != null ? fixedDiscount : 0));
                break;
            default:
                adjustedPrice = basePrice;
        }

        return adjustedPrice;
    }

    /**
     * Check if this rule applies to a given date
     */
    public Boolean appliesToDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate) && isActive;
    }

    /**
     * Check if this rule overlaps with another date range
     */
    public Boolean overlapsWithRange(LocalDate checkIn, LocalDate checkOut) {
        return !endDate.isBefore(checkIn) && !startDate.isAfter(checkOut.minusDays(1));
    }
}
