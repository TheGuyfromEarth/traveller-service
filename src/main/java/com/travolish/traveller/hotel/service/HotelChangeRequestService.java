package com.travolish.traveller.hotel.service;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.HotelChangeRequest;

public interface HotelChangeRequestService {
    HotelChangeRequest submit(HotelChangeRequest request);
    List<HotelChangeRequest> findAll();
    List<HotelChangeRequest> findByStatus(HotelChangeRequest.RequestStatus status);
    Optional<HotelChangeRequest> findById(Long id);
    HotelChangeRequest approve(Long id, String adminComment);
    HotelChangeRequest reject(Long id, String adminComment);
}
