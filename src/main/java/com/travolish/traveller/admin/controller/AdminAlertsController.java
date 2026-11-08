package com.travolish.traveller.admin.controller;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
public class AdminAlertsController {

    private final HostKYCRepository hostKYCRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAlerts() {
        long pendingKYC = hostKYCRepository.findPendingVerifications().size();
        long pendingListings = hotelRepository.findByStatus(Hotel.HotelStatus.PENDING_REVIEW).size();
        long flaggedReviews = reviewRepository.countByReviewStatus(Review.ReviewStatus.FLAGGED);

        return ResponseEntity.ok(List.of(
            Map.of("type", "KYC",        "count", pendingKYC,       "label", "KYC submissions awaiting review",       "href", "/admin/verification"),
            Map.of("type", "LISTINGS",   "count", pendingListings,  "label", "Listing submissions awaiting approval", "href", "/admin/listing-approvals"),
            Map.of("type", "MODERATION", "count", flaggedReviews,   "label", "Reviews flagged for moderation",        "href", "/admin/moderation")
        ));
    }
}
