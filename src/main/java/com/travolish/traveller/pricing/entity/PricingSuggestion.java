package com.travolish.traveller.pricing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "pricing_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private BigDecimal suggestedPrice;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private BigDecimal priceChange;

    @Column(nullable = false)
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingTrend trend;

    @Column(nullable = false)
    private LocalDate suggestedFromDate;

    @Column(nullable = false)
    private LocalDate suggestedToDate;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private Integer occupancyRate;

    private Integer competitorAvgPrice;

    private Integer demandLevel;

    public enum SuggestionReason {
        LOW_OCCUPANCY,
        HIGH_DEMAND,
        SEASONAL_TREND,
        COMPETITOR_ANALYSIS,
        MARKET_ADJUSTMENT,
        BOOKING_VELOCITY,
        CANCELLATION_RATE
    }

    public enum PricingTrend {
        INCREASE,
        DECREASE,
        STABLE
    }

    public enum SuggestionStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        APPLIED,
        EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
