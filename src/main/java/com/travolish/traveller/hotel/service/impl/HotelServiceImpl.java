package com.travolish.traveller.hotel.service.impl;

import java.util.List;
import java.util.Optional;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.service.HotelService;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public HotelServiceImpl(HotelRepository hotelRepository, UserRepository userRepository,
                            NotificationService notificationService) {
        this.hotelRepository = hotelRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findByHostId(Long hostId) {
        return hotelRepository.findByHostId(hostId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hotels", key = "#id")
    public Optional<Hotel> findById(Long id) {
        return hotelRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotel-search", allEntries = true)
    public Hotel create(Hotel hotel) {
        hotel.setId(null);
        if (hotel.getName() == null || hotel.getName().isBlank()) {
            throw new IllegalArgumentException("Hotel name is required and must not be blank");
        }
        hotel.setStatus(Hotel.HotelStatus.PENDING_REVIEW);
        Hotel saved = hotelRepository.save(hotel);
        // Promote the host user to HOST role on first listing and send listing notification
        if (hotel.getHostId() != null) {
            userRepository.findById(hotel.getHostId()).ifPresent(host -> {
                if (!"HOST".equals(host.getRole()) && !"ADMIN".equals(host.getRole())) {
                    host.setRole("HOST");
                    userRepository.save(host);
                }
                // Notify the host that their listing is live
                try {
                    SendNotificationRequest req = new SendNotificationRequest();
                    req.setType(NotificationType.BOOKING_CONFIRMATION); // reuse as generic notification
                    req.setChannel(NotificationChannel.EMAIL);
                    req.setUserId(host.getId());
                    req.setRecipientEmail(host.getEmail());
                    req.setHotelId(saved.getId());
                    req.setSendImmediately(true);
                    req.setSubject("Listing submitted for review — " + saved.getName());
                    req.setMessage("Hi " + (host.getFirstName() != null ? host.getFirstName() : "Host") + ",\n\n"
                            + "Your property \"" + saved.getName() + "\" has been submitted to Travolish and is now pending admin review.\n\n"
                            + "Location: " + (saved.getCity() != null ? saved.getCity() : "—") + "\n"
                            + "Status:   Pending Review\n\n"
                            + "Our team will review your listing shortly. Once approved it will be visible to travellers.\n"
                            + "Make sure your availability calendar and room details are complete in the meantime.\n\n"
                            + "— The Travolish Team");
                    notificationService.sendNotificationAsync(req);
                } catch (Exception e) {
                    log.warn("Failed to send listing notification for hotel {}: {}", saved.getId(), e.getMessage());
                }
            });
        }
        return saved;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
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
            if (hotel.getHouseRules() != null) existing.setHouseRules(hotel.getHouseRules());
            if (hotel.getInstantBooking() != null) existing.setInstantBooking(hotel.getInstantBooking());
            if (hotel.getMinimumStay() != null) existing.setMinimumStay(hotel.getMinimumStay());
            if (hotel.getCheckInTime() != null) existing.setCheckInTime(hotel.getCheckInTime());
            if (hotel.getCheckOutTime() != null) existing.setCheckOutTime(hotel.getCheckOutTime());
            if (hotel.getStatus() != null) existing.setStatus(hotel.getStatus());
            if (hotel.getImageUrl() != null) existing.setImageUrl(hotel.getImageUrl());
            if (hotel.getVideoUrl() != null) existing.setVideoUrl(hotel.getVideoUrl());
            return hotelRepository.save(existing);
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotel-search", allEntries = true)
    public Hotel save(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public Optional<Hotel> updateImageUrl(Long id, String imageUrl) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setImageUrl(imageUrl);
            return hotelRepository.save(existing);
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public Optional<Hotel> updateVideoUrl(Long id, String videoUrl) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setVideoUrl(videoUrl);
            return hotelRepository.save(existing);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findByStatus(Hotel.HotelStatus status) {
        return hotelRepository.findByStatus(status);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public Optional<Hotel> updateStatus(Long id, Hotel.HotelStatus status) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setStatus(status);
            return hotelRepository.save(existing);
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public Optional<Hotel> updateStatus(Long id, Hotel.HotelStatus status, String adminNote) {
        return hotelRepository.findById(id).map(existing -> {
            existing.setStatus(status);
            if (adminNote != null && !adminNote.isBlank()) {
                existing.setAdminNote(adminNote);
            }
            return hotelRepository.save(existing);
        });
    }
}
