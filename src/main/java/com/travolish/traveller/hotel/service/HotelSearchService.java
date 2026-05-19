package com.travolish.traveller.hotel.service;

import java.util.List;
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

        List<HotelSearchResponse> responseList = hotelPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, hotelPage.getTotalElements());
    }

    private Specification<Hotel> buildSpecification(HotelSearchRequest searchRequest) {
        return HotelSpecification.withCountry(searchRequest.getCountry())
                .and(HotelSpecification.withCity(searchRequest.getCity()))
                .and(HotelSpecification.withName(searchRequest.getName()))
                .and(HotelSpecification.withMinRating(searchRequest.getMinRating()))
                .and(HotelSpecification.withMaxRating(searchRequest.getMaxRating()));
    }

    private HotelSearchResponse mapToResponse(Hotel hotel) {
        return HotelSearchResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .rating(hotel.getRating())
                .reviewCount(hotel.getReviews() != null ? hotel.getReviews().size() : 0)
                .phone(hotel.getPhone())
                .email(hotel.getEmail())
                .description(hotel.getDescription())
                .createdAt(hotel.getCreatedAt())
                .build();
    }

}
