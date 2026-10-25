package com.travolish.traveller.hotel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travolish.traveller.hotel.model.PropertyContact;

public interface PropertyContactRepository extends JpaRepository<PropertyContact, Long> {
    Optional<PropertyContact> findByHotelId(Long hotelId);
}
