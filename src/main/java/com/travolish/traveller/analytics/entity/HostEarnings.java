package com.travolish.traveller.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "host_earnings", indexes = {
    @Index(name = "idx_host_id_earnings", columnList = "host_id"),
    @Index(name = "idx_booking_id", columnList = "booking_id"),
    @Index(name = "idx_earnings_date", columnList = "earnings_date"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostEarnings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;
    
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    
    @Column(name = "guest_id", nullable = false)
    private Long guestId;
    
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;
    
    @Column(name = "number_of_nights")
    private Integer numberOfNights;
    
    // Revenue breakdown
    @Column(name = "base_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal baseAmount;
    
    @Column(name = "taxes_and_fees", precision = 15, scale = 2)
    private BigDecimal taxesAndFees = BigDecimal.ZERO;
    
    @Column(name = "gross_earnings", precision = 15, scale = 2, nullable = false)
    private BigDecimal grossEarnings;
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;
    
    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    
    @Column(name = "discounts_applied", precision = 15, scale = 2)
    private BigDecimal discountsApplied = BigDecimal.ZERO;
    
    @Column(name = "net_earnings", precision = 15, scale = 2, nullable = false)
    private BigDecimal netEarnings;
    
    // Status tracking
    @Column(name = "status", nullable = false)
    private String status; // PENDING, EARNED, PAID, REFUNDED
    
    @Column(name = "earned_date")
    private LocalDate earnedDate;
    
    @Column(name = "paid_date")
    private LocalDate paidDate;
    
    @Column(name = "payout_id")
    private Long payoutId;
    
    // Additional info
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(Integer numberOfNights) { this.numberOfNights = numberOfNights; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getTaxesAndFees() { return taxesAndFees; }
    public void setTaxesAndFees(BigDecimal taxesAndFees) { this.taxesAndFees = taxesAndFees; }

    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public BigDecimal getDiscountsApplied() { return discountsApplied; }
    public void setDiscountsApplied(BigDecimal discountsApplied) { this.discountsApplied = discountsApplied; }

    public BigDecimal getNetEarnings() { return netEarnings; }
    public void setNetEarnings(BigDecimal netEarnings) { this.netEarnings = netEarnings; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDate earnedDate) { this.earnedDate = earnedDate; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long payoutId) { this.payoutId = payoutId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
