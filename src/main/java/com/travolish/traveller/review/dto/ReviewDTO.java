package com.travolish.traveller.review.dto;

import java.time.OffsetDateTime;

import com.travolish.traveller.review.model.Review.ReviewStatus;
import com.travolish.traveller.review.model.Review.ReviewType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    private Long id;
    private Long userId;
    private Long hotelId;
    private Long roomId;
    private Long guestId;
    private Long bookingId;
    private String title;
    private String content;
    private Integer rating;
    private ReviewStatus status;
    private ReviewType reviewType;
    private Integer helpfulCount;
    private Integer unhelpfulCount;
    // Guest-review sub-ratings
    // Hotel review category sub-ratings (1–5)
    private Integer cleanlinessRating;
    private Integer accuracyRating;
    private Integer communicationRating;
    private Integer locationRating;
    private Integer checkInRating;
    private Integer valueRating;
    // Guest-review sub-ratings
    private Integer theftRating;
    private Integer behaviorRating;
    // Comma-separated highlight tags
    private String tags;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
