package com.travolish.traveller.hotel.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.hotel.model.HotelChangeRequest;

import java.util.List;

@Repository
public interface HotelChangeRequestRepository extends JpaRepository<HotelChangeRequest, Long> {
    List<HotelChangeRequest> findByStatus(HotelChangeRequest.RequestStatus status);

    long countByStatus(HotelChangeRequest.RequestStatus status);

    @Query("SELECT r FROM HotelChangeRequest r WHERE r.status IN :statuses AND r.processedAt IS NOT NULL ORDER BY r.processedAt DESC")
    List<HotelChangeRequest> findRecentProcessed(@Param("statuses") List<HotelChangeRequest.RequestStatus> statuses, Pageable pageable);
}
