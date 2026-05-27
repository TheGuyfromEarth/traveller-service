package com.travolish.traveller.hotel.service;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.Hotel;

public interface HotelService {
    List<Hotel> findAll();
    List<Hotel> findByHostId(Long hostId);
    Optional<Hotel> findById(Long id);
    Hotel create(Hotel hotel);
    Optional<Hotel> update(Long id, Hotel hotel);
    void delete(Long id);
    Optional<Hotel> updateImageUrl(Long id, String imageUrl);
    Optional<Hotel> updateVideoUrl(Long id, String videoUrl);
}
