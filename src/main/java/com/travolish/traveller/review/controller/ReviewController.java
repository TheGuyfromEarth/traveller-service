package com.travolish.traveller.review.controller;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.review.dto.ReviewDTO;
import com.travolish.traveller.review.dto.ReviewModeratorDTO;
import com.travolish.traveller.review.dto.RatingStatsDTO;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

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
     * Escalate a review to senior review team
     *
     * @param reviewId Review ID
     * @return Escalated review
     */
    @PostMapping("/{reviewId}/escalate")
    public ResponseEntity<ReviewModeratorDTO> escalateReview(@PathVariable Long reviewId) {
        ReviewModeratorDTO escalatedReview = reviewService.escalateReview(reviewId);
        return ResponseEntity.ok(escalatedReview);
    }

    /**
     * Redact review content (admin only) — overwrites title and content, then rejects.
     */
    @PostMapping("/{reviewId}/redact")
    public ResponseEntity<ReviewModeratorDTO> redactReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "X-Moderator-Id", defaultValue = "1") Long moderatorId) {
        reviewRepository.findById(reviewId).ifPresent(review -> {
            review.setTitle("[Redacted]");
            review.setContent("[This content has been removed by a platform administrator.]");
            reviewRepository.save(review);
        });
        ReviewModeratorDTO redacted = reviewService.rejectReview(reviewId, "Content redacted by admin", moderatorId);
        return ResponseEntity.ok(redacted);
    }

    /**
     * Dismiss a flagged/pending review — no policy violation found, approve and close.
     */
    @PostMapping("/{reviewId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissReview(@PathVariable Long reviewId) {
        reviewRepository.findById(reviewId).ifPresent(review -> {
            review.setStatus(Review.ReviewStatus.APPROVED);
            review.setModeratorNotes("Dismissed — no policy violation found");
            reviewRepository.save(review);
        });
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

    // ==================== Host-to-Guest Reviews ====================

    /**
     * Host submits a review for a guest after a completed booking.
     * Uses X-Host-Id header (the host's userId) so the same JWT filter flow applies.
     */
    @PostMapping("/guests/{guestId}")
    public ResponseEntity<ReviewDTO> submitGuestReview(
            @RequestHeader("X-Host-Id") Long hostUserId,
            @PathVariable Long guestId,
            @RequestParam Long bookingId,
            @Valid @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO created = reviewService.submitGuestReview(hostUserId, guestId, bookingId, reviewDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Get approved guest reviews for a specific guest (visible to admins / the guest themselves).
     */
    @GetMapping("/guests/{guestId}")
    public ResponseEntity<Page<ReviewDTO>> getGuestReviews(
            @PathVariable Long guestId,
            Pageable pageable) {
        Page<ReviewDTO> reviews = reviewRepository.findApprovedGuestReviews(guestId, pageable)
                .map(review -> reviewService.getReviewById(review.getId()));
        return ResponseEntity.ok(reviews);
    }

    // ==================== §22 Host Response ====================

    @PostMapping("/{reviewId}/respond")
    public ResponseEntity<ReviewDTO> hostRespond(
            @PathVariable Long reviewId,
            @RequestParam String response,
            @RequestHeader("X-Host-Id") Long hostId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        review.setHostResponse(response);
        review.setHostResponseAt(OffsetDateTime.now());
        reviewRepository.save(review);
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    // ==================== Moderator Assignment ====================

    /**
     * Assign a moderator to a review without changing its status.
     * moderatorId is the admin/moderator's user ID.
     */
    @PatchMapping("/{reviewId}/assign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignModerator(
            @PathVariable Long reviewId,
            @RequestParam Long moderatorId) {
        reviewRepository.findById(reviewId).ifPresent(review -> {
            review.setModeratorId(moderatorId);
            reviewRepository.save(review);
        });
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
