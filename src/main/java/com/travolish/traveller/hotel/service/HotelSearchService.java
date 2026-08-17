package com.travolish.traveller.hotel.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.hotel.dto.HotelSearchRequest;
import com.travolish.traveller.hotel.dto.HotelSearchResponse;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.repository.RoomRepository;
import com.travolish.traveller.hotel.specification.HotelSpecification;

@Service
public class HotelSearchService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelSearchService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    // HotelSearchRequest is a Lombok @Data class — equals/hashCode cover all fields,
    // so Spring's default SimpleKeyGenerator produces a correct per-combination key.
    // Cache is evicted by HotelServiceImpl on any create / update / delete.
    @Cacheable(value = "hotel-search")
    @Transactional(readOnly = true)
    public Page<HotelSearchResponse> searchHotels(HotelSearchRequest searchRequest) {
        Specification<Hotel> spec = buildSpecification(searchRequest)
                .and(HotelSpecification.withStatus(Hotel.HotelStatus.LIVE));
        Pageable pageable = PageRequest.of(searchRequest.getPageNumber(), searchRequest.getPageSize());
        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);

        List<Hotel> hotels = hotelPage.getContent();
        List<Long> ids = hotels.stream().map(Hotel::getId).collect(Collectors.toList());

        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Review counts — single batch query
        Map<Long, Integer> reviewCounts = hotelRepository.countReviewsByHotelIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // Cheapest available room price per hotel — single GROUP BY query.
        // This lets the frontend display prices on search cards without calling
        // GET /api/rooms (which previously dumped the entire rooms table).
        Map<Long, Double> priceMap = roomRepository.findCheapestPriceByHotelIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[1] != null ? ((Number) row[1]).doubleValue() : null
                ));

        List<HotelSearchResponse> responseList = hotels.stream()
                .map(h -> mapToResponse(h,
                        reviewCounts.getOrDefault(h.getId(), 0),
                        priceMap.get(h.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, hotelPage.getTotalElements());
    }

    private Specification<Hotel> buildSpecification(HotelSearchRequest searchRequest) {
        return HotelSpecification.withQuery(searchRequest.getQuery())
                .and(HotelSpecification.withCountry(searchRequest.getCountry()))
                .and(HotelSpecification.withCity(searchRequest.getCity()))
                .and(HotelSpecification.withName(searchRequest.getName()))
                .and(HotelSpecification.withMinRating(searchRequest.getMinRating()))
                .and(HotelSpecification.withMaxRating(searchRequest.getMaxRating()))
                .and(HotelSpecification.withBbox(
                        searchRequest.getLatMin(), searchRequest.getLatMax(),
                        searchRequest.getLngMin(), searchRequest.getLngMax()));
    }

    private HotelSearchResponse mapToResponse(Hotel hotel, int reviewCount, Double cheapestRoomPrice) {
        return HotelSearchResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .rating(hotel.getRating())
                .reviewCount(reviewCount)
                .phone(hotel.getPhone())
                .email(hotel.getEmail())
                .description(hotel.getDescription())
                .latitude(hotel.getLatitude())
                .longitude(hotel.getLongitude())
                .imageUrl(hotel.getImageUrl())
                .videoUrl(hotel.getVideoUrl())
                .amenities(hotel.getAmenities())
                .maxGuests(hotel.getMaxGuests())
                .instantBooking(hotel.getInstantBooking())
                .minimumStay(hotel.getMinimumStay())
                .checkInTime(hotel.getCheckInTime())
                .checkOutTime(hotel.getCheckOutTime())
                .cheapestRoomPrice(cheapestRoomPrice)
                .createdAt(hotel.getCreatedAt())
                .build();
    }

}
