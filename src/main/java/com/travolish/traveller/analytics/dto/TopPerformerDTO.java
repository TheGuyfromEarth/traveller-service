package com.travolish.traveller.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopPerformerDTO {
    private Long id;
    private String name;
    private BigDecimal metric;
    private String metricLabel; // revenue, occupancy, rating
}
