package com.travolish.traveller.pricing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoostListingRequest {
    @NotNull(message = "Hotel ID required")
    private Long hotelId;
    
    @NotNull(message = "Room ID required")
    private Long roomId;
    
    @NotNull(message = "Boost type required")
    private String boostType;
    
    @NotNull(message = "Boost tier required")
    private String boostTier;
    
    @NotNull(message = "Duration in days required")
    private Integer durationDays;
}
