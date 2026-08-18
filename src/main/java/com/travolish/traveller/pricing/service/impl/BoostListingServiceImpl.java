package com.travolish.traveller.pricing.service.impl;

import com.travolish.traveller.pricing.dto.BoostListingDTO;
import com.travolish.traveller.pricing.dto.BoostListingRequest;
import com.travolish.traveller.pricing.entity.BoostListing;
import com.travolish.traveller.pricing.repository.BoostListingRepository;
import com.travolish.traveller.pricing.service.BoostListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoostListingServiceImpl implements BoostListingService {

    private final BoostListingRepository boostListingRepository;

    private static final BigDecimal SILVER_COST = BigDecimal.valueOf(49.99);
    private static final BigDecimal GOLD_COST = BigDecimal.valueOf(99.99);
    private static final BigDecimal PLATINUM_COST = BigDecimal.valueOf(199.99);

    @Override
    public BoostListingDTO purchaseBoost(BoostListingRequest request) {
        log.info("Purchasing boost for room: {} with tier: {}", request.getRoomId(), request.getBoostTier());

        BigDecimal cost = calculateBoostCost(request.getBoostTier());
        Integer visibilityMultiplier = calculateVisibilityMultiplier(request.getBoostTier());

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(request.getDurationDays());

        BoostListing boost = BoostListing.builder()
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .boostType(BoostListing.BoostType.valueOf(request.getBoostType()))
                .boostTier(BoostListing.BoostTier.valueOf(request.getBoostTier()))
                .cost(cost)
                .durationDays(request.getDurationDays())
                .visibilityMultiplier(visibilityMultiplier)
                .status(BoostListing.BoostStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .impressionGain(0)
                .clickGain(0)
                .bookingGain(0)
                .build();

        BoostListing saved = boostListingRepository.save(boost);
        log.info("Boost purchased successfully with ID: {}", saved.getId());

        return mapToDTO(saved);
    }

    @Override
    public BoostListingDTO getBoostById(Long boostId) {
        BoostListing boost = boostListingRepository.findById(boostId)
                .orElseThrow(() -> new RuntimeException("Boost not found"));
        return mapToDTO(boost);
    }

    @Override
    public List<BoostListingDTO> getActiveBoostsForHotel(Long hotelId) {
        return boostListingRepository.findActiveBoostsForHotel(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BoostListingDTO> getBoostsForHotel(Long hotelId, Pageable pageable) {
        return boostListingRepository.findByHotelIdAndStatus(hotelId, "ACTIVE", pageable)
                .map(this::mapToDTO);
    }

    @Override
    public BoostListingDTO cancelBoost(Long boostId, String reason) {
        BoostListing boost = boostListingRepository.findById(boostId)
                .orElseThrow(() -> new RuntimeException("Boost not found"));

        boost.setStatus(BoostListing.BoostStatus.CANCELLED);
        boost.setCancelledAt(LocalDateTime.now());
        boost.setCancellationReason(reason);

        BoostListing saved = boostListingRepository.save(boost);
        log.info("Boost {} cancelled: {}", boostId, reason);

        return mapToDTO(saved);
    }

    @Override
    public List<BoostListingDTO> findExpiredBoosts() {
        return boostListingRepository.findExpiredBoosts()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BoostListingDTO updateBoostStatus(Long boostId, String newStatus) {
        BoostListing boost = boostListingRepository.findById(boostId)
                .orElseThrow(() -> new RuntimeException("Boost not found"));

        boost.setStatus(BoostListing.BoostStatus.valueOf(newStatus));
        BoostListing saved = boostListingRepository.save(boost);

        log.info("Boost {} status updated to: {}", boostId, newStatus);

        return mapToDTO(saved);
    }

    @Override
    public List<BoostListingDTO> getBoostAnalytics(Long hotelId) {
        return boostListingRepository.findByHotelId(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isListingBoosted(Long roomId) {
        List<BoostListing> activeBoosts = boostListingRepository.findByRoomId(roomId);
        return activeBoosts.stream().anyMatch(b -> b.getStatus() == BoostListing.BoostStatus.ACTIVE && b.getEndDate().isAfter(LocalDateTime.now()));
    }

    private BigDecimal calculateBoostCost(String tier) {
        return switch (tier) {
            case "SILVER" -> SILVER_COST;
            case "GOLD" -> GOLD_COST;
            case "PLATINUM" -> PLATINUM_COST;
            default -> SILVER_COST;
        };
    }

    private Integer calculateVisibilityMultiplier(String tier) {
        return switch (tier) {
            case "SILVER" -> 2;
            case "GOLD" -> 5;
            case "PLATINUM" -> 10;
            default -> 1;
        };
    }

    private BoostListingDTO mapToDTO(BoostListing boost) {
        return BoostListingDTO.builder()
                .id(boost.getId())
                .hotelId(boost.getHotelId())
                .roomId(boost.getRoomId())
                .boostType(boost.getBoostType().toString())
                .boostTier(boost.getBoostTier().toString())
                .cost(boost.getCost())
                .durationDays(boost.getDurationDays())
                .visibilityMultiplier(boost.getVisibilityMultiplier())
                .status(boost.getStatus().toString())
                .startDate(boost.getStartDate())
                .endDate(boost.getEndDate())
                .impressionGain(boost.getImpressionGain())
                .clickGain(boost.getClickGain())
                .bookingGain(boost.getBookingGain())
                .build();
    }
}
