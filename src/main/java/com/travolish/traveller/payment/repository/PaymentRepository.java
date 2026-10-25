package com.travolish.traveller.payment.repository;

import com.travolish.traveller.payment.entity.Payment;
import com.travolish.traveller.payment.entity.PaymentStatus;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find by user
    List<Payment> findByUserId(Long userId);
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    
    // Find by booking
    Optional<Payment> findByBookingId(Long bookingId);
    List<Payment> findByBookingIdAndPaymentStatus(Long bookingId, PaymentStatus status);
    
    // Find by status
    List<Payment> findByPaymentStatus(PaymentStatus status);
    Page<Payment> findByPaymentStatus(PaymentStatus status, Pageable pageable);
    
    // Find by user and status
    Page<Payment> findByUserIdAndPaymentStatus(Long userId, PaymentStatus status, Pageable pageable);
    
    // Find by transaction reference
    Optional<Payment> findByTransactionReference(String transactionReference);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    
    // Find by date range
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Sum completed payments for user
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.userId = :userId AND p.paymentStatus = 'COMPLETED'")
    Long sumCompletedPaymentsByUserId(@Param("userId") Long userId);
    
    // Sum failed payments for retry
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = 'FAILED' AND p.retryCount < 3")
    Long countFailedPaymentsForRetry();
    
    // Find failed payments eligible for retry
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'FAILED' AND p.retryCount < 3 ORDER BY p.lastRetryAt ASC")
    List<Payment> findFailedPaymentsForRetry();
    
    // Find pending payments
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'PENDING' AND p.createdAt < :expiryTime")
    List<Payment> findExpiredPendingPayments(@Param("expiryTime") LocalDateTime expiryTime);
    
    // Count by status
    Long countByUserIdAndPaymentStatus(Long userId, PaymentStatus status);
    
    // Find last successful payment for user
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.paymentStatus = 'COMPLETED' ORDER BY p.completedAt DESC LIMIT 1")
    Optional<Payment> findLastSuccessfulPaymentByUserId(@Param("userId") Long userId);

    // Find the most recent completed payment for a booking — used when processing cancellation refunds
    Optional<Payment> findTopByBookingIdAndPaymentStatusOrderByCompletedAtDesc(Long bookingId, PaymentStatus status);
}
