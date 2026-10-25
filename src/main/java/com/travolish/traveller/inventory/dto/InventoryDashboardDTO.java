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
public class InventoryDashboardDTO {
    private Long hotelId;
    private LocalDate date;
    private Integer totalRooms;
    private Integer bookedRooms;
    private Integer availableRooms;
    private Integer blockedRooms;
    private Double occupancyPercentage;
    private Double estimatedRevenue;
    private String status; // GOOD, WARNING, CRITICAL
    private List<AvailabilityCheckDTO> roomDetails;
}
