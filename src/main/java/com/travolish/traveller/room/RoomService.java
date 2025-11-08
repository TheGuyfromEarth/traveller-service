package com.travolish.traveller.room;

import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<Room> findAll();
    Optional<Room> findById(Long id);
    Room create(Room room);
    Optional<Room> update(Long id, Room room);
    void delete(Long id);
}
