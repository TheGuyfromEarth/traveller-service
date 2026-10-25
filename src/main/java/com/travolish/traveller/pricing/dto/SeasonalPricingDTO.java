package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalPricingDTO {
    private List<SeasonWindow> windows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeasonWindow {
        private String label;
        private String suggestedAdjustment;
        private String note;
    }
}
