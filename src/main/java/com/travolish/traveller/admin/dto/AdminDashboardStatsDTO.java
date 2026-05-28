package com.travolish.traveller.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsDTO {

    private long totalUsers;
    private long totalHotels;
    private long pendingHotelRequests;
    private long flaggedReviews;
    private long pendingKYC;
    private long totalBookings;
    private long confirmedBookings;

    private List<DayTrendDTO> bookingTrend;
    private List<ActivityItemDTO> recentActivity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayTrendDTO {
        private String label;
        private long bookings;
        private double revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItemDTO {
        private String title;
        private String meta;
        private String time;
    }
}
