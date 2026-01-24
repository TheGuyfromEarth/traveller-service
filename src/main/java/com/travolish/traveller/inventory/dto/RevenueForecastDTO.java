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
public class RevenueForecastDTO {
    private Long hotelId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalRevenue;
    private Double averageDailyRevenue;
    private Double estimatedRevenue;
    private Double revenueTrend; // Percentage change
    private List<InventoryForecastDTO> dailyForecasts;
}
