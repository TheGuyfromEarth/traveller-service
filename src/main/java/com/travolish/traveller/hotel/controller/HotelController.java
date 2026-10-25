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

    /**
     * List hotels by hostId or status filter.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code ?hostId=} — returns that host's own listings (host/admin only in practice,
     *       enforced by the JWT check in the UI; no further server-side guard needed because
     *       hostId is derived from the JWT on the host dashboard).</li>
     *   <li>{@code ?status=} — returns hotels with that status; admin-only.</li>
     *   <li>No params — returns LIVE hotels only (capped at 200 rows to prevent an accidental
     *       full-table dump; use GET /api/hotels/search for paginated public search).</li>
     * </ul>
     *
     * <p>The in-memory {@code ?search=} filter is intentionally removed: push text search to
     * GET /api/hotels/search which uses a database-level Specification with an index.
     */
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
        // No filter: return LIVE hotels capped at 200.
        // Public search should use GET /api/hotels/search (paginated + cached).
        return hotelService.findByStatus(Hotel.HotelStatus.LIVE)
                .stream()
                .limit(200)
                .collect(java.util.stream.Collectors.toList());
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
