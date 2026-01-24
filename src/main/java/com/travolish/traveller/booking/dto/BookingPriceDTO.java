package com.travolish.traveller.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for booking price calculation breakdown.
 * Shows the detailed price components for a booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPriceDTO {

    /**
     * Base price per night
     */
    private Double basePrice;

    /**
     * Number of nights in the booking
     */
    private Integer numberOfNights;

    /**
     * Base price total (basePrice * numberOfNights)
     */
    private Double basePriceTotal;

    /**
     * Seasonal adjustment amount
     */
    private Double seasonalAdjustment;

    /**
     * Dynamic pricing adjustment amount
     */
    private Double dynamicPricingAdjustment;

    /**
     * Promotional discount amount
     */
    private Double promotionalDiscount;

    /**
     * Final total price for the booking
     */
    private Double totalPrice;

    /**
     * Get a summary string of the price breakdown
     */
    public String getPriceSummary() {
        return String.format(
            "Base: $%.2f/night × %d nights = $%.2f | " +
            "Seasonal: %+.2f | Dynamic: %+.2f | Promo: %+.2f | " +
            "Total: $%.2f",
            basePrice, numberOfNights, basePriceTotal,
            seasonalAdjustment, dynamicPricingAdjustment, promotionalDiscount,
            totalPrice
        );
    }
}
