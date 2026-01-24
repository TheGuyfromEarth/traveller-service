package com.travolish.traveller.review.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "reviews")
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
        FLAGGED       // Flagged for review
    }

    public enum ReviewType {
        HOTEL,
        ROOM
    }
}
