package com.travolish.traveller.hotel.service.impl;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.service.HotelService;
import org.springframework.stereotype.Service;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    public HotelServiceImpl(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Override
    public List<Hotel> findByHostId(Long hostId) {
        return hotelRepository.findByHostId(hostId);
    }

    @Override
    public Optional<Hotel> findById(Long id) {
        return hotelRepository.findById(id);
    }

    @Override
    public Hotel create(Hotel hotel) {
        hotel.setId(null);
        return hotelRepository.save(hotel);
    }

    @Override
    public Optional<Hotel> update(Long id, Hotel hotel) {
        return hotelRepository.findById(id).map(existing -> {
            if (hotel.getHostId() != null) existing.setHostId(hotel.getHostId());
            existing.setName(hotel.getName());
            existing.setAddress(hotel.getAddress());
            existing.setCity(hotel.getCity());
            existing.setCountry(hotel.getCountry());
            existing.setRating(hotel.getRating());
            existing.setPhone(hotel.getPhone());
            existing.setEmail(hotel.getEmail());
            existing.setDescription(hotel.getDescription());
            if (hotel.getImageUrl() != null) existing.setImageUrl(hotel.getImageUrl());
            if (hotel.getVideoUrl() != null) existing.setVideoUrl(hotel.getVideoUrl());
            return hotelRepository.save(existing);
        });
    }

    @Override
    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }

    @Override
    public Optional<Hotel> updateImageUrl(Long id, String imageUrl) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setImageUrl(imageUrl);
            return hotelRepository.save(existing);
        });
    }

    @Override
    public Optional<Hotel> updateVideoUrl(Long id, String videoUrl) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setVideoUrl(videoUrl);
            return hotelRepository.save(existing);
        });
    }
}
