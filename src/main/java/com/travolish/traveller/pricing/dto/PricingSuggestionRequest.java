package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSuggestionRequest {
    private Long hotelId;
    private Long roomId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer targetOccupancyRate;
}
