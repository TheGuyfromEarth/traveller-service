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
public class OfferDTO {
    private Long id;
    private String title;         // human-readable offer name (from description)
    private String ruleType;      // PROMOTIONAL | EARLY_BIRD | LAST_MINUTE | LOYALTY
    private String promoCode;     // traveller-facing code (e.g. WEEKEND18)
    private String discountSummary;  // e.g. "10% off", "₹8,400 off", "18% premium"
    private Double discountPercent;  // absolute discount %, null when fixedDiscount applies
    private Double fixedDiscount;    // flat INR discount, null when multiplier applies
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Long hotelId;
    private String hotelName;
    private boolean expiringSoon; // true if validUntil ≤ 30 days from today
    private boolean active;
}
