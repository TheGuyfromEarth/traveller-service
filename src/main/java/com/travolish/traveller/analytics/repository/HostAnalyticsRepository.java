package com.travolish.traveller.analytics.repository;

import com.travolish.traveller.analytics.entity.HostAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HostAnalyticsRepository extends JpaRepository<HostAnalytics, Long> {
    
    @Query("SELECT ha FROM HostAnalytics ha WHERE ha.hostId = :hostId " +
           "AND ha.analyticsDate = :date")
    Optional<HostAnalytics> findByHostIdAndDate(
        @Param("hostId") Long hostId,
        @Param("date") LocalDate date
    );
    
    @Query("SELECT ha FROM HostAnalytics ha WHERE ha.hostId = :hostId " +
           "AND ha.analyticsDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ha.analyticsDate DESC")
    List<HostAnalytics> findByHostIdAndDateRange(
        @Param("hostId") Long hostId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT ha FROM HostAnalytics ha WHERE ha.hostId = :hostId " +
           "AND ha.periodStartDate = :periodStart AND ha.periodEndDate = :periodEnd")
    Optional<HostAnalytics> findByHostIdAndPeriod(
        @Param("hostId") Long hostId,
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd") LocalDate periodEnd
    );
    
    @Query("SELECT ha FROM HostAnalytics ha WHERE ha.hostId = :hostId " +
           "ORDER BY ha.analyticsDate DESC LIMIT 30")
    List<HostAnalytics> findLatestAnalytics(@Param("hostId") Long hostId);
}
