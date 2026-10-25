package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyReportDTO {
    private Long hotelId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double averageOccupancy;
    private Double peakOccupancy;
    private Double lowOccupancy;
    private Integer totalBookings;
    private Integer totalCancellations;
    private Double cancellationRate;
    private List<SegmentDTO> segments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SegmentDTO {
        private String segmentName;
        private Double percentage;
        private String trend;
    }
}
