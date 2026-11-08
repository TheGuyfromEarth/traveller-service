package com.travolish.traveller.listing.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.travolish.traveller.hotel.model.PropertyCategory;
import com.travolish.traveller.hotel.model.PropertySubType;
import com.travolish.traveller.hotel.model.StayType;
import com.travolish.traveller.hotel.model.TargetGuest;

import jakarta.persistence.*;
import lombok.*;

/**
 * Wizard session state for the multi-step listing creation flow.
 * Accumulates data across steps 1–4 before being published as a Hotel entity.
 */
@Entity
@Table(name = "listing_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostId;

    // ── Step 1 — Category ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private PropertyCategory category;

    // ── Step 2 — Sub-types (multi-select tags) ────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_draft_sub_types", joinColumns = @JoinColumn(name = "draft_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_type")
    @Builder.Default
    private List<PropertySubType> subTypes = new ArrayList<>();

    // ── Step 3 — Property details ─────────────────────────────────────────
    private String name;

    private Integer starRating;

    private Integer numBedrooms;

    private Integer numBathrooms;

    private Integer maxGuests;

    private Integer numUnits;

    private String checkInTime;

    private String checkOutTime;

    // ── Step 4 — Features & amenities ────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_draft_amenities", joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "amenity")
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_draft_target_guests", joinColumns = @JoinColumn(name = "draft_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "target_guest")
    @Builder.Default
    private List<TargetGuest> targetGuests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private StayType stayType;

    // ── Wizard lifecycle ──────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DraftStatus status = DraftStatus.IN_PROGRESS;

    @Builder.Default
    private Integer currentStep = 1;

    /** Set when draft is successfully published — points to the created Hotel. */
    private Long publishedHotelId;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /** Drafts auto-expire after 30 days of inactivity. */
    @Builder.Default
    private OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(30);

    public enum DraftStatus {
        IN_PROGRESS,
        READY_TO_PUBLISH,
        PUBLISHED,
        ABANDONED,
        EXPIRED
    }
}
