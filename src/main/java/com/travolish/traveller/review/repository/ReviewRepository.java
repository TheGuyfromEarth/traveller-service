package com.travolish.traveller.review.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.model.Review.ReviewStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find reviews by hotel (only approved)
    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId AND r.status = 'APPROVED' AND r.reviewType = 'HOTEL' ORDER BY r.createdAt DESC")
    Page<Review> findApprovedHotelReviews(@Param("hotelId") Long hotelId, Pageable pageable);

    // Find reviews by room (only approved)
    @Query("SELECT r FROM Review r WHERE r.roomId = :roomId AND r.status = 'APPROVED' AND r.reviewType = 'ROOM' ORDER BY r.createdAt DESC")
    Page<Review> findApprovedRoomReviews(@Param("roomId") Long roomId, Pageable pageable);

    // Find all reviews by hotel (including pending for moderators)
    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId ORDER BY r.createdAt DESC")
    List<Review> findAllHotelReviews(@Param("hotelId") Long hotelId);

    // Find reviews by specific status
    @Query("SELECT r FROM Review r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<Review> findByStatus(@Param("status") ReviewStatus status, Pageable pageable);

    // Find reviews by user
    @Query("SELECT r FROM Review r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    Page<Review> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Find reviews by hotel and status
    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId AND r.status = :status ORDER BY r.createdAt DESC")
    List<Review> findByHotelIdAndStatus(@Param("hotelId") Long hotelId, @Param("status") ReviewStatus status);

    // Find reviews by rating range
    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId AND r.rating >= :minRating AND r.rating <= :maxRating AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    List<Review> findByHotelIdAndRatingRange(@Param("hotelId") Long hotelId, @Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating);

    // Check if user already reviewed a hotel — ORDER BY + LIMIT guards against duplicate rows
    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.hotelId = :hotelId AND r.reviewType = 'HOTEL' ORDER BY r.createdAt DESC LIMIT 1")
    Optional<Review> findUserHotelReview(@Param("userId") Long userId, @Param("hotelId") Long hotelId);

    // Check if user already reviewed a room
    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.roomId = :roomId AND r.reviewType = 'ROOM' ORDER BY r.createdAt DESC LIMIT 1")
    Optional<Review> findUserRoomReview(@Param("userId") Long userId, @Param("roomId") Long roomId);

    // Count approved reviews for hotel
    @Query("SELECT COUNT(r) FROM Review r WHERE r.hotelId = :hotelId AND r.status = 'APPROVED' AND r.reviewType = 'HOTEL'")
    Long countApprovedHotelReviews(@Param("hotelId") Long hotelId);

    // Find reviews by room and status (for stats aggregation)
    @Query("SELECT r FROM Review r WHERE r.roomId = :roomId AND r.status = :status AND r.reviewType = 'ROOM' ORDER BY r.createdAt DESC")
    List<Review> findByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") ReviewStatus status);

    // Count approved reviews for room
    @Query("SELECT COUNT(r) FROM Review r WHERE r.roomId = :roomId AND r.status = 'APPROVED' AND r.reviewType = 'ROOM'")
    Long countApprovedRoomReviews(@Param("roomId") Long roomId);

    // Find pending reviews for moderation
    @Query("SELECT r FROM Review r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    Page<Review> findPendingReviews(Pageable pageable);

    // Find flagged reviews for moderation
    @Query("SELECT r FROM Review r WHERE r.status = 'FLAGGED' ORDER BY r.createdAt ASC")
    Page<Review> findFlaggedReviews(Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.status = :status")
    long countByReviewStatus(@Param("status") ReviewStatus status);

    // Find GUEST reviews written by a specific host (userId = hostUserId, reviewType = GUEST)
    @Query("SELECT r FROM Review r WHERE r.userId = :hostUserId AND r.reviewType = 'GUEST' ORDER BY r.createdAt DESC")
    Page<Review> findGuestReviewsByHost(@Param("hostUserId") Long hostUserId, Pageable pageable);

    // Find GUEST reviews about a specific guest
    @Query("SELECT r FROM Review r WHERE r.guestId = :guestId AND r.reviewType = 'GUEST' AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    Page<Review> findApprovedGuestReviews(@Param("guestId") Long guestId, Pageable pageable);

    // Check if host already reviewed this guest for a specific booking
    @Query("SELECT r FROM Review r WHERE r.userId = :hostUserId AND r.guestId = :guestId AND r.bookingId = :bookingId AND r.reviewType = 'GUEST' LIMIT 1")
    Optional<Review> findHostGuestReview(@Param("hostUserId") Long hostUserId, @Param("guestId") Long guestId, @Param("bookingId") Long bookingId);

    // ── AnalyticsService batch query — avoids N+1 per-hotel loop ─────────────
    @Query("SELECT r FROM Review r WHERE r.hotelId IN :hotelIds AND r.status = :status")
    List<Review> findByHotelIdInAndStatus(@Param("hotelIds") List<Long> hotelIds, @Param("status") ReviewStatus status);

    // Aggregate AVG for refreshHotelRating — avoids loading full entity list
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotelId = :hotelId AND r.status = 'APPROVED' AND r.reviewType = 'HOTEL'")
    Double findAverageRatingByHotelId(@Param("hotelId") Long hotelId);
}
