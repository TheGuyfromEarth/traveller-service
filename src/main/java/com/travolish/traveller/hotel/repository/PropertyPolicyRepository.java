package com.travolish.traveller.hotel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travolish.traveller.hotel.model.PropertyPolicy;

public interface PropertyPolicyRepository extends JpaRepository<PropertyPolicy, Long> {
    Optional<PropertyPolicy> findByHotelId(Long hotelId);
}
