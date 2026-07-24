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
            // Identity
            if (hotel.getHostId() != null) existing.setHostId(hotel.getHostId());

            // Basics
            existing.setName(hotel.getName());
            existing.setDescription(hotel.getDescription());
            if (hotel.getHouseRules() != null) existing.setHouseRules(hotel.getHouseRules());
            if (hotel.getCategory() != null) existing.setCategory(hotel.getCategory());
            if (hotel.getSubTypes() != null) existing.setSubTypes(hotel.getSubTypes());
            if (hotel.getTargetGuests() != null) existing.setTargetGuests(hotel.getTargetGuests());
            if (hotel.getStayType() != null) existing.setStayType(hotel.getStayType());
            if (hotel.getStarRating() != null) existing.setStarRating(hotel.getStarRating());
            if (hotel.getBrand() != null) existing.setBrand(hotel.getBrand());
            if (hotel.getLanguagesSpoken() != null) existing.setLanguagesSpoken(hotel.getLanguagesSpoken());
            if (hotel.getYearBuilt() != null) existing.setYearBuilt(hotel.getYearBuilt());
            if (hotel.getLastRenovated() != null) existing.setLastRenovated(hotel.getLastRenovated());

            // Location
            existing.setAddress(hotel.getAddress());
            existing.setCity(hotel.getCity());
            if (hotel.getState() != null) existing.setState(hotel.getState());
            existing.setCountry(hotel.getCountry());
            existing.setPostalCode(hotel.getPostalCode());
            if (hotel.getLatitude() != null) existing.setLatitude(hotel.getLatitude());
            if (hotel.getLongitude() != null) existing.setLongitude(hotel.getLongitude());
            if (hotel.getDistanceToAirport() != null) existing.setDistanceToAirport(hotel.getDistanceToAirport());
            if (hotel.getDistanceToTrain() != null) existing.setDistanceToTrain(hotel.getDistanceToTrain());
            if (hotel.getDistanceToCityCentre() != null) existing.setDistanceToCityCentre(hotel.getDistanceToCityCentre());
            if (hotel.getDistanceToBeach() != null) existing.setDistanceToBeach(hotel.getDistanceToBeach());

            // Capacity
            if (hotel.getMaxGuests() != null) existing.setMaxGuests(hotel.getMaxGuests());
            if (hotel.getNumBedrooms() != null) existing.setNumBedrooms(hotel.getNumBedrooms());
            if (hotel.getNumBathrooms() != null) existing.setNumBathrooms(hotel.getNumBathrooms());
            if (hotel.getNumUnits() != null) existing.setNumUnits(hotel.getNumUnits());

            // Property details
            if (hotel.getTotalRooms() != null) existing.setTotalRooms(hotel.getTotalRooms());
            if (hotel.getTotalFloors() != null) existing.setTotalFloors(hotel.getTotalFloors());
            if (hotel.getTotalBuildings() != null) existing.setTotalBuildings(hotel.getTotalBuildings());
            if (hotel.getPropertySize() != null) existing.setPropertySize(hotel.getPropertySize());
            if (hotel.getReceptionHours() != null) existing.setReceptionHours(hotel.getReceptionHours());
            if (hotel.getTwentyFourHourFrontDesk() != null) existing.setTwentyFourHourFrontDesk(hotel.getTwentyFourHourFrontDesk());
            if (hotel.getThreeSixtyTourUrl() != null) existing.setThreeSixtyTourUrl(hotel.getThreeSixtyTourUrl());

            // Bed details
            if (hotel.getPrimaryBedType() != null) existing.setPrimaryBedType(hotel.getPrimaryBedType());
            if (hotel.getSecondaryBedType() != null) existing.setSecondaryBedType(hotel.getSecondaryBedType());

            // Booking settings
            if (hotel.getInstantBooking() != null) existing.setInstantBooking(hotel.getInstantBooking());
            if (hotel.getMinimumStay() != null) existing.setMinimumStay(hotel.getMinimumStay());
            if (hotel.getMaximumStay() != null) existing.setMaximumStay(hotel.getMaximumStay());
            if (hotel.getBookingWindow() != null) existing.setBookingWindow(hotel.getBookingWindow());
            if (hotel.getLastMinuteBooking() != null) existing.setLastMinuteBooking(hotel.getLastMinuteBooking());
            if (hotel.getLastMinuteCutoffHours() != null) existing.setLastMinuteCutoffHours(hotel.getLastMinuteCutoffHours());
            if (hotel.getSameDayBooking() != null) existing.setSameDayBooking(hotel.getSameDayBooking());
            if (hotel.getCheckInTime() != null) existing.setCheckInTime(hotel.getCheckInTime());
            if (hotel.getCheckOutTime() != null) existing.setCheckOutTime(hotel.getCheckOutTime());

            // Amenities & services
            if (hotel.getAmenities() != null) existing.setAmenities(hotel.getAmenities());
            if (hotel.getMealOptions() != null) existing.setMealOptions(hotel.getMealOptions());
            if (hotel.getTransportationOptions() != null) existing.setTransportationOptions(hotel.getTransportationOptions());
            if (hotel.getGuestServices() != null) existing.setGuestServices(hotel.getGuestServices());
            if (hotel.getSustainabilityFeatures() != null) existing.setSustainabilityFeatures(hotel.getSustainabilityFeatures());

            // Contact
            existing.setRating(hotel.getRating());
            existing.setPhone(hotel.getPhone());
            existing.setEmail(hotel.getEmail());
            if (hotel.getContactPerson() != null) existing.setContactPerson(hotel.getContactPerson());
            if (hotel.getWebsiteUrl() != null) existing.setWebsiteUrl(hotel.getWebsiteUrl());
            if (hotel.getEmergencyContact() != null) existing.setEmergencyContact(hotel.getEmergencyContact());

            // AI & SEO
            if (hotel.getTargetAudience() != null) existing.setTargetAudience(hotel.getTargetAudience());
            if (hotel.getUsp() != null) existing.setUsp(hotel.getUsp());
            if (hotel.getNearbyLandmark() != null) existing.setNearbyLandmark(hotel.getNearbyLandmark());
            if (hotel.getAiTranslation() != null) existing.setAiTranslation(hotel.getAiTranslation());

            // Media
            if (hotel.getImageUrl() != null) existing.setImageUrl(hotel.getImageUrl());
            if (hotel.getVideoUrl() != null) existing.setVideoUrl(hotel.getVideoUrl());

            // Status (host-facing; admin note)
            if (hotel.getStatus() != null) {
                // Guard: DRAFT listings cannot jump directly to LIVE — they must go through
                // PENDING_REVIEW via the submit-for-review endpoint first.
                if (existing.getStatus() == Hotel.HotelStatus.DRAFT
                        && hotel.getStatus() == Hotel.HotelStatus.LIVE) {
                    throw new IllegalStateException(
                            "A DRAFT listing must be submitted for review before it can go LIVE.");
                }
                existing.setStatus(hotel.getStatus());
            }
            if (hotel.getAdminNote() != null) existing.setAdminNote(hotel.getAdminNote());

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
            Hotel saved = hotelRepository.save(existing);
            sendStatusChangeNotification(saved, status, adminNote);
            return saved;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hotels", key = "#id"),
        @CacheEvict(value = "hotel-search", allEntries = true)
    })
    public Optional<Hotel> submitForReview(Long id) {
        return hotelRepository.findById(id).map(existing -> {
            if (existing.getStatus() != Hotel.HotelStatus.DRAFT) {
                throw new IllegalStateException(
                        "Only DRAFT listings can be submitted for review. Current status: " + existing.getStatus());
            }
            existing.setStatus(Hotel.HotelStatus.PENDING_REVIEW);
            Hotel saved = hotelRepository.save(existing);
            sendStatusChangeNotification(saved, Hotel.HotelStatus.PENDING_REVIEW, null);
            return saved;
        });
    }

    private void sendStatusChangeNotification(Hotel hotel, Hotel.HotelStatus newStatus, String adminNote) {
        if (hotel.getHostId() == null) return;
        userRepository.findById(hotel.getHostId()).ifPresent(host -> {
            if (host.getEmail() == null) return;
            String firstName = host.getFirstName() != null ? host.getFirstName() : "Host";
            String subject;
            String message;
            switch (newStatus) {
                case LIVE -> {
                    subject = "Your listing is now live — " + hotel.getName();
                    message = "Hi " + firstName + ",\n\n"
                            + "Great news! Your property \"" + hotel.getName() + "\" has been approved "
                            + "and is now live on Travolish.\n\n"
                            + "Travellers can now find and book your property.\n\n"
                            + "— The Travolish Team";
                }
                case DRAFT -> {
                    subject = "Listing update required — " + hotel.getName();
                    String reason = (adminNote != null && !adminNote.isBlank())
                            ? adminNote
                            : "Please review your listing and make the necessary updates.";
                    message = "Hi " + firstName + ",\n\n"
                            + "Your property \"" + hotel.getName() + "\" has been returned to Draft "
                            + "and requires your attention.\n\n"
                            + "Reason: " + reason + "\n\n"
                            + "Please log in to the Host Portal, review the feedback, update your listing, "
                            + "and resubmit for review.\n\n"
                            + "— The Travolish Team";
                }
                case PENDING_REVIEW -> {
                    subject = "Listing submitted for review — " + hotel.getName();
                    message = "Hi " + firstName + ",\n\n"
                            + "Your property \"" + hotel.getName() + "\" has been submitted for review.\n\n"
                            + "Our team will review your listing and notify you once a decision is made.\n\n"
                            + "— The Travolish Team";
                }
                default -> { return; }
            }
            try {
                SendNotificationRequest req = new SendNotificationRequest();
                req.setType(NotificationType.BOOKING_CONFIRMATION);
                req.setChannel(NotificationChannel.EMAIL);
                req.setUserId(host.getId());
                req.setRecipientEmail(host.getEmail());
                req.setHotelId(hotel.getId());
                req.setSendImmediately(true);
                req.setSubject(subject);
                req.setMessage(message);
                notificationService.sendNotificationAsync(req);
            } catch (Exception e) {
                log.warn("Failed to send status-change notification for hotel {}: {}", hotel.getId(), e.getMessage());
            }
        });
    }
}
