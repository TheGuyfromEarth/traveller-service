package com.travolish.traveller.user.controller;

import com.travolish.traveller.user.entity.EmergencyContact;
import com.travolish.traveller.user.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD endpoints for a traveller's emergency contacts.
 *
 *  GET    /api/users/{userId}/emergency-contacts
 *  POST   /api/users/{userId}/emergency-contacts
 *  PUT    /api/users/{userId}/emergency-contacts/{contactId}
 *  DELETE /api/users/{userId}/emergency-contacts/{contactId}
 *  PATCH  /api/users/{userId}/emergency-contacts/{contactId}/primary
 *
 * Security
 * --------
 * No @CrossOrigin here — the global CorsConfigurationSource in UserSecurityConfig
 * already applies the configured allowed-origins to every path.
 *
 * Every endpoint verifies that the JWT subject (= backend user ID) matches the
 * {userId} path variable so user A cannot read or mutate user B's contacts (IDOR fix).
 *
 * All write methods are @Transactional so that multi-step operations (clearPrimary +
 * save, delete + promoteFallbackPrimary) are atomic and concurrent requests cannot
 * leave the DB with zero or multiple primary contacts.
 */
@RestController
@RequestMapping("/api/users/{userId}/emergency-contacts")
@RequiredArgsConstructor
@Slf4j
public class EmergencyContactController {

    private static final int MAX_CONTACTS = 10;

    private final EmergencyContactRepository repo;

    // ── List ────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<EmergencyContact>> list(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {

        if (!isOwner(jwt, userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.ok(
                repo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId));
    }

    // ── Create ──────────────────────────────────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<EmergencyContact> create(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {

        if (!isOwner(jwt, userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        // Validate required fields before any DB work — avoids a 500 on constraint violation.
        String name  = getString(body, "name");
        String phone = getString(body, "phone");
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            log.warn("Create emergency contact rejected — name or phone missing for user {}", userId);
            return ResponseEntity.badRequest().build();
        }

        // Single count query — used for both the limit guard and isFirst detection.
        long count = repo.countByUserId(userId);
        if (count >= MAX_CONTACTS) {
            log.warn("User {} hit the emergency-contact limit ({})", userId, MAX_CONTACTS);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        boolean isPrimary = parseBool(body, "isPrimary");
        // Auto-promote the very first contact even if the client didn't set the flag.
        boolean isFirst   = count == 0;

        // Exactly one primary at a time — clear any existing primary first.
        if (isPrimary || isFirst) clearPrimary(userId);

        EmergencyContact contact = EmergencyContact.builder()
                .userId(userId)
                .name(name.trim())
                .relationship(getString(body, "relationship"))
                .phone(phone.trim())
                .isPrimary(isPrimary || isFirst)
                .build();

        EmergencyContact saved = repo.save(contact);
        log.info("Emergency contact {} created for user {}", saved.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @PutMapping("/{contactId}")
    @Transactional
    public ResponseEntity<EmergencyContact> update(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {

        if (!isOwner(jwt, userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        EmergencyContact contact = repo.findById(contactId)
                .filter(c -> c.getUserId().equals(userId))
                .orElse(null);
        if (contact == null) return ResponseEntity.notFound().build();

        // Validate any supplied required fields before mutating.
        if (body.containsKey("name")) {
            String name = getString(body, "name");
            if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
            contact.setName(name.trim());
        }
        if (body.containsKey("phone")) {
            String phone = getString(body, "phone");
            if (phone == null || phone.isBlank()) return ResponseEntity.badRequest().build();
            contact.setPhone(phone.trim());
        }
        if (body.containsKey("relationship")) {
            contact.setRelationship(getString(body, "relationship"));
        }

        // isPrimary is only updated when the key is explicitly present in the body.
        // Without this guard a partial update such as PUT { "name": "…" } would
        // silently demote a primary contact because parseBool defaults to false.
        if (body.containsKey("isPrimary")) {
            boolean isPrimary = parseBool(body, "isPrimary");
            if (isPrimary && !Boolean.TRUE.equals(contact.getIsPrimary())) {
                clearPrimary(userId);
            }
            contact.setIsPrimary(isPrimary);
        }

        EmergencyContact saved = repo.save(contact);
        log.info("Emergency contact {} updated for user {}", contactId, userId);
        return ResponseEntity.ok(saved);
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{contactId}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal Jwt jwt) {

        if (!isOwner(jwt, userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (!repo.existsByIdAndUserId(contactId, userId)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteByIdAndUserId(contactId, userId);
        log.info("Emergency contact {} deleted for user {}", contactId, userId);

        // If the deleted contact was the primary, auto-promote the next oldest.
        promoteFallbackPrimary(userId);

        return ResponseEntity.noContent().build();
    }

    // ── Set primary ─────────────────────────────────────────────────────────

    @PatchMapping("/{contactId}/primary")
    @Transactional
    public ResponseEntity<EmergencyContact> setPrimary(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal Jwt jwt) {

        if (!isOwner(jwt, userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        EmergencyContact contact = repo.findById(contactId)
                .filter(c -> c.getUserId().equals(userId))
                .orElse(null);
        if (contact == null) return ResponseEntity.notFound().build();

        clearPrimary(userId);
        contact.setIsPrimary(true);
        return ResponseEntity.ok(repo.save(contact));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns true only when the authenticated user's backend ID (JWT subject)
     * matches the {userId} path variable.  Prevents IDOR: user A cannot read
     * or modify user B's emergency contacts.
     */
    private boolean isOwner(Jwt jwt, Long userId) {
        try {
            return Long.valueOf(jwt.getSubject()).equals(userId);
        } catch (NumberFormatException e) {
            log.warn("JWT subject '{}' is not a valid user ID", jwt.getSubject());
            return false;
        }
    }

    /** Demotes every contact for the user — used before promoting a new primary. */
    private void clearPrimary(Long userId) {
        repo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId)
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPrimary()))
                .forEach(c -> {
                    c.setIsPrimary(false);
                    repo.save(c);
                });
    }

    /**
     * After a delete, if no primary remains, auto-promotes the oldest surviving
     * contact so the user always has an identifiable primary when possible.
     */
    private void promoteFallbackPrimary(Long userId) {
        boolean hasPrimary = repo.findFirstByUserIdAndIsPrimaryTrue(userId).isPresent();
        if (!hasPrimary) {
            List<EmergencyContact> remaining =
                    repo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId);
            if (!remaining.isEmpty()) {
                EmergencyContact next = remaining.get(0);
                next.setIsPrimary(true);
                repo.save(next);
                log.info("Auto-promoted emergency contact {} to primary for user {}",
                        next.getId(), userId);
            }
        }
    }

    private String getString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val != null ? val.toString() : null;
    }

    private boolean parseBool(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val != null && Boolean.parseBoolean(val.toString());
    }
}
