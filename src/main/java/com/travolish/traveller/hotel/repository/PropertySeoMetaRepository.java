package com.travolish.traveller.hotel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.travolish.traveller.hotel.model.PropertySeoMeta;

public interface PropertySeoMetaRepository extends JpaRepository<PropertySeoMeta, Long> {
    Optional<PropertySeoMeta> findByHotelId(Long hotelId);
    Optional<PropertySeoMeta> findByUrlSlug(String urlSlug);
}
