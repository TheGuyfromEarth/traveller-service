package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorAnalysisDTO {
    private String avgRate;
    private String rateNote;
    private String yourPosition;
    private String positionNote;
    private String competitorOccupancy;
    private String occupancyNote;
    private String priceGap;
    private String gapNote;
}
