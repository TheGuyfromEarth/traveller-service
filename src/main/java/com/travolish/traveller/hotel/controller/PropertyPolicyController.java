package com.travolish.traveller.hotel.controller;

import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.dto.PropertyPolicyDTO;
import com.travolish.traveller.hotel.model.PropertyPolicy;
import com.travolish.traveller.hotel.repository.PropertyPolicyRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotels/{hotelId}/policies")
@RequiredArgsConstructor
public class PropertyPolicyController {

    private final PropertyPolicyRepository policyRepository;

    @GetMapping
    public ResponseEntity<PropertyPolicyDTO> getPolicy(@PathVariable Long hotelId) {
        return policyRepository.findByHotelId(hotelId)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<PropertyPolicyDTO> upsertPolicy(
            @PathVariable Long hotelId,
            @RequestBody PropertyPolicyDTO dto) {

        PropertyPolicy policy = policyRepository.findByHotelId(hotelId)
            .orElse(PropertyPolicy.builder().hotelId(hotelId).createdAt(OffsetDateTime.now()).build());

        policy.setCancellationPolicy(dto.getCancellationPolicy());
        policy.setRefundPolicy(dto.getRefundPolicy());
        policy.setChildPolicy(dto.getChildPolicy());
        policy.setPetPolicy(dto.getPetPolicy());
        policy.setSmokingPolicy(dto.getSmokingPolicy());
        policy.setVisitorPolicy(dto.getVisitorPolicy());
        policy.setDamagePolicy(dto.getDamagePolicy());
        policy.setQuietHours(dto.getQuietHours());
        policy.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(toDTO(policyRepository.save(policy)));
    }

    private PropertyPolicyDTO toDTO(PropertyPolicy p) {
        return PropertyPolicyDTO.builder()
            .id(p.getId()).hotelId(p.getHotelId())
            .cancellationPolicy(p.getCancellationPolicy())
            .refundPolicy(p.getRefundPolicy())
            .childPolicy(p.getChildPolicy())
            .petPolicy(p.getPetPolicy())
            .smokingPolicy(p.getSmokingPolicy())
            .visitorPolicy(p.getVisitorPolicy())
            .damagePolicy(p.getDamagePolicy())
            .quietHours(p.getQuietHours())
            .build();
    }
}
