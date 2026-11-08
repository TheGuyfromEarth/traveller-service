package com.travolish.traveller.hotel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travolish.traveller.hotel.model.NearbyAttraction;

public interface NearbyAttractionRepository extends JpaRepository<NearbyAttraction, Long> {
    List<NearbyAttraction> findByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);
}
