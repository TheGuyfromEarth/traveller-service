package com.travolish.traveller.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingStatsDTO {
    private Long entityId; // hotelId or roomId
    private String entityType; // HOTEL or ROOM
    private Double averageRating;
    private Long totalReviews;
    private Long oneStar;
    private Long twoStars;
    private Long threeStars;
    private Long fourStars;
    private Long fiveStars;
    private Double fiveStarPercentage; // Percentage of reviews that are 5-star
}
