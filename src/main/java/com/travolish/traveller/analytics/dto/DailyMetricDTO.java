package com.travolish.traveller.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyMetricDTO {
    private LocalDate date;
    private BigDecimal value;
    private String metric; // bookings, revenue, occupancy
}
