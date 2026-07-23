package com.travolish.traveller.admin.controller;

import com.travolish.traveller.admin.dto.AdminDashboardStatsDTO;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.HotelChangeRequest;
import com.travolish.traveller.hotel.repository.HotelChangeRequestRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        long pendingChangeRequests = hotelChangeRequestRepository
                .findByStatus(HotelChangeRequest.RequestStatus.PENDING).size();
        long pendingListingApprovals = hotelRepository
                .findAll().stream()
                .filter(h -> com.travolish.traveller.hotel.model.Hotel.HotelStatus.PENDING_REVIEW.name()
                        .equals(h.getStatus() != null ? h.getStatus().name() : ""))
                .count();
        long pendingRequests = pendingChangeRequests + pendingListingApprovals;

        long flaggedReviews = reviewRepository
                .countByReviewStatus(Review.ReviewStatus.FLAGGED);

        long pendingKYC = hostKYCRepository.findPendingVerifications().size();

        List<Booking> allBookings = bookingRepository.findAll();
        long totalBookings = allBookings.size();
        long confirmedBookings = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .count();

        List<AdminDashboardStatsDTO.DayTrendDTO> trend = buildWeekTrend(allBookings);

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

        List<HotelChangeRequest> processed = new ArrayList<>();
        processed.addAll(hotelChangeRequestRepository.findByStatus(HotelChangeRequest.RequestStatus.APPROVED));
        processed.addAll(hotelChangeRequestRepository.findByStatus(HotelChangeRequest.RequestStatus.REJECTED));
        for (HotelChangeRequest req : processed) {
            if (req.getProcessedAt() == null) continue;
            String title = req.getStatus() == HotelChangeRequest.RequestStatus.APPROVED
                    ? "Listing approved" : "Listing rejected";
            String meta = req.getName() != null ? req.getName() : "Hotel #" + req.getHotelId();
            entries.add(new Entry(title, meta, req.getProcessedAt().toInstant()));
        }

        List<com.travolish.traveller.kyc.entity.HostKYC> kycRecords = new ArrayList<>();
        kycRecords.addAll(hostKYCRepository.findByKYCStatus("VERIFIED"));
        kycRecords.addAll(hostKYCRepository.findByKYCStatus("REJECTED"));
        for (com.travolish.traveller.kyc.entity.HostKYC kyc : kycRecords) {
            if (kyc.getUpdatedAt() == null) continue;
            String title = "VERIFIED".equals(kyc.getKycStatus()) ? "KYC verified" : "KYC rejected";
            String first = kyc.getFirstName() != null ? kyc.getFirstName() : "";
            String last = kyc.getLastName() != null ? kyc.getLastName() : "";
            String name = (first + " " + last).trim();
            String meta = name.isEmpty() ? "Host #" + kyc.getHostId() : name;
            entries.add(new Entry(title, meta, kyc.getUpdatedAt().toInstant(ZoneOffset.UTC)));
        }

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

    private List<AdminDashboardStatsDTO.DayTrendDTO> buildWeekTrend(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        Map<LocalDate, List<Booking>> byDay = bookings.stream()
                .filter(b -> b.getCheckInDate() != null
                        && !b.getCheckInDate().isBefore(weekStart)
                        && !b.getCheckInDate().isAfter(today))
                .collect(Collectors.groupingBy(Booking::getCheckInDate));

        List<AdminDashboardStatsDTO.DayTrendDTO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<Booking> dayBookings = byDay.getOrDefault(date, List.of());
            long count = dayBookings.size();
            double revenue = dayBookings.stream()
                    .mapToDouble(b -> b.getTotalPrice() != null ? b.getTotalPrice() : 0)
                    .sum();
            String label = DAY_LABELS[date.getDayOfWeek().getValue() - 1];
            trend.add(AdminDashboardStatsDTO.DayTrendDTO.builder()
                    .label(label)
                    .bookings(count)
                    .revenue(revenue)
                    .build());
        }
        return trend;
    }
}
