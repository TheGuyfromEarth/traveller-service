package com.travolish.traveller.review.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.review.dto.ReviewDTO;
import com.travolish.traveller.review.dto.ReviewModeratorDTO;
import com.travolish.traveller.review.dto.RatingStatsDTO;
import com.travolish.traveller.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== Review Submission ====================

    /**
     * Submit a review for a hotel
     * 
     * @param userId User submitting the review
     * @param hotelId Hotel being reviewed
     * @param reviewDTO Review details
     * @return Created review
     */
    @PostMapping("/hotels/{hotelId}")
    public ResponseEntity<ReviewDTO> submitHotelReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long hotelId,
            @Valid @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO createdReview = reviewService.submitHotelReview(userId, hotelId, reviewDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Submit a review for a specific room
     * 
     * @param userId User submitting the review
     * @param hotelId Hotel ID
     * @param roomId Room being reviewed
     * @param reviewDTO Review details
     * @return Created review
     */
    @PostMapping("/hotels/{hotelId}/rooms/{roomId}")
    public ResponseEntity<ReviewDTO> submitRoomReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @Valid @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO createdReview = reviewService.submitRoomReview(userId, hotelId, roomId, reviewDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // ==================== Review Retrieval ====================

    /**
     * Get a specific review by ID
     * 
     * @param reviewId Review ID
     * @return Review details
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReview(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Get all approved reviews for a hotel (paginated)
     * 
     * @param hotelId Hotel ID
     * @param pageable Pagination parameters
     * @return Page of reviews
     */
    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<Page<ReviewDTO>> getHotelReviews(
            @PathVariable Long hotelId,
            Pageable pageable) {
        Page<ReviewDTO> reviews = reviewService.getHotelReviews(hotelId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get all approved reviews for a room (paginated)
     * 
     * @param roomId Room ID
     * @param pageable Pagination parameters
     * @return Page of reviews
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Page<ReviewDTO>> getRoomReviews(
            @PathVariable Long roomId,
            Pageable pageable) {
        Page<ReviewDTO> reviews = reviewService.getRoomReviews(roomId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get all reviews submitted by a user
     * 
     * @param pageable Pagination parameters
     * @return Page of user's reviews
     */
    @GetMapping("/user")
    public ResponseEntity<Page<ReviewDTO>> getUserReviews(
            @RequestHeader("X-User-Id") Long userId,
            Pageable pageable) {
        Page<ReviewDTO> reviews = reviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    // ==================== Rating Statistics ====================

    /**
     * Get rating statistics and aggregation for a hotel
     * 
     * @param hotelId Hotel ID
     * @return Rating statistics
     */
    @GetMapping("/hotels/{hotelId}/stats")
    public ResponseEntity<RatingStatsDTO> getHotelRatingStats(@PathVariable Long hotelId) {
        RatingStatsDTO stats = reviewService.getHotelRatingStats(hotelId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get rating statistics and aggregation for a room
     * 
     * @param roomId Room ID
     * @return Rating statistics
     */
    @GetMapping("/rooms/{roomId}/stats")
    public ResponseEntity<RatingStatsDTO> getRoomRatingStats(@PathVariable Long roomId) {
        RatingStatsDTO stats = reviewService.getRoomRatingStats(roomId);
        return ResponseEntity.ok(stats);
    }

    // ==================== Review Management ====================

    /**
     * Update a review
     * 
     * @param reviewId Review ID
     * @param reviewDTO Updated review details
     * @return Updated review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO updatedReview = reviewService.updateReview(reviewId, reviewDTO);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Delete a review
     * 
     * @param reviewId Review ID
     * @return No content
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Review Moderation ====================

    /**
     * Approve a review (admin/moderator only)
     * 
     * @param reviewId Review ID
     * @return Approved review
     */
    @PostMapping("/{reviewId}/approve")
    public ResponseEntity<ReviewModeratorDTO> approveReview(
            @PathVariable Long reviewId,
            @RequestHeader("X-Moderator-Id") Long moderatorId) {
        ReviewModeratorDTO approvedReview = reviewService.approveReview(reviewId, moderatorId);
        return ResponseEntity.ok(approvedReview);
    }

    /**
     * Reject a review with reason (admin/moderator only)
     * 
     * @param reviewId Review ID
     * @param reason Rejection reason
     * @return Rejected review
     */
    @PostMapping("/{reviewId}/reject")
    public ResponseEntity<ReviewModeratorDTO> rejectReview(
            @PathVariable Long reviewId,
            @RequestParam String reason,
            @RequestHeader("X-Moderator-Id") Long moderatorId) {
        ReviewModeratorDTO rejectedReview = reviewService.rejectReview(reviewId, reason, moderatorId);
        return ResponseEntity.ok(rejectedReview);
    }

    /**
     * Flag a review for further review
     * 
     * @param reviewId Review ID
     * @return Flagged review
     */
    @PostMapping("/{reviewId}/flag")
    public ResponseEntity<ReviewModeratorDTO> flagReview(@PathVariable Long reviewId) {
        ReviewModeratorDTO flaggedReview = reviewService.flagReview(reviewId);
        return ResponseEntity.ok(flaggedReview);
    }

    /**
     * Get all pending reviews awaiting moderation (admin/moderator only)
     * 
     * @param pageable Pagination parameters
     * @return Page of pending reviews
     */
    @GetMapping("/moderation/pending")
    public ResponseEntity<Page<ReviewModeratorDTO>> getPendingReviews(Pageable pageable) {
        Page<ReviewModeratorDTO> pendingReviews = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(pendingReviews);
    }

    /**
     * Get all flagged reviews (admin/moderator only)
     * 
     * @param pageable Pagination parameters
     * @return Page of flagged reviews
     */
    @GetMapping("/moderation/flagged")
    public ResponseEntity<Page<ReviewModeratorDTO>> getFlaggedReviews(Pageable pageable) {
        Page<ReviewModeratorDTO> flaggedReviews = reviewService.getFlaggedReviews(pageable);
        return ResponseEntity.ok(flaggedReviews);
    }

    // ==================== Helpful/Unhelpful Voting ====================

    /**
     * Mark a review as helpful
     * 
     * @param reviewId Review ID
     * @return Updated review
     */
    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<ReviewDTO> markHelpful(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Mark a review as unhelpful
     * 
     * @param reviewId Review ID
     * @return Updated review
     */
    @PostMapping("/{reviewId}/unhelpful")
    public ResponseEntity<ReviewDTO> markUnhelpful(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.markUnhelpful(reviewId);
        return ResponseEntity.ok(review);
    }

    // ==================== Review Eligibility ====================

    /**
     * Check if user can review a hotel
     * 
     * @param hotelId Hotel ID
     * @return True if user can review, false otherwise
     */
    @GetMapping("/hotels/{hotelId}/can-review")
    public ResponseEntity<Boolean> canReviewHotel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long hotelId) {
        boolean canReview = reviewService.canUserReviewHotel(userId, hotelId);
        return ResponseEntity.ok(canReview);
    }

    /**
     * Check if user can review a room
     * 
     * @param roomId Room ID
     * @return True if user can review, false otherwise
     */
    @GetMapping("/rooms/{roomId}/can-review")
    public ResponseEntity<Boolean> canReviewRoom(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long roomId) {
        boolean canReview = reviewService.canUserReviewRoom(userId, roomId);
        return ResponseEntity.ok(canReview);
    }
}
