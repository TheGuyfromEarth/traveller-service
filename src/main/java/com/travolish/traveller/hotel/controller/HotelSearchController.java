package com.travolish.traveller.hotel.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.travolish.traveller.hotel.dto.HotelSearchRequest;
import com.travolish.traveller.hotel.dto.HotelSearchResponse;
import com.travolish.traveller.hotel.service.HotelSearchService;

@RestController
@RequestMapping("/api/hotels/search")
public class HotelSearchController {

    private final HotelSearchService hotelSearchService;

    public HotelSearchController(HotelSearchService hotelSearchService) {
        this.hotelSearchService = hotelSearchService;
    }

    @GetMapping
    public ResponseEntity<Page<HotelSearchResponse>> searchHotels(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        HotelSearchRequest searchRequest = HotelSearchRequest.builder()
                .query(query)
                .country(country)
                .city(city)
                .name(name)
                .minRating(minRating)
                .maxRating(maxRating)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        Page<HotelSearchResponse> results = hotelSearchService.searchHotels(searchRequest);
        return ResponseEntity.ok(results);
    }

}
