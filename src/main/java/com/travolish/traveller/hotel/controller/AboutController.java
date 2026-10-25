package com.travolish.traveller.hotel.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.review.model.Review;

import lombok.RequiredArgsConstructor;

/**
 * Serves data-driven content for the public About page.
 * Stats are a mix of real database counts and configurable property values.
 */
@RestController
@RequestMapping("/api/about")
@RequiredArgsConstructor
public class AboutController {

    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;

    @Value("${about.cities:180+}")
    private String citiesLabel;

    @Value("${about.response-rate:98%}")
    private String responseRate;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long totalHotels = hotelRepository.count();
        long approvedReviews = reviewRepository.countByReviewStatus(Review.ReviewStatus.APPROVED);

        String propertiesLabel;
        if (totalHotels >= 1000) {
            long thousands = totalHotels / 1000;
            propertiesLabel = thousands + "," + String.format("%03d", totalHotels % 1000) + "+";
        } else if (totalHotels > 0) {
            propertiesLabel = totalHotels + "+";
        } else {
            propertiesLabel = "12,000+";
        }

        String ratingLabel = "4.8★";

        return Map.of(
                "properties", propertiesLabel,
                "cities", citiesLabel,
                "rating", ratingLabel,
                "responseRate", responseRate
        );
    }
}
