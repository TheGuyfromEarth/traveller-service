package com.travolish.traveller.hotel.service;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.Room;

public interface RoomService {
    List<Room> findAll();
    List<Room> findByHotelId(Long hotelId);
    Optional<Room> findById(Long id);
    Room create(Room room);
    Optional<Room> update(Long id, Room room);
    void delete(Long id);
}
