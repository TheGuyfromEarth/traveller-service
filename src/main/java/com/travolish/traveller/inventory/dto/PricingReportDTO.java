package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingReportDTO {
    private Long hotelId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double averagePrice;
    private Double minPrice;
    private Double maxPrice;
    private Integer rulesApplied;
    private Double estimatedRevenue;
    private List<PricingRuleDTO> activeRules;
}
