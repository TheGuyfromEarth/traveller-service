package com.travolish.traveller.review.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.travolish.traveller.review.dto.ReviewDTO;
import com.travolish.traveller.review.dto.RatingStatsDTO;
import com.travolish.traveller.review.exception.InvalidReviewException;
import com.travolish.traveller.review.exception.ReviewNotFoundException;
import com.travolish.traveller.review.model.Review;
import com.travolish.traveller.review.model.Review.ReviewStatus;
import com.travolish.traveller.review.model.Review.ReviewType;
import com.travolish.traveller.review.repository.ReviewRepository;
import com.travolish.traveller.review.service.impl.ReviewServiceImpl;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests — Review Submission & Moderation Flow")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private static final Long USER_ID   = 100L;
    private static final Long HOTEL_ID  = 1L;
    private static final Long ROOM_ID   = 10L;
    private static final Long MOD_ID    = 999L;

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private ReviewDTO buildReviewDTO(int rating) {
        ReviewDTO dto = new ReviewDTO();
        dto.setTitle("Great stay");
        dto.setContent("Everything was perfect.");
        dto.setRating(rating);
        return dto;
    }

    private Review buildReview(Long id, ReviewStatus status, ReviewType type, int rating) {
        return Review.builder()
                .id(id)
                .userId(USER_ID)
                .hotelId(HOTEL_ID)
                .roomId(type == ReviewType.ROOM ? ROOM_ID : null)
                .title("Great stay")
                .content("Everything was perfect.")
                .rating(rating)
                .status(status)
                .reviewType(type)
                .helpfulCount(0)
                .unhelpfulCount(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // submitHotelReview()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitHotelReview() — hotel review submission")
    class SubmitHotelReview {

        @Test
        @DisplayName("TC-RV-01 Happy path: review saved with PENDING status")
        void happyPath() {
            when(reviewRepository.findUserHotelReview(USER_ID, HOTEL_ID)).thenReturn(Optional.empty());
            Review saved = buildReview(1L, ReviewStatus.PENDING, ReviewType.HOTEL, 5);
            when(reviewRepository.save(any(Review.class))).thenReturn(saved);

            ReviewDTO result = reviewService.submitHotelReview(USER_ID, HOTEL_ID, buildReviewDTO(5));

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(ReviewStatus.PENDING, result.getStatus());
            assertEquals(ReviewType.HOTEL, result.getReviewType());
        }

        @Test
        @DisplayName("TC-RV-02 Duplicate hotel review: throws InvalidReviewException")
        void duplicateThrows() {
            Review existing = buildReview(1L, ReviewStatus.APPROVED, ReviewType.HOTEL, 4);
            when(reviewRepository.findUserHotelReview(USER_ID, HOTEL_ID)).thenReturn(Optional.of(existing));

            assertThrows(InvalidReviewException.class,
                () -> reviewService.submitHotelReview(USER_ID, HOTEL_ID, buildReviewDTO(3)));
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-RV-03 Correct entity shape: roomId is null for hotel review")
        void roomIdIsNullForHotelReview() {
            when(reviewRepository.findUserHotelReview(USER_ID, HOTEL_ID)).thenReturn(Optional.empty());
            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            Review saved = buildReview(2L, ReviewStatus.PENDING, ReviewType.HOTEL, 5);
            when(reviewRepository.save(captor.capture())).thenReturn(saved);

            reviewService.submitHotelReview(USER_ID, HOTEL_ID, buildReviewDTO(5));

            assertNull(captor.getValue().getRoomId(), "Hotel review must not carry a roomId");
            assertEquals(ReviewType.HOTEL, captor.getValue().getReviewType());
            assertEquals(ReviewStatus.PENDING, captor.getValue().getStatus());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // submitRoomReview()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitRoomReview() — room review submission")
    class SubmitRoomReview {

        @Test
        @DisplayName("TC-RV-04 Happy path: room review saved with PENDING status")
        void happyPath() {
            when(reviewRepository.findUserRoomReview(USER_ID, ROOM_ID)).thenReturn(Optional.empty());
            Review saved = buildReview(3L, ReviewStatus.PENDING, ReviewType.ROOM, 4);
            when(reviewRepository.save(any())).thenReturn(saved);

            ReviewDTO result = reviewService.submitRoomReview(USER_ID, HOTEL_ID, ROOM_ID, buildReviewDTO(4));

            assertEquals(ReviewType.ROOM, result.getReviewType());
            assertEquals(ReviewStatus.PENDING, result.getStatus());
        }

        @Test
        @DisplayName("TC-RV-05 Duplicate room review: throws InvalidReviewException")
        void duplicateThrows() {
            Review existing = buildReview(3L, ReviewStatus.PENDING, ReviewType.ROOM, 4);
            when(reviewRepository.findUserRoomReview(USER_ID, ROOM_ID)).thenReturn(Optional.of(existing));

            assertThrows(InvalidReviewException.class,
                () -> reviewService.submitRoomReview(USER_ID, HOTEL_ID, ROOM_ID, buildReviewDTO(4)));
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-RV-06 Correct entity shape: roomId and ROOM type are set")
        void roomIdAndTypeAreSet() {
            when(reviewRepository.findUserRoomReview(USER_ID, ROOM_ID)).thenReturn(Optional.empty());
            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            Review saved = buildReview(4L, ReviewStatus.PENDING, ReviewType.ROOM, 5);
            when(reviewRepository.save(captor.capture())).thenReturn(saved);

            reviewService.submitRoomReview(USER_ID, HOTEL_ID, ROOM_ID, buildReviewDTO(5));

            assertEquals(ROOM_ID, captor.getValue().getRoomId());
            assertEquals(ReviewType.ROOM, captor.getValue().getReviewType());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approveReview() / rejectReview() / flagReview()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Moderation: approve / reject / flag")
    class Moderation {

        @Test
        @DisplayName("TC-RV-07 approveReview: status set to APPROVED, moderatorId captured")
        void approve() {
            Review review = buildReview(10L, ReviewStatus.PENDING, ReviewType.HOTEL, 5);
            when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var dto = reviewService.approveReview(10L, MOD_ID);

            assertEquals(ReviewStatus.APPROVED, review.getStatus());
            assertEquals(MOD_ID, review.getModeratorId());
            assertNotNull(review.getReviewedAt());
            assertEquals(ReviewStatus.APPROVED, dto.getStatus());
        }

        @Test
        @DisplayName("TC-RV-08 approveReview on missing review: throws ReviewNotFoundException")
        void approveNotFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.approveReview(999L, MOD_ID));
        }

        @Test
        @DisplayName("TC-RV-09 rejectReview: status set to REJECTED with reason stored")
        void reject() {
            Review review = buildReview(11L, ReviewStatus.PENDING, ReviewType.HOTEL, 2);
            when(reviewRepository.findById(11L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.rejectReview(11L, "Violates community guidelines", MOD_ID);

            assertEquals(ReviewStatus.REJECTED, review.getStatus());
            assertEquals("Violates community guidelines", review.getModeratorNotes());
            assertEquals(MOD_ID, review.getModeratorId());
        }

        @Test
        @DisplayName("TC-RV-10 rejectReview without reason: throws InvalidReviewException")
        void rejectWithoutReasonThrows() {
            Review review = buildReview(12L, ReviewStatus.PENDING, ReviewType.HOTEL, 1);
            when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

            assertThrows(InvalidReviewException.class,
                () -> reviewService.rejectReview(12L, "", MOD_ID),
                "Empty rejection reason must be rejected");
            assertThrows(InvalidReviewException.class,
                () -> reviewService.rejectReview(12L, null, MOD_ID),
                "Null rejection reason must be rejected");
        }

        @Test
        @DisplayName("TC-RV-11 flagReview: status set to FLAGGED")
        void flag() {
            Review review = buildReview(13L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5);
            when(reviewRepository.findById(13L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.flagReview(13L);

            assertEquals(ReviewStatus.FLAGGED, review.getStatus());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateReview()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateReview() — editing a review")
    class UpdateReview {

        @Test
        @DisplayName("TC-RV-12 PENDING review can be updated")
        void pendingCanBeUpdated() {
            Review review = buildReview(20L, ReviewStatus.PENDING, ReviewType.HOTEL, 3);
            when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewDTO patch = buildReviewDTO(4);
            patch.setTitle("Updated title");
            patch.setContent("Updated content after second thought.");

            ReviewDTO result = reviewService.updateReview(20L, patch);

            assertEquals("Updated title", review.getTitle());
            assertEquals(4, review.getRating());
        }

        @Test
        @DisplayName("TC-RV-13 APPROVED review cannot be updated: throws InvalidReviewException")
        void approvedCannotBeUpdated() {
            Review review = buildReview(21L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5);
            when(reviewRepository.findById(21L)).thenReturn(Optional.of(review));

            assertThrows(InvalidReviewException.class,
                () -> reviewService.updateReview(21L, buildReviewDTO(4)),
                "Approved reviews are locked from edits");
        }

        @Test
        @DisplayName("TC-RV-14 Update on missing review: throws ReviewNotFoundException")
        void updateNotFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class,
                () -> reviewService.updateReview(999L, buildReviewDTO(4)));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteReview()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteReview() — removal")
    class DeleteReview {

        @Test
        @DisplayName("TC-RV-15 Existing review is deleted")
        void deleteExisting() {
            Review review = buildReview(30L, ReviewStatus.PENDING, ReviewType.HOTEL, 3);
            when(reviewRepository.findById(30L)).thenReturn(Optional.of(review));

            reviewService.deleteReview(30L);

            verify(reviewRepository).deleteById(30L);
        }

        @Test
        @DisplayName("TC-RV-16 Missing review: throws ReviewNotFoundException, no delete called")
        void deleteMissingThrows() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class,
                () -> reviewService.deleteReview(999L));
            verify(reviewRepository, never()).deleteById(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // markHelpful() / markUnhelpful()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markHelpful() / markUnhelpful() — vote counting")
    class VoteCounting {

        @Test
        @DisplayName("TC-RV-17 markHelpful increments helpfulCount by 1")
        void markHelpful() {
            Review review = buildReview(40L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5);
            // helpfulCount starts at 0
            when(reviewRepository.findById(40L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewDTO result = reviewService.markHelpful(40L);

            assertEquals(1, review.getHelpfulCount(), "HelpfulCount should be 1 after single vote");
        }

        @Test
        @DisplayName("TC-RV-18 markUnhelpful increments unhelpfulCount by 1")
        void markUnhelpful() {
            Review review = buildReview(41L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5);
            when(reviewRepository.findById(41L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.markUnhelpful(41L);

            assertEquals(1, review.getUnhelpfulCount());
        }

        @Test
        @DisplayName("TC-RV-19 markHelpful on missing review: throws ReviewNotFoundException")
        void markHelpfulNotFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.markHelpful(999L));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // canUserReviewHotel() / canUserReviewRoom()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Review eligibility checks")
    class Eligibility {

        @Test
        @DisplayName("TC-RV-20 canUserReviewHotel: true when no prior review")
        void canReviewHotelWhenNoPrior() {
            when(reviewRepository.findUserHotelReview(USER_ID, HOTEL_ID)).thenReturn(Optional.empty());
            assertTrue(reviewService.canUserReviewHotel(USER_ID, HOTEL_ID));
        }

        @Test
        @DisplayName("TC-RV-21 canUserReviewHotel: false when review already exists")
        void cannotReviewHotelIfAlreadyDone() {
            when(reviewRepository.findUserHotelReview(USER_ID, HOTEL_ID))
                .thenReturn(Optional.of(buildReview(1L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5)));
            assertFalse(reviewService.canUserReviewHotel(USER_ID, HOTEL_ID));
        }

        @Test
        @DisplayName("TC-RV-22 canUserReviewRoom: true when no prior review")
        void canReviewRoomWhenNoPrior() {
            when(reviewRepository.findUserRoomReview(USER_ID, ROOM_ID)).thenReturn(Optional.empty());
            assertTrue(reviewService.canUserReviewRoom(USER_ID, ROOM_ID));
        }

        @Test
        @DisplayName("TC-RV-23 canUserReviewRoom: false when review already exists")
        void cannotReviewRoomIfAlreadyDone() {
            when(reviewRepository.findUserRoomReview(USER_ID, ROOM_ID))
                .thenReturn(Optional.of(buildReview(2L, ReviewStatus.PENDING, ReviewType.ROOM, 4)));
            assertFalse(reviewService.canUserReviewRoom(USER_ID, ROOM_ID));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getHotelRatingStats()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHotelRatingStats() — aggregation")
    class RatingStats {

        @Test
        @DisplayName("TC-RV-24 No approved reviews: returns zeroed stats")
        void noReviews() {
            when(reviewRepository.findByHotelIdAndStatus(HOTEL_ID, ReviewStatus.APPROVED))
                .thenReturn(List.of());

            RatingStatsDTO stats = reviewService.getHotelRatingStats(HOTEL_ID);

            assertEquals(0.0, stats.getAverageRating());
            assertEquals(0L,  stats.getTotalReviews());
        }

        @Test
        @DisplayName("TC-RV-25 Mixed ratings: averageRating and counts are correct")
        void mixedRatings() {
            List<Review> reviews = List.of(
                buildReview(1L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5),
                buildReview(2L, ReviewStatus.APPROVED, ReviewType.HOTEL, 4),
                buildReview(3L, ReviewStatus.APPROVED, ReviewType.HOTEL, 3),
                buildReview(4L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5)
            );
            when(reviewRepository.findByHotelIdAndStatus(HOTEL_ID, ReviewStatus.APPROVED))
                .thenReturn(reviews);

            RatingStatsDTO stats = reviewService.getHotelRatingStats(HOTEL_ID);

            assertEquals(4L, stats.getTotalReviews());
            assertEquals(4.25, stats.getAverageRating(), 0.01, "(5+4+3+5)/4 = 4.25");
            assertEquals(2L, stats.getFiveStars(), "Two 5-star ratings");
            assertEquals(1L, stats.getFourStars());
            assertEquals(1L, stats.getThreeStars());
            assertEquals(0L, stats.getTwoStars());
            assertEquals(0L, stats.getOneStar());
        }

        @Test
        @DisplayName("TC-RV-26 All five-star: percentageRating = 100.0")
        void allFiveStar() {
            List<Review> reviews = List.of(
                buildReview(5L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5),
                buildReview(6L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5)
            );
            when(reviewRepository.findByHotelIdAndStatus(HOTEL_ID, ReviewStatus.APPROVED))
                .thenReturn(reviews);

            RatingStatsDTO stats = reviewService.getHotelRatingStats(HOTEL_ID);

            assertEquals(100.0, stats.getFiveStarPercentage(), 0.01);
        }

        @Test
        @DisplayName("TC-RV-27 getReviewById: returns correct DTO or throws on missing")
        void getByIdAndNotFound() {
            Review review = buildReview(60L, ReviewStatus.APPROVED, ReviewType.HOTEL, 4);
            when(reviewRepository.findById(60L)).thenReturn(Optional.of(review));

            ReviewDTO dto = reviewService.getReviewById(60L);
            assertEquals(60L, dto.getId());

            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ReviewNotFoundException.class, () -> reviewService.getReviewById(999L));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getHotelReviews() / getUserReviews()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Paginated review retrieval")
    class PaginatedRetrieval {

        @Test
        @DisplayName("TC-RV-28 getHotelReviews: returns only APPROVED hotel reviews")
        void hotelReviews() {
            Pageable pageable = PageRequest.of(0, 10);
            Review r1 = buildReview(70L, ReviewStatus.APPROVED, ReviewType.HOTEL, 5);
            Page<Review> page = new PageImpl<>(List.of(r1));
            when(reviewRepository.findApprovedHotelReviews(HOTEL_ID, pageable)).thenReturn(page);

            Page<ReviewDTO> result = reviewService.getHotelReviews(HOTEL_ID, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(ReviewStatus.APPROVED, result.getContent().get(0).getStatus());
        }

        @Test
        @DisplayName("TC-RV-29 getUserReviews: returns all reviews for a user")
        void userReviews() {
            Pageable pageable = PageRequest.of(0, 5);
            Review r1 = buildReview(80L, ReviewStatus.PENDING, ReviewType.HOTEL, 3);
            Review r2 = buildReview(81L, ReviewStatus.APPROVED, ReviewType.ROOM, 5);
            Page<Review> page = new PageImpl<>(List.of(r1, r2));
            when(reviewRepository.findByUserId(USER_ID, pageable)).thenReturn(page);

            Page<ReviewDTO> result = reviewService.getUserReviews(USER_ID, pageable);

            assertEquals(2, result.getTotalElements());
        }
    }
}
