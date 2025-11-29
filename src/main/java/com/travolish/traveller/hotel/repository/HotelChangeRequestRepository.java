package com.travolish.traveller.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.hotel.model.HotelChangeRequest;

import java.util.List;

@Repository
public interface HotelChangeRequestRepository extends JpaRepository<HotelChangeRequest, Long> {
    List<HotelChangeRequest> findByStatus(HotelChangeRequest.RequestStatus status);
}
