package com.travolish.traveller.hotel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.dto.PropertySeoMetaDTO;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.PropertySeoMeta;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.service.PropertySeoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotels/{hotelId}/seo")
@RequiredArgsConstructor
public class PropertySeoController {

    private final PropertySeoService seoService;
    private final HotelRepository hotelRepository;

    @GetMapping
    public ResponseEntity<PropertySeoMetaDTO> getSeoMeta(@PathVariable Long hotelId) {
        PropertySeoMeta meta = seoService.getOrGenerateSeoMeta(hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
        return ResponseEntity.ok(toDTO(meta, hotel));
    }

    @PostMapping("/regenerate")
    public ResponseEntity<PropertySeoMetaDTO> regenerate(@PathVariable Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
        PropertySeoMeta meta = seoService.generateAndSaveSeoMeta(hotel);
        return ResponseEntity.ok(toDTO(meta, hotel));
    }

    private PropertySeoMetaDTO toDTO(PropertySeoMeta meta, Hotel hotel) {
        List<String> guestTags = seoService.generateGuestMatchTags(hotel);
        Double valueScore = seoService.computeValueScore(hotel);
        Double locationScore = seoService.computeLocationScore(hotel);
        return PropertySeoMetaDTO.builder()
            .hotelId(meta.getHotelId())
            .pageTitle(meta.getPageTitle())
            .metaDescription(meta.getMetaDescription())
            .urlSlug(meta.getUrlSlug())
            .schemaJson(meta.getSchemaJson())
            .coverImageTitle(meta.getCoverImageTitle())
            .guestMatchTags(guestTags)
            .valueScore(valueScore)
            .locationScore(locationScore)
            .lastGeneratedAt(meta.getLastGeneratedAt())
            .build();
    }
}
