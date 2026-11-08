package com.travolish.traveller.payment.repository;

import com.travolish.traveller.payment.entity.Receipt;
import com.travolish.traveller.payment.entity.ReceiptStatus;
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
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    // Find by payment ID
    Optional<Receipt> findByPaymentId(Long paymentId);
    
    // Find by user ID
    List<Receipt> findByUserId(Long userId);
    Page<Receipt> findByUserId(Long userId, Pageable pageable);
    
    // Find by booking ID
    Optional<Receipt> findByBookingId(Long bookingId);
    
    // Find by receipt number
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    
    // Find by status
    List<Receipt> findByReceiptStatus(ReceiptStatus status);
    Page<Receipt> findByReceiptStatus(ReceiptStatus status, Pageable pageable);
    
    // Find by user and status
    Page<Receipt> findByUserIdAndReceiptStatus(Long userId, ReceiptStatus status, Pageable pageable);
    
    // Find by user and date range
    @Query("SELECT r FROM Receipt r WHERE r.userId = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    List<Receipt> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find receipts pending generation
    @Query("SELECT r FROM Receipt r WHERE r.receiptStatus = 'DRAFT' ORDER BY r.createdAt ASC")
    List<Receipt> findPendingReceipts();
    
    // Find receipts not sent
    @Query("SELECT r FROM Receipt r WHERE r.receiptStatus = 'GENERATED' AND r.generatedAt < :cutoffTime")
    List<Receipt> findUnsentReceipts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Count by user and status
    Long countByUserIdAndReceiptStatus(Long userId, ReceiptStatus status);
}
