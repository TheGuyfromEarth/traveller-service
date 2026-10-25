package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoostListingDTO {
    private Long id;
    private Long hotelId;
    private Long roomId;
    private String boostType;
    private String boostTier;
    private BigDecimal cost;
    private Integer durationDays;
    private Integer visibilityMultiplier;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer impressionGain;
    private Integer clickGain;
    private Integer bookingGain;
}
