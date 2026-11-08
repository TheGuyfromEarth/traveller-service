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
public class InventoryAlertDTO {
    private Long roomId;
    private LocalDate date;
    private String alertType; // LOW_STOCK, OVERBOOKED, MAINTENANCE_NEEDED, HIGH_DEMAND, PRICE_ANOMALY
    private String message;
    private String severity; // INFO, WARNING, CRITICAL
}
