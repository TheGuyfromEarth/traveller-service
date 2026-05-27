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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
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

        long pendingRequests = hotelChangeRequestRepository
                .findByStatus(HotelChangeRequest.RequestStatus.PENDING).size();

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
                .build();

        return ResponseEntity.ok(stats);
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
            String label = DAY_LABELS[date.getDayOfWeek().getValue() % 7];
            trend.add(AdminDashboardStatsDTO.DayTrendDTO.builder()
                    .label(label)
                    .bookings(count)
                    .revenue(revenue)
                    .build());
        }
        return trend;
    }
}
