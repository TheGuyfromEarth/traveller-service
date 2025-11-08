package com.travolish.traveller.hotel.service;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.Hotel;

public interface HotelService {
    List<Hotel> findAll();
    Optional<Hotel> findById(Long id);
    Hotel create(Hotel hotel);
    Optional<Hotel> update(Long id, Hotel hotel);
    void delete(Long id);
}
