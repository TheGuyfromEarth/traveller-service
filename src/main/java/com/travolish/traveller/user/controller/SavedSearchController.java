package com.travolish.traveller.user.controller;

import com.travolish.traveller.user.entity.SavedSearch;
import com.travolish.traveller.user.repository.SavedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Persists traveller saved searches server-side so they survive device changes.
 *
 *  GET    /api/users/{userId}/saved-searches
 *  POST   /api/users/{userId}/saved-searches
 *  DELETE /api/users/{userId}/saved-searches/{id}
 */
@RestController
@RequestMapping("/api/users/{userId}/saved-searches")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SavedSearchController {

    private static final int MAX_SAVED_SEARCHES = 20;

    private final SavedSearchRepository savedSearchRepository;

    /** List all saved searches for a user, newest first. */
    @GetMapping
    public ResponseEntity<List<SavedSearch>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(savedSearchRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /** Save a new search. Silently drops the oldest if limit is reached. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SavedSearch> create(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        // Enforce per-user limit
        long count = savedSearchRepository.countByUserId(userId);
        if (count >= MAX_SAVED_SEARCHES) {
            List<SavedSearch> existing = savedSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
            // Delete oldest
            SavedSearch oldest = existing.get(existing.size() - 1);
            savedSearchRepository.delete(oldest);
        }

        SavedSearch search = SavedSearch.builder()
            .userId(userId)
            .name(getString(body, "name"))
            .destination(getString(body, "destination"))
            .checkIn(getString(body, "checkIn"))
            .checkOut(getString(body, "checkOut"))
            .adults(body.containsKey("adults") ? Integer.valueOf(body.get("adults").toString()) : null)
            .children(body.containsKey("children") ? Integer.valueOf(body.get("children").toString()) : null)
            .filtersJson(body.containsKey("filtersJson") ? body.get("filtersJson").toString() : null)
            .createdAt(OffsetDateTime.now())
            .build();

        SavedSearch saved = savedSearchRepository.save(search);
        log.info("Saved search {} created for user {}", saved.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Delete a specific saved search (must belong to the user). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long id) {
        savedSearchRepository.deleteByIdAndUserId(id, userId);
        log.info("Saved search {} deleted for user {}", id, userId);
        return ResponseEntity.noContent().build();
    }

    private String getString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val != null ? val.toString() : null;
    }
}
