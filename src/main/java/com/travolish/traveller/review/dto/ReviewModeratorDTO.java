package com.travolish.traveller.review.dto;

import java.time.OffsetDateTime;

import com.travolish.traveller.review.model.Review.ReviewStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModeratorDTO {
    private Long id;
    private Long userId;
    private Long hotelId;
    private Long roomId;
    private String title;
    private String content;
    private Integer rating;
    private ReviewStatus status;
    private String moderatorNotes;
    private Long moderatorId;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
}
