package com.travolish.traveller.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandAnalysisDTO {
    private String searchVolumeTrend;
    private String searchNote;
    private String bookingPace;
    private String paceNote;
    private String demandScore;
    private String scoreNote;
    private String peakWindow;
    private String peakNote;
}
