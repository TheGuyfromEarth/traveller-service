package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSuggestionDTO {
    private Long id;
    private Long hotelId;
    private Long roomId;
    private BigDecimal suggestedPrice;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private Double confidenceScore;
    private String reason;
    private String trend;
    private LocalDate suggestedFromDate;
    private LocalDate suggestedToDate;
    private String analysis;
    private String status;
    private Integer occupancyRate;
    private Integer competitorAvgPrice;
    private Integer demandLevel;
    private LocalDateTime createdAt;
    private String roomName;
}
