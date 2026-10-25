package com.travolish.traveller.admin.controller;

import com.travolish.traveller.admin.dto.AdminDashboardStatsDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.HotelChangeRequest;
import com.travolish.traveller.hotel.repository.HotelChangeRequestRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final HotelChangeRequestRepository hotelChangeRequestRepository;
    private final HostKYCRepository hostKYCRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getStats() {
        long totalUsers = userRepository.count();
        long totalHotels = hotelRepository.count();

        long pendingChangeRequests = hotelChangeRequestRepository.countByStatus(HotelChangeRequest.RequestStatus.PENDING);
        long pendingListingApprovals = hotelRepository.countByStatus(Hotel.HotelStatus.PENDING_REVIEW);
        long pendingRequests = pendingChangeRequests + pendingListingApprovals;

        long flaggedReviews = reviewRepository.countByReviewStatus(Review.ReviewStatus.FLAGGED);
        long pendingKYC = hostKYCRepository.countByKycStatus("PENDING");

        long totalBookings = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus(Booking.BookingStatus.CONFIRMED);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<Object[]> trendRaw = bookingRepository.countAndRevenueByCheckInDateBetween(weekStart, today);
        List<AdminDashboardStatsDTO.DayTrendDTO> trend = buildWeekTrend(trendRaw, today);

        AdminDashboardStatsDTO stats = AdminDashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalHotels(totalHotels)
                .pendingHotelRequests(pendingRequests)
                .flaggedReviews(flaggedReviews)
                .pendingKYC(pendingKYC)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .bookingTrend(trend)
                .recentActivity(buildRecentActivity())
                .build();

        return ResponseEntity.ok(stats);
    }

    private List<AdminDashboardStatsDTO.ActivityItemDTO> buildRecentActivity() {
        record Entry(String title, String meta, Instant at) {}
        List<Entry> entries = new ArrayList<>();

        hotelChangeRequestRepository.findRecentProcessed(
                List.of(HotelChangeRequest.RequestStatus.APPROVED, HotelChangeRequest.RequestStatus.REJECTED),
                PageRequest.of(0, 8)
        ).forEach(req -> {
            String title = req.getStatus() == HotelChangeRequest.RequestStatus.APPROVED
                    ? "Listing approved" : "Listing rejected";
            String meta = req.getName() != null ? req.getName() : "Hotel #" + req.getHotelId();
            entries.add(new Entry(title, meta, req.getProcessedAt().toInstant()));
        });

        hostKYCRepository.findRecentByStatuses(
                List.of("VERIFIED", "REJECTED"),
                PageRequest.of(0, 8)
        ).forEach(kyc -> {
            String title = "VERIFIED".equals(kyc.getKycStatus()) ? "KYC verified" : "KYC rejected";
            String first = kyc.getFirstName() != null ? kyc.getFirstName() : "";
            String last = kyc.getLastName() != null ? kyc.getLastName() : "";
            String name = (first + " " + last).trim();
            String meta = name.isEmpty() ? "Host #" + kyc.getHostId() : name;
            entries.add(new Entry(title, meta, kyc.getUpdatedAt().toInstant(ZoneOffset.UTC)));
        });

        Instant now = Instant.now();
        return entries.stream()
                .sorted(Comparator.comparing(Entry::at).reversed())
                .limit(8)
                .map(e -> AdminDashboardStatsDTO.ActivityItemDTO.builder()
                        .title(e.title())
                        .meta(e.meta())
                        .time(formatTimeAgo(e.at(), now))
                        .build())
                .collect(Collectors.toList());
    }

    private static String formatTimeAgo(Instant then, Instant now) {
        long minutes = ChronoUnit.MINUTES.between(then, now);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = ChronoUnit.HOURS.between(then, now);
        if (hours < 24) return hours + " hr ago";
        long days = ChronoUnit.DAYS.between(then, now);
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    private List<AdminDashboardStatsDTO.DayTrendDTO> buildWeekTrend(List<Object[]> rows, LocalDate today) {
        record DayStats(long count, double revenue) {}

        Map<LocalDate, DayStats> byDay = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            long count = ((Number) row[1]).longValue();
            double revenue = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            byDay.put(date, new DayStats(count, revenue));
        }

        List<AdminDashboardStatsDTO.DayTrendDTO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DayStats stats = byDay.getOrDefault(date, new DayStats(0, 0.0));
            String label = DAY_LABELS[date.getDayOfWeek().getValue() - 1];
            trend.add(AdminDashboardStatsDTO.DayTrendDTO.builder()
                    .label(label)
                    .bookings(stats.count())
                    .revenue(stats.revenue())
                    .build());
        }
        return trend;
    }
}
