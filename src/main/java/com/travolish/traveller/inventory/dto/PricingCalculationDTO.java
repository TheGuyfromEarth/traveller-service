package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingCalculationDTO {

    private Long roomId;

    private Long hotelId;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Double basePrice;

    private Double seasonalAdjustment;

    private Double dynamicAdjustment;

    private Double promotionalDiscount;

    private Double finalPrice;

    private Double totalPrice; // Final price * number of nights

    private Integer numberOfNights;

    private String priceBreakdown;

    private String appliedRules; // Comma-separated list of applied pricing rules

    /**
     * Calculate number of nights
     */
    public Integer calculateNights() {
        if (checkInDate == null || checkOutDate == null) return 0;
        numberOfNights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return numberOfNights;
    }

    /**
     * Get detailed price breakdown
     */
    public String getDetailedBreakdown() {
        return String.format(
            "Base: %.2f, Seasonal: %.2f, Dynamic: %.2f, Discount: %.2f → Final: %.2f/night × %d nights = %.2f",
            basePrice,
            seasonalAdjustment != null ? seasonalAdjustment : 0,
            dynamicAdjustment != null ? dynamicAdjustment : 0,
            promotionalDiscount != null ? promotionalDiscount : 0,
            finalPrice,
            numberOfNights,
            totalPrice
        );
    }
}
