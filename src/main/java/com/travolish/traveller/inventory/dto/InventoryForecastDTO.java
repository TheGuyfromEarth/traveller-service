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
public class InventoryForecastDTO {
    private LocalDate date;
    private Integer expectedBookings;
    private Integer expectedCancellations;
    private Integer projectedOccupancy;
    private Double projectedPrice;
    private Double projectedRevenue;
    private String demandLevel;
}
