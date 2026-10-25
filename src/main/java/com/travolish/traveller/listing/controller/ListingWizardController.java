package com.travolish.traveller.listing.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.listing.dto.*;
import com.travolish.traveller.listing.service.ListingWizardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Listing wizard — drives hosts through a 4-step property creation flow.
 *
 * All endpoints take a hostId query param as a simple stand-in for the
 * authenticated principal until JWT auth is wired in.
 */
@RestController
@RequestMapping("/api/listing/draft")
@RequiredArgsConstructor
@Validated
public class ListingWizardController {

    private final ListingWizardService wizardService;

    /** Step 0 — start a new draft session */
    @PostMapping
    public ResponseEntity<StepResponseDTO> createDraft(@RequestParam Long hostId) {
        StepResponseDTO response = wizardService.createDraft(hostId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Step 1 — save category selection */
    @PutMapping("/{draftId}/step/1")
    public ResponseEntity<StepResponseDTO> saveStep1(
            @PathVariable Long draftId,
            @RequestParam Long hostId,
            @RequestBody @Valid Step1Request req) {
        return ResponseEntity.ok(wizardService.saveStep1(draftId, hostId, req));
    }

    /** Step 2 — save sub-type multi-select */
    @PutMapping("/{draftId}/step/2")
    public ResponseEntity<StepResponseDTO> saveStep2(
            @PathVariable Long draftId,
            @RequestParam Long hostId,
            @RequestBody @Valid Step2Request req) {
        return ResponseEntity.ok(wizardService.saveStep2(draftId, hostId, req));
    }

    /** Step 3 — save property details (name, bedrooms, guests, etc.) */
    @PutMapping("/{draftId}/step/3")
    public ResponseEntity<StepResponseDTO> saveStep3(
            @PathVariable Long draftId,
            @RequestParam Long hostId,
            @RequestBody @Valid Step3Request req) {
        return ResponseEntity.ok(wizardService.saveStep3(draftId, hostId, req));
    }

    /** Step 4 — save amenities, target guests, stay type; marks draft READY_TO_PUBLISH */
    @PutMapping("/{draftId}/step/4")
    public ResponseEntity<StepResponseDTO> saveStep4(
            @PathVariable Long draftId,
            @RequestParam Long hostId,
            @RequestBody @Valid Step4Request req) {
        return ResponseEntity.ok(wizardService.saveStep4(draftId, hostId, req));
    }

    /** Read the current state of a draft */
    @GetMapping("/{draftId}")
    public ResponseEntity<ListingDraftDTO> getDraft(
            @PathVariable Long draftId,
            @RequestParam Long hostId) {
        return ResponseEntity.ok(wizardService.getDraft(draftId, hostId));
    }

    /** Publish — converts draft into a Hotel entity (status DRAFT, awaiting review) */
    @PostMapping("/{draftId}/publish")
    public ResponseEntity<Map<String, Object>> publish(
            @PathVariable Long draftId,
            @RequestParam Long hostId) {
        Long hotelId = wizardService.publish(draftId, hostId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("hotelId", hotelId, "message",
                        "Property created in DRAFT status. Complete your listing to go live."));
    }

    /** Abandon — soft-deletes the draft */
    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> abandonDraft(
            @PathVariable Long draftId,
            @RequestParam Long hostId) {
        wizardService.abandonDraft(draftId, hostId);
        return ResponseEntity.noContent().build();
    }
}
