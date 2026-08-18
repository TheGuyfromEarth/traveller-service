package com.travolish.traveller.hotel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.dto.NearbyAttractionDTO;
import com.travolish.traveller.hotel.model.NearbyAttraction;
import com.travolish.traveller.hotel.repository.NearbyAttractionRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotels/{hotelId}/nearby")
@RequiredArgsConstructor
public class NearbyAttractionController {

    private final NearbyAttractionRepository nearbyRepository;

    @GetMapping
    public ResponseEntity<List<NearbyAttractionDTO>> list(@PathVariable Long hotelId) {
        return ResponseEntity.ok(nearbyRepository.findByHotelId(hotelId).stream().map(this::toDTO).toList());
    }

    @PostMapping
    public ResponseEntity<NearbyAttractionDTO> add(
            @PathVariable Long hotelId,
            @RequestBody NearbyAttractionDTO dto) {
        NearbyAttraction entity = NearbyAttraction.builder()
            .hotelId(hotelId)
            .name(dto.getName())
            .distanceText(dto.getDistanceText())
            .attractionType(dto.getAttractionType())
            .build();
        return ResponseEntity.ok(toDTO(nearbyRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long hotelId, @PathVariable Long id) {
        nearbyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private NearbyAttractionDTO toDTO(NearbyAttraction a) {
        return NearbyAttractionDTO.builder()
            .id(a.getId()).hotelId(a.getHotelId())
            .name(a.getName()).distanceText(a.getDistanceText())
            .attractionType(a.getAttractionType())
            .build();
    }
}
