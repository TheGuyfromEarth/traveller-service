package com.travolish.traveller.hotel.controller;

import java.util.List;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.service.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @RequestParam(required = false) Hotel.HotelStatus status,
            @RequestParam(required = false) String search) {
        List<Hotel> results;
        if (hostId != null) {
            results = hotelService.findByHostId(hostId);
        } else if (status != null) {
            results = hotelService.findByStatus(status);
        } else {
            // Default to LIVE-only for public listing; callers that need all statuses must pass ?status=
            results = hotelService.findByStatus(Hotel.HotelStatus.LIVE);
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            results = results.stream()
                    .filter(h -> (h.getName() != null && h.getName().toLowerCase().contains(q))
                            || (h.getCity() != null && h.getCity().toLowerCase().contains(q))
                            || (h.getCountry() != null && h.getCountry().toLowerCase().contains(q)))
                    .collect(java.util.stream.Collectors.toList());
        }
        return results;
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
    public ResponseEntity<Hotel> update(@PathVariable Long id, @Validated @RequestBody Hotel hotel,
            Authentication authentication) {
        // Hosts cannot change status via PUT — only admins may do so via PATCH /{id}/status.
        // Stripping status here means HotelServiceImpl.update() leaves the existing value intact.
        if (!isAdmin(authentication)) {
            hotel.setStatus(null);
        }
        return hotelService.update(id, hotel)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/request-documents")
    public ResponseEntity<Hotel> requestDocuments(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String note = "Documents requested by admin" + (reason != null && !reason.isBlank() ? ": " + reason : ".");
        return hotelService.updateStatus(id, Hotel.HotelStatus.DRAFT, note)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Host submits a DRAFT listing for admin review.
     * Returns 409 if the listing is not currently in DRAFT status.
     */
    @PostMapping("/{id}/submit-for-review")
    public ResponseEntity<Hotel> submitForReview(@PathVariable Long id) {
        try {
            return hotelService.submitForReview(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }

}
