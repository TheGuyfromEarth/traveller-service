package com.travolish.traveller.review.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_reviews_hotel_id",  columnList = "hotelId"),
    @Index(name = "idx_reviews_user_id",   columnList = "userId"),
    @Index(name = "idx_reviews_room_id",   columnList = "roomId"),
    @Index(name = "idx_reviews_status",    columnList = "status"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long hotelId;

    private Long roomId; // Optional: review can be for hotel or specific room

    private Long guestId; // Populated for GUEST review type — the guest being reviewed

    private Long bookingId; // Booking this review is associated with (GUEST reviews)

    @NotBlank(message = "Review title cannot be blank")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Review content cannot be blank")
    @Column(nullable = false, length = 5000)
    private String content;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType reviewType;

    private String moderatorNotes;

    private Long moderatorId;

    private OffsetDateTime reviewedAt;

    // Hotel review category sub-ratings (1–5 each)
    @Min(1) @Max(5)
    private Integer cleanlinessRating;

    @Min(1) @Max(5)
    private Integer accuracyRating;

    @Min(1) @Max(5)
    private Integer communicationRating;

    @Min(1) @Max(5)
    private Integer locationRating;

    @Min(1) @Max(5)
    private Integer checkInRating;

    @Min(1) @Max(5)
    private Integer valueRating;

    @Min(1) @Max(5)
    private Integer staffRating;

    @Min(1) @Max(5)
    private Integer comfortRating;

    // Comma-separated highlight tags selected by the reviewer
    @Column(length = 1000)
    private String tags;

    // Guest-review-specific sub-ratings (host reviewing a guest)
    @Min(1) @Max(5)
    private Integer theftRating;      // Did the guest report any theft concerns?

    @Min(1) @Max(5)
    private Integer behaviorRating;   // Was the guest respectful and well-behaved?

    // §22 — Host response
    @Column(length = 2000)
    private String hostResponse;

    private OffsetDateTime hostResponseAt;

    // §22 — Import source (e.g. "Booking.com", "Google")
    private String importedFrom;

    @Builder.Default
    private Integer helpfulCount = 0;

    @Builder.Default
    private Integer unhelpfulCount = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ReviewStatus {
        PENDING,      // Awaiting moderation
        APPROVED,     // Approved and visible
        REJECTED,     // Rejected by moderator
        FLAGGED,      // Flagged for review
        ESCALATED     // Escalated to senior review
    }

    public enum ReviewType {
        HOTEL,
        ROOM,
        GUEST   // Host reviewing a guest after a completed stay
    }
}
