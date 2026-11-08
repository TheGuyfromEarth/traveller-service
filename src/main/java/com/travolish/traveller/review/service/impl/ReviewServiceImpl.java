package com.travolish.traveller.review.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final HotelRepository hotelRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ==================== Review Submission ====================

    @Override
    @Transactional
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
    @Transactional
    public ReviewDTO submitGuestReview(Long hostUserId, Long guestId, Long bookingId, ReviewDTO reviewDTO) {
        if (reviewRepository.findHostGuestReview(hostUserId, guestId, bookingId).isPresent()) {
            throw new InvalidReviewException("You have already reviewed this guest for booking #" + bookingId + ".");
        }

        Review review = Review.builder()
                .userId(hostUserId)
                .hotelId(reviewDTO.getHotelId() != null ? reviewDTO.getHotelId() : 0L)
                .guestId(guestId)
                .bookingId(bookingId)
                .title(reviewDTO.getTitle())
                .content(reviewDTO.getContent())
                .rating(reviewDTO.getRating())
                .cleanlinessRating(reviewDTO.getCleanlinessRating())
                .theftRating(reviewDTO.getTheftRating())
                .behaviorRating(reviewDTO.getBehaviorRating())
                .reviewType(Review.ReviewType.GUEST)
                .status(Review.ReviewStatus.PENDING)
                .build();

        return convertToDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
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
                    .fiveStarPercentage(0.0)
                    .build();
        }

        return calculateRatingStats(hotelId, "HOTEL", approvedReviews);
    }

    @Override
    public RatingStatsDTO getRoomRatingStats(Long roomId) {
        List<Review> approvedReviews = reviewRepository.findByRoomIdAndStatus(roomId, ReviewStatus.APPROVED);

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
                    .fiveStarPercentage(0.0)
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

        // Single pass instead of 5 separate filter().count() passes
        Map<Integer, Long> byStar = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));
        long oneStar    = byStar.getOrDefault(1, 0L);
        long twoStars   = byStar.getOrDefault(2, 0L);
        long threeStars = byStar.getOrDefault(3, 0L);
        long fourStars  = byStar.getOrDefault(4, 0L);
        long fiveStars  = byStar.getOrDefault(5, 0L);

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
                .fiveStarPercentage(Math.round(percentageRating * 100.0) / 100.0)
                .build();
    }

    // ==================== Review Moderation ====================

    @Override
    @Transactional
    public ReviewModeratorDTO approveReview(Long reviewId, Long moderatorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        review.setModeratorId(moderatorId);
        review.setReviewedAt(OffsetDateTime.now());

        Review savedReview = reviewRepository.save(review);

        // Recompute hotel rating from all approved HOTEL reviews
        if (review.getHotelId() != null) {
            refreshHotelRating(review.getHotelId());
        }

        // Notify the reviewer
        notifyReviewStatus(savedReview, "Your review has been approved and is now live. Thank you for your feedback!");

        return convertToModeratorDTO(savedReview);
    }

    /** Sends an in-app + email notification to the review author about their review status. */
    private void notifyReviewStatus(Review review, String message) {
        try {
            userRepository.findById(review.getUserId()).ifPresent(user -> {
                SendNotificationRequest req = new SendNotificationRequest();
                req.setUserId(user.getId());
                req.setType(NotificationType.BOOKING_CONFIRMATION); // reuse as generic notification
                req.setChannel(NotificationChannel.EMAIL);
                req.setRecipientEmail(user.getEmail());
                req.setSubject("Update on your Travolish review");
                req.setMessage("Hi " + (user.getFirstName() != null ? user.getFirstName() : "there") + ",\n\n" + message + "\n\n— The Travolish Team");
                req.setSendImmediately(true);
                notificationService.sendNotificationAsync(req);
            });
        } catch (Exception e) {
            // Non-critical
        }
    }

    /** Recalculates and persists hotel.rating via a DB AVG aggregate — no entity list loaded. */
    private void refreshHotelRating(Long hotelId) {
        Double avg = reviewRepository.findAverageRatingByHotelId(hotelId);
        if (avg == null) return;
        hotelRepository.findById(hotelId).ifPresent(hotel -> {
            hotel.setRating(Math.round(avg * 10.0) / 10.0);
            hotelRepository.save(hotel);
        });
    }

    @Override
    @Transactional
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
        notifyReviewStatus(savedReview, "Your review was not published. Reason: " + reason + ". You may edit and resubmit from the Reviews section.");
        return convertToModeratorDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewModeratorDTO flagReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.FLAGGED);

        Review savedReview = reviewRepository.save(review);
        return convertToModeratorDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewModeratorDTO escalateReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.ESCALATED);

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
    @Transactional
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
    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        reviewRepository.deleteById(reviewId);
    }

    // ==================== Helpful/Unhelpful Voting ====================

    @Override
    @Transactional
    public ReviewDTO markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    @Override
    @Transactional
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
                .guestId(review.getGuestId())
                .bookingId(review.getBookingId())
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .status(review.getStatus())
                .reviewType(review.getReviewType())
                .helpfulCount(review.getHelpfulCount())
                .unhelpfulCount(review.getUnhelpfulCount())
                .cleanlinessRating(review.getCleanlinessRating())
                .theftRating(review.getTheftRating())
                .behaviorRating(review.getBehaviorRating())
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
