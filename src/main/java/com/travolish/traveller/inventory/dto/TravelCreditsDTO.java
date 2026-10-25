package com.travolish.traveller.inventory.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCreditsDTO {
    private Long userId;
    private BigDecimal availableCredits;  // earned from completed bookings (2% cashback)
    private String currency;              // INR
    private Integer bookingsEarned;       // number of CONFIRMED bookings that generated credits
}
