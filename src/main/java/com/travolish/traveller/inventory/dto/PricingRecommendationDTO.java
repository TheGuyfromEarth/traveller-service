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
public class PricingRecommendationDTO {
    private LocalDate date;
    private Double currentPrice;
    private Double recommendedPrice;
    private Double priceChange;
    private String reason; // HIGH_DEMAND, LOW_DEMAND, OPTIMIZE_REVENUE, MAINTAIN_OCCUPANCY
    private Double estimatedImpact; // Revenue impact percentage
}
