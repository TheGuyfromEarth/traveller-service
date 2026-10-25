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
    private Double estimatedRevenue;   // RevPAR (revenue per available room night)
    private Double revenueTrend;       // kept for compatibility — same value as revenueGrowth
    private Double revenueGrowth;      // % change in total revenue vs prior period
    private Double adrChange;          // % change in ADR vs prior period
    private Double revParChange;       // % change in RevPAR vs prior period
    private List<InventoryForecastDTO> dailyForecasts;
}
