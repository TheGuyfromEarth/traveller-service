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
    private String title;
    private String content;
    private Integer rating;
    private ReviewStatus status;
    private ReviewType reviewType;
    private Integer helpfulCount;
    private Integer unhelpfulCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
