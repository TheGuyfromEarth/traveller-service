package com.travolish.traveller.analytics.dto;

import java.math.BigDecimal;

public class OccupancyForecastDTO {
    private Integer daysAhead;
    private BigDecimal forecastedOccupancy;
    private Integer bookedNights;
    private Integer availableNights;
    private Integer recommendedPrice;
    private String priceRecommendation; // increase, decrease, maintain
    private Integer demandLevel; // 1-5 scale

    // Constructors
    public OccupancyForecastDTO() {}

    public OccupancyForecastDTO(Integer daysAhead, BigDecimal forecastedOccupancy, Integer bookedNights,
                               Integer availableNights, Integer recommendedPrice, String priceRecommendation,
                               Integer demandLevel) {
        this.daysAhead = daysAhead;
        this.forecastedOccupancy = forecastedOccupancy;
        this.bookedNights = bookedNights;
        this.availableNights = availableNights;
        this.recommendedPrice = recommendedPrice;
        this.priceRecommendation = priceRecommendation;
        this.demandLevel = demandLevel;
    }

    // Getters and Setters
    public Integer getDaysAhead() { return daysAhead; }
    public void setDaysAhead(Integer daysAhead) { this.daysAhead = daysAhead; }

    public BigDecimal getForecastedOccupancy() { return forecastedOccupancy; }
    public void setForecastedOccupancy(BigDecimal forecastedOccupancy) { this.forecastedOccupancy = forecastedOccupancy; }

    public Integer getBookedNights() { return bookedNights; }
    public void setBookedNights(Integer bookedNights) { this.bookedNights = bookedNights; }

    public Integer getAvailableNights() { return availableNights; }
    public void setAvailableNights(Integer availableNights) { this.availableNights = availableNights; }

    public Integer getRecommendedPrice() { return recommendedPrice; }
    public void setRecommendedPrice(Integer recommendedPrice) { this.recommendedPrice = recommendedPrice; }

    public String getPriceRecommendation() { return priceRecommendation; }
    public void setPriceRecommendation(String priceRecommendation) { this.priceRecommendation = priceRecommendation; }

    public Integer getDemandLevel() { return demandLevel; }
    public void setDemandLevel(Integer demandLevel) { this.demandLevel = demandLevel; }
}
