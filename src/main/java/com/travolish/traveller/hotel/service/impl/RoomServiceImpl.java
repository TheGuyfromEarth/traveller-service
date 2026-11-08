package com.travolish.traveller.hotel.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.repository.RoomRepository;
import com.travolish.traveller.hotel.service.RoomService;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    @Override
    public List<Room> findByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    @Override
    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    @Override
    @CacheEvict(value = "hotel-search", allEntries = true)
    public Room create(Room room) {
        room.setId(null);
        return roomRepository.save(room);
    }

    @Override
    @CacheEvict(value = "hotel-search", allEntries = true)
    public Optional<Room> update(Long id, Room room) {
        return roomRepository.findById(id).map(existing -> {
            existing.setNumber(room.getNumber());
            existing.setType(room.getType());
            existing.setPricePerNight(room.getPricePerNight());
            existing.setAvailable(room.getAvailable());
            existing.setHotelId(room.getHotelId());
            return roomRepository.save(existing);
        });
    }

    @Override
    @CacheEvict(value = "hotel-search", allEntries = true)
    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}
