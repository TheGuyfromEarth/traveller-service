package com.travolish.traveller.payment.repository;

import com.travolish.traveller.payment.entity.Refund;
import com.travolish.traveller.payment.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    // Find by payment ID
    List<Refund> findByPaymentId(Long paymentId);
    Optional<Refund> findFirstByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    
    // Find by user ID
    List<Refund> findByUserId(Long userId);
    Page<Refund> findByUserId(Long userId, Pageable pageable);
    
    // Find by booking ID
    List<Refund> findByBookingId(Long bookingId);
    
    // Find by status
    List<Refund> findByRefundStatus(RefundStatus status);
    Page<Refund> findByRefundStatus(RefundStatus status, Pageable pageable);
    
    // Find by user and status
    Page<Refund> findByUserIdAndRefundStatus(Long userId, RefundStatus status, Pageable pageable);
    
    // Find by razorpay refund ID
    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);
    
    // Find refunds needing retry
    @Query("SELECT r FROM Refund r WHERE r.refundStatus = 'PROCESSING' AND r.retryCount < 3 AND r.lastRetryAt < :retryTime")
    List<Refund> findRefundsNeedingRetry(@Param("retryTime") LocalDateTime retryTime);
    
    // Count pending refunds
    Long countByRefundStatus(RefundStatus status);
    
    // Sum refunded amount by user
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r WHERE r.userId = :userId AND r.refundStatus = 'COMPLETED'")
    java.math.BigDecimal sumRefundedAmountByUserId(@Param("userId") Long userId);
    
    // Find by date range
    @Query("SELECT r FROM Refund r WHERE r.userId = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    List<Refund> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
