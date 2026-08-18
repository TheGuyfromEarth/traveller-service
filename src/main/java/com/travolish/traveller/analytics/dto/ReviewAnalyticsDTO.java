package com.travolish.traveller.analytics.dto;

import java.math.BigDecimal;

public class ReviewAnalyticsDTO {
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer fiveStarCount;
    private Integer fourStarCount;
    private Integer threeStarCount;
    private Integer twoStarCount;
    private Integer oneStarCount;
    private Integer reviewsThisMonth;
    private BigDecimal ratingTrend;

    public ReviewAnalyticsDTO() {}

    public ReviewAnalyticsDTO(BigDecimal averageRating, Integer totalReviews, Integer fiveStarCount,
                             Integer fourStarCount, Integer threeStarCount, Integer twoStarCount,
                             Integer oneStarCount, Integer reviewsThisMonth, BigDecimal ratingTrend) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.fiveStarCount = fiveStarCount;
        this.fourStarCount = fourStarCount;
        this.threeStarCount = threeStarCount;
        this.twoStarCount = twoStarCount;
        this.oneStarCount = oneStarCount;
        this.reviewsThisMonth = reviewsThisMonth;
        this.ratingTrend = ratingTrend;
    }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(Integer fiveStarCount) { this.fiveStarCount = fiveStarCount; }

    public Integer getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(Integer fourStarCount) { this.fourStarCount = fourStarCount; }

    public Integer getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(Integer threeStarCount) { this.threeStarCount = threeStarCount; }

    public Integer getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(Integer twoStarCount) { this.twoStarCount = twoStarCount; }

    public Integer getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(Integer oneStarCount) { this.oneStarCount = oneStarCount; }

    public Integer getReviewsThisMonth() { return reviewsThisMonth; }
    public void setReviewsThisMonth(Integer reviewsThisMonth) { this.reviewsThisMonth = reviewsThisMonth; }

    public BigDecimal getRatingTrend() { return ratingTrend; }
    public void setRatingTrend(BigDecimal ratingTrend) { this.ratingTrend = ratingTrend; }
}
