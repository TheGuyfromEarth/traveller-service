package com.travolish.traveller.analytics.repository;

import com.travolish.traveller.analytics.entity.HostEarnings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HostEarningsRepository extends JpaRepository<HostEarnings, Long> {
    
    @Query("SELECT he FROM HostEarnings he WHERE he.hostId = :hostId " +
           "ORDER BY he.checkInDate DESC")
    Page<HostEarnings> findByHostId(@Param("hostId") Long hostId, Pageable pageable);
    
    @Query("SELECT he FROM HostEarnings he WHERE he.hostId = :hostId " +
           "AND he.checkInDate >= :startDate AND he.checkOutDate <= :endDate")
    List<HostEarnings> findByHostIdAndDateRange(
        @Param("hostId") Long hostId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT he FROM HostEarnings he WHERE he.hostId = :hostId " +
           "AND he.status = :status")
    List<HostEarnings> findByHostIdAndStatus(
        @Param("hostId") Long hostId,
        @Param("status") String status
    );
    
    @Query("SELECT SUM(he.netEarnings) FROM HostEarnings he " +
           "WHERE he.hostId = :hostId AND he.status = 'PAID'")
    BigDecimal getTotalPaidEarnings(@Param("hostId") Long hostId);
    
    @Query("SELECT SUM(he.netEarnings) FROM HostEarnings he " +
           "WHERE he.hostId = :hostId AND he.status = 'EARNED'")
    BigDecimal getTotalEarnedButUnpaidEarnings(@Param("hostId") Long hostId);
    
    @Query("SELECT SUM(he.netEarnings) FROM HostEarnings he " +
           "WHERE he.hostId = :hostId AND MONTH(he.checkInDate) = :month " +
           "AND YEAR(he.checkInDate) = :year AND he.status IN ('EARNED', 'PAID')")
    BigDecimal getMonthlyEarnings(
        @Param("hostId") Long hostId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );
    
    @Query("SELECT COUNT(he) FROM HostEarnings he WHERE he.hostId = :hostId " +
           "AND he.status = 'PAID'")
    Integer getCompletedBookingsCount(@Param("hostId") Long hostId);
    
    @Query("SELECT he FROM HostEarnings he WHERE he.hostId = :hostId " +
           "AND he.guestId IN " +
           "(SELECT he2.guestId FROM HostEarnings he2 " +
           "WHERE he2.hostId = :hostId GROUP BY he2.guestId HAVING COUNT(*) > 1)")
    List<HostEarnings> findReturningGuestEarnings(@Param("hostId") Long hostId);
}
