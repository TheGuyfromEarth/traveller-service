package com.travolish.traveller.payout.repository;

import com.travolish.traveller.payout.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    
    @Query("SELECT p FROM Payout p WHERE p.hostId = :hostId AND p.isDeleted = false")
    List<Payout> findByHostId(@Param("hostId") Long hostId);
    
    @Query("SELECT p FROM Payout p WHERE p.hostId = :hostId AND p.payoutStatus = :status AND p.isDeleted = false")
    List<Payout> findByHostIdAndStatus(@Param("hostId") Long hostId, @Param("status") String status);
    
    @Query("SELECT p FROM Payout p WHERE p.hostId = :hostId AND p.isDeleted = false " +
           "ORDER BY p.requestedDate DESC")
    Page<Payout> findByHostIdPaginated(@Param("hostId") Long hostId, Pageable pageable);
    
    @Query("SELECT p FROM Payout p WHERE p.hostId = :hostId AND p.payoutStatus = :status " +
           "AND p.isDeleted = false ORDER BY p.requestedDate DESC")
    Page<Payout> findByHostIdAndStatusPaginated(
        @Param("hostId") Long hostId,
        @Param("status") String status,
        Pageable pageable
    );
    
    @Query("SELECT p FROM Payout p WHERE p.payoutStatus = :status AND p.isDeleted = false " +
           "ORDER BY p.requestedDate ASC")
    List<Payout> findByPayoutStatus(@Param("status") String status);
    
    @Query("SELECT p FROM Payout p WHERE p.requestedDate >= :startDate AND p.requestedDate <= :endDate " +
           "AND p.hostId = :hostId AND p.isDeleted = false")
    List<Payout> findByHostIdAndDateRange(
        @Param("hostId") Long hostId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT p FROM Payout p WHERE p.bankAccountId = :bankAccountId AND p.isDeleted = false")
    List<Payout> findByBankAccountId(@Param("bankAccountId") Long bankAccountId);
    
    @Query("SELECT p FROM Payout p WHERE p.transactionReference = :transactionReference")
    Optional<Payout> findByTransactionReference(@Param("transactionReference") String transactionReference);
    
    @Query("SELECT SUM(p.amount) FROM Payout p WHERE p.hostId = :hostId " +
           "AND p.payoutStatus = 'COMPLETED' AND p.isDeleted = false")
    BigDecimal sumCompletedPayoutsByHostId(@Param("hostId") Long hostId);
    
    @Query("SELECT SUM(p.amount) FROM Payout p WHERE p.hostId = :hostId " +
           "AND p.payoutStatus IN ('PENDING', 'APPROVED', 'PROCESSING') AND p.isDeleted = false")
    BigDecimal sumPendingPayoutsByHostId(@Param("hostId") Long hostId);
    
    @Query("SELECT COUNT(p) FROM Payout p WHERE p.payoutStatus = 'FAILED' " +
           "AND p.isDeleted = false AND p.retryCount < 3")
    Integer countFailedPayoutsNeedingRetry();
    
    @Query("SELECT p FROM Payout p WHERE p.payoutStatus = 'FAILED' " +
           "AND p.isDeleted = false AND p.retryCount < 3 ORDER BY p.lastRetryDate ASC")
    List<Payout> findFailedPayoutsForRetry();
    
    @Query("SELECT p FROM Payout p WHERE p.hostId = :hostId AND p.payoutStatus = 'COMPLETED' " +
           "AND p.isDeleted = false ORDER BY p.completedDate DESC LIMIT 1")
    Optional<Payout> findLastCompletedPayoutByHostId(@Param("hostId") Long hostId);
}
