package com.travolish.traveller.hotel.controller;

import java.util.List;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.service.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping
    public List<Hotel> list(
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Hotel.HotelStatus status) {
        if (hostId != null) {
            return hotelService.findByHostId(hostId);
        }
        if (status != null) {
            return hotelService.findByStatus(status);
        }
        return hotelService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> get(@PathVariable Long id) {
        return hotelService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Hotel create(@Validated @RequestBody Hotel hotel, Authentication authentication) {
        if (hotel.getHostId() == null && authentication != null) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            hotel.setHostId(Long.parseLong(jwt.getSubject()));
        }
        return hotelService.create(hotel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hotel> update(@PathVariable Long id, @Validated @RequestBody Hotel hotel) {
        return hotelService.update(id, hotel)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Hotel> updateStatus(
            @PathVariable Long id,
            @RequestParam Hotel.HotelStatus status,
            @RequestParam(required = false) String reason) {
        return hotelService.updateStatus(id, status, reason)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Request additional documents from the host — returns the listing to DRAFT
     * with an admin note explaining what is needed.
     */
    @PostMapping("/{id}/request-documents")
    public ResponseEntity<Hotel> requestDocuments(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String note = "Documents requested by admin" + (reason != null && !reason.isBlank() ? ": " + reason : ".");
        return hotelService.updateStatus(id, Hotel.HotelStatus.DRAFT, note)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }

}
