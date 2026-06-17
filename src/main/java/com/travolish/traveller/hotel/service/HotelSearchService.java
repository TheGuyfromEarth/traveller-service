package com.travolish.traveller.hotel.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.travolish.traveller.hotel.specification.HotelSpecification;

@Service
public class HotelSearchService {

    private final HotelRepository hotelRepository;

    public HotelSearchService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Transactional(readOnly = true)
    public Page<HotelSearchResponse> searchHotels(HotelSearchRequest searchRequest) {
        Specification<Hotel> spec = buildSpecification(searchRequest);
        Pageable pageable = PageRequest.of(searchRequest.getPageNumber(), searchRequest.getPageSize());
        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);

        List<Hotel> hotels = hotelPage.getContent();
        List<Long> ids = hotels.stream().map(Hotel::getId).collect(Collectors.toList());
        Map<Long, Integer> reviewCounts = hotelRepository.countReviewsByHotelIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        List<HotelSearchResponse> responseList = hotels.stream()
                .map(h -> mapToResponse(h, reviewCounts.getOrDefault(h.getId(), 0)))
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

    private HotelSearchResponse mapToResponse(Hotel hotel, int reviewCount) {
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
                .createdAt(hotel.getCreatedAt())
                .build();
    }

}
