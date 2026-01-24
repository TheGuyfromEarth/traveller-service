package com.travolish.traveller.review.service.impl;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.travolish.traveller.review.dto.ReviewDTO;
import com.travolish.traveller.review.dto.ReviewModeratorDTO;
import com.travolish.traveller.review.dto.RatingStatsDTO;
import com.travolish.traveller.review.exception.InvalidReviewException;
import com.travolish.traveller.review.exception.ReviewNotFoundException;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.model.Review.ReviewStatus;
import com.travolish.traveller.review.model.Review.ReviewType;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.review.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    // ==================== Review Submission ====================

    @Override
    public ReviewDTO submitHotelReview(Long userId, Long hotelId, ReviewDTO reviewDTO) {
        // Check if user already reviewed this hotel
        if (reviewRepository.findUserHotelReview(userId, hotelId).isPresent()) {
            throw new InvalidReviewException("User has already reviewed this hotel. Please update your existing review.");
        }

        Review review = Review.builder()
                .userId(userId)
                .hotelId(hotelId)
                .title(reviewDTO.getTitle())
                .content(reviewDTO.getContent())
                .rating(reviewDTO.getRating())
                .reviewType(ReviewType.HOTEL)
                .status(ReviewStatus.PENDING)
                .build();

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    @Override
    public ReviewDTO submitRoomReview(Long userId, Long hotelId, Long roomId, ReviewDTO reviewDTO) {
        // Check if user already reviewed this room
        if (reviewRepository.findUserRoomReview(userId, roomId).isPresent()) {
            throw new InvalidReviewException("User has already reviewed this room. Please update your existing review.");
        }

        Review review = Review.builder()
                .userId(userId)
                .hotelId(hotelId)
                .roomId(roomId)
                .title(reviewDTO.getTitle())
                .content(reviewDTO.getContent())
                .rating(reviewDTO.getRating())
                .reviewType(ReviewType.ROOM)
                .status(ReviewStatus.PENDING)
                .build();

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    @Override
    public ReviewDTO getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));
        return convertToDTO(review);
    }

    @Override
    public Page<ReviewDTO> getHotelReviews(Long hotelId, Pageable pageable) {
        return reviewRepository.findApprovedHotelReviews(hotelId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ReviewDTO> getRoomReviews(Long roomId, Pageable pageable) {
        return reviewRepository.findApprovedRoomReviews(roomId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ReviewDTO> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(this::convertToDTO);
    }

    // ==================== Rating Statistics ====================

    @Override
    public RatingStatsDTO getHotelRatingStats(Long hotelId) {
        List<Review> approvedReviews = reviewRepository.findByHotelIdAndStatus(hotelId, ReviewStatus.APPROVED);

        if (approvedReviews.isEmpty()) {
            return RatingStatsDTO.builder()
                    .entityId(hotelId)
                    .entityType("HOTEL")
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .oneStar(0L)
                    .twoStars(0L)
                    .threeStars(0L)
                    .fourStars(0L)
                    .fiveStars(0L)
                    .percentageRating(0.0)
                    .build();
        }

        return calculateRatingStats(hotelId, "HOTEL", approvedReviews);
    }

    @Override
    public RatingStatsDTO getRoomRatingStats(Long roomId) {
        List<Review> approvedReviews = reviewRepository.findByStatus(ReviewStatus.APPROVED, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .filter(r -> roomId.equals(r.getRoomId()) && r.getReviewType() == ReviewType.ROOM)
                .toList();

        if (approvedReviews.isEmpty()) {
            return RatingStatsDTO.builder()
                    .entityId(roomId)
                    .entityType("ROOM")
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .oneStar(0L)
                    .twoStars(0L)
                    .threeStars(0L)
                    .fourStars(0L)
                    .fiveStars(0L)
                    .percentageRating(0.0)
                    .build();
        }

        return calculateRatingStats(roomId, "ROOM", approvedReviews);
    }

    private RatingStatsDTO calculateRatingStats(Long entityId, String entityType, List<Review> reviews) {
        long totalReviews = reviews.size();
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        long oneStar = reviews.stream().filter(r -> r.getRating() == 1).count();
        long twoStars = reviews.stream().filter(r -> r.getRating() == 2).count();
        long threeStars = reviews.stream().filter(r -> r.getRating() == 3).count();
        long fourStars = reviews.stream().filter(r -> r.getRating() == 4).count();
        long fiveStars = reviews.stream().filter(r -> r.getRating() == 5).count();

        double percentageRating = (fiveStars * 100.0) / totalReviews;

        return RatingStatsDTO.builder()
                .entityId(entityId)
                .entityType(entityType)
                .averageRating(Math.round(averageRating * 100.0) / 100.0)
                .totalReviews(totalReviews)
                .oneStar(oneStar)
                .twoStars(twoStars)
                .threeStars(threeStars)
                .fourStars(fourStars)
                .fiveStars(fiveStars)
                .percentageRating(Math.round(percentageRating * 100.0) / 100.0)
                .build();
    }

    // ==================== Review Moderation ====================

    @Override
    public ReviewModeratorDTO approveReview(Long reviewId, Long moderatorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        review.setModeratorId(moderatorId);
        review.setReviewedAt(OffsetDateTime.now());

        Review savedReview = reviewRepository.save(review);
        return convertToModeratorDTO(savedReview);
    }

    @Override
    public ReviewModeratorDTO rejectReview(Long reviewId, String reason, Long moderatorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidReviewException("Rejection reason is required");
        }

        review.setStatus(ReviewStatus.REJECTED);
        review.setModeratorNotes(reason);
        review.setModeratorId(moderatorId);
        review.setReviewedAt(OffsetDateTime.now());

        Review savedReview = reviewRepository.save(review);
        return convertToModeratorDTO(savedReview);
    }

    @Override
    public ReviewModeratorDTO flagReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.FLAGGED);

        Review savedReview = reviewRepository.save(review);
        return convertToModeratorDTO(savedReview);
    }

    @Override
    public Page<ReviewModeratorDTO> getPendingReviews(Pageable pageable) {
        return reviewRepository.findPendingReviews(pageable)
                .map(this::convertToModeratorDTO);
    }

    @Override
    public Page<ReviewModeratorDTO> getFlaggedReviews(Pageable pageable) {
        return reviewRepository.findFlaggedReviews(pageable)
                .map(this::convertToModeratorDTO);
    }

    // ==================== Review Management ====================

    @Override
    public ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Only allow updates if review is not approved
        if (review.getStatus() == ReviewStatus.APPROVED) {
            throw new InvalidReviewException("Cannot update an approved review. Please delete and create a new one.");
        }

        review.setTitle(reviewDTO.getTitle());
        review.setContent(reviewDTO.getContent());
        review.setRating(reviewDTO.getRating());
        review.setUpdatedAt(OffsetDateTime.now());

        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        reviewRepository.deleteById(reviewId);
    }

    // ==================== Helpful/Unhelpful Voting ====================

    @Override
    public ReviewDTO markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    @Override
    public ReviewDTO markUnhelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setUnhelpfulCount(review.getUnhelpfulCount() + 1);
        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    // ==================== Review Eligibility ====================

    @Override
    public boolean canUserReviewHotel(Long userId, Long hotelId) {
        return reviewRepository.findUserHotelReview(userId, hotelId).isEmpty();
    }

    @Override
    public boolean canUserReviewRoom(Long userId, Long roomId) {
        return reviewRepository.findUserRoomReview(userId, roomId).isEmpty();
    }

    // ==================== DTO Conversions ====================

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .hotelId(review.getHotelId())
                .roomId(review.getRoomId())
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .status(review.getStatus())
                .reviewType(review.getReviewType())
                .helpfulCount(review.getHelpfulCount())
                .unhelpfulCount(review.getUnhelpfulCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private ReviewModeratorDTO convertToModeratorDTO(Review review) {
        return ReviewModeratorDTO.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .hotelId(review.getHotelId())
                .roomId(review.getRoomId())
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .status(review.getStatus())
                .moderatorNotes(review.getModeratorNotes())
                .moderatorId(review.getModeratorId())
                .reviewedAt(review.getReviewedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
