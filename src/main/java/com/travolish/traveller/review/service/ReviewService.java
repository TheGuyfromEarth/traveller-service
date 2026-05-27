package com.travolish.traveller.review.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.travolish.traveller.review.dto.ReviewDTO;
import com.travolish.traveller.review.dto.ReviewModeratorDTO;
import com.travolish.traveller.review.dto.RatingStatsDTO;

public interface ReviewService {

    // Review submission and retrieval
    ReviewDTO submitHotelReview(Long userId, Long hotelId, ReviewDTO reviewDTO);

    ReviewDTO submitRoomReview(Long userId, Long hotelId, Long roomId, ReviewDTO reviewDTO);

    ReviewDTO getReviewById(Long reviewId);

    Page<ReviewDTO> getHotelReviews(Long hotelId, Pageable pageable);

    Page<ReviewDTO> getRoomReviews(Long roomId, Pageable pageable);

    Page<ReviewDTO> getUserReviews(Long userId, Pageable pageable);

    // Rating statistics and aggregation
    RatingStatsDTO getHotelRatingStats(Long hotelId);

    RatingStatsDTO getRoomRatingStats(Long roomId);

    // Review moderation
    ReviewModeratorDTO approveReview(Long reviewId, Long moderatorId);

    ReviewModeratorDTO rejectReview(Long reviewId, String reason, Long moderatorId);

    ReviewModeratorDTO flagReview(Long reviewId);

    ReviewModeratorDTO escalateReview(Long reviewId);

    Page<ReviewModeratorDTO> getPendingReviews(Pageable pageable);

    Page<ReviewModeratorDTO> getFlaggedReviews(Pageable pageable);

    // Review management
    ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO);

    void deleteReview(Long reviewId);

    // Helpful/Unhelpful voting
    ReviewDTO markHelpful(Long reviewId);

    ReviewDTO markUnhelpful(Long reviewId);

    // Check if user can review
    boolean canUserReviewHotel(Long userId, Long hotelId);

    boolean canUserReviewRoom(Long userId, Long roomId);
}
