package com.travolish.traveller.payout.controller;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides host tax documents computed from live booking and payout data.
 * Documents are generated on-demand; no static PDF is stored.
 */
@RestController
@RequestMapping("/api/payouts/tax")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaxDocumentController {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    private static final double PLATFORM_COMMISSION_RATE = 0.12; // 12%

    /**
     * List tax document summaries for a host for a given year.
     * GET /api/payouts/tax/documents?hostId=17&year=2026
     */
    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> getTaxDocuments(
            @RequestParam Long hostId,
            @RequestParam(required = false) Integer year) {

        int targetYear = year != null ? year : LocalDate.now().getYear();
        log.info("Fetching tax documents for host {} year {}", hostId, targetYear);

        TaxSummary summary = computeTaxSummary(hostId, targetYear);

        List<Map<String, Object>> docs = new ArrayList<>();

        // Annual earnings summary
        Map<String, Object> earningsSummary = new LinkedHashMap<>();
        earningsSummary.put("id", "earnings-" + targetYear);
        earningsSummary.put("title", targetYear + " Earnings Summary");
        earningsSummary.put("type", "EARNINGS_SUMMARY");
        earningsSummary.put("year", targetYear);
        earningsSummary.put("status", summary.totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? "READY" : "NO_ACTIVITY");
        earningsSummary.put("grossRevenue", summary.totalRevenue);
        earningsSummary.put("platformCommission", summary.commission);
        earningsSummary.put("netEarnings", summary.netEarnings);
        earningsSummary.put("completedBookings", summary.completedBookings);
        earningsSummary.put("note", "Downloadable after year-end reconciliation");
        docs.add(earningsSummary);

        // Form 1099-K equivalent (for INR: Form 26AS / ITR reporting)
        Map<String, Object> form1099 = new LinkedHashMap<>();
        form1099.put("id", "1099k-" + targetYear);
        form1099.put("title", targetYear + " Form 1099-K / Platform Earnings Statement");
        form1099.put("type", "FORM_1099K");
        form1099.put("year", targetYear);
        boolean thresholdMet = summary.totalRevenue.compareTo(BigDecimal.valueOf(20000)) >= 0
                            || summary.completedBookings >= 200;
        form1099.put("status", thresholdMet ? "READY" : "THRESHOLD_NOT_MET");
        form1099.put("grossRevenue", summary.totalRevenue);
        form1099.put("transactions", summary.completedBookings);
        form1099.put("note", thresholdMet
            ? "Available for download"
            : "Form issued when gross revenue ≥ ₹20,000 or 200+ transactions per year");
        docs.add(form1099);

        // Tax profile
        Map<String, Object> taxProfile = new LinkedHashMap<>();
        taxProfile.put("id", "tax-profile-" + hostId);
        taxProfile.put("title", "Tax Profile");
        taxProfile.put("type", "TAX_PROFILE");
        taxProfile.put("status", "NEEDS_REVIEW");
        taxProfile.put("note", "Confirm legal name, PAN/GSTIN, and registered address for accurate tax filings");
        docs.add(taxProfile);

        return ResponseEntity.ok(docs);
    }

    /**
     * Full tax summary for a host for a given year.
     * GET /api/payouts/tax/summary?hostId=17&year=2026
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTaxSummary(
            @RequestParam Long hostId,
            @RequestParam(required = false) Integer year) {

        int targetYear = year != null ? year : LocalDate.now().getYear();
        TaxSummary summary = computeTaxSummary(hostId, targetYear);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("hostId", hostId);
        resp.put("year", targetYear);
        resp.put("grossRevenue", summary.totalRevenue);
        resp.put("platformCommission", summary.commission);
        resp.put("netEarnings", summary.netEarnings);
        resp.put("completedBookings", summary.completedBookings);
        resp.put("monthlyBreakdown", summary.monthlyBreakdown);

        return ResponseEntity.ok(resp);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private TaxSummary computeTaxSummary(Long hostId, int year) {
        List<Long> hotelIds = hotelRepository.findByHostId(hostId).stream()
            .map(Hotel::getId).collect(Collectors.toList());

        List<Booking> yearBookings = hotelIds.stream()
            .flatMap(hid -> bookingRepository.findByHotelId(hid).stream())
            .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                      || b.getStatus() == Booking.BookingStatus.COMPLETED)
            .filter(b -> b.getCheckInDate() != null && b.getCheckInDate().getYear() == year)
            .collect(Collectors.toList());

        BigDecimal gross = yearBookings.stream()
            .map(b -> BigDecimal.valueOf(b.getTotalPrice() != null ? b.getTotalPrice() : 0.0))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commission = gross.multiply(BigDecimal.valueOf(PLATFORM_COMMISSION_RATE))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        // Monthly breakdown
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            int month = m;
            BigDecimal monthTotal = yearBookings.stream()
                .filter(b -> b.getCheckInDate().getMonthValue() == month)
                .map(b -> BigDecimal.valueOf(b.getTotalPrice() != null ? b.getTotalPrice() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
            monthly.put(LocalDate.of(year, m, 1).getMonth().name(), monthTotal);
        }

        return new TaxSummary(gross.setScale(2, RoundingMode.HALF_UP), commission, net,
            yearBookings.size(), monthly);
    }

    private record TaxSummary(
        BigDecimal totalRevenue,
        BigDecimal commission,
        BigDecimal netEarnings,
        int completedBookings,
        Map<String, BigDecimal> monthlyBreakdown
    ) {}
}
