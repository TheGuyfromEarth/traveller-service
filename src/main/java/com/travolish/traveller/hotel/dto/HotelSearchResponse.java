package com.travolish.traveller.hotel.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResponse {

    // ── Identity & location ───────────────────────────────────────────────────
    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    // ── Ratings & reviews ─────────────────────────────────────────────────────
    private Double rating;
    private Integer reviewCount;

    // ── Contact (host-tools use these) ────────────────────────────────────────
    private String phone;
    private String email;

    // ── Description ───────────────────────────────────────────────────────────
    private String description;

    // ── Media — cover photo + optional video (no full gallery on list view) ───
    private String imageUrl;
    private String videoUrl;

    // ── Property attributes needed by the search card & filters ──────────────
    private List<String> amenities;   // used for client-side amenity filter
    private Integer maxGuests;        // used for guest-count filter
    private Boolean instantBooking;   // used for instant-book filter
    private Integer minimumStay;      // informational
    private String checkInTime;
    private String checkOutTime;

    // ── Pricing — cheapest available room per night ───────────────────────────
    // Populated from a single GROUP BY query on the rooms table so the frontend
    // does NOT need to call GET /api/rooms to get a price for the search card.
    private Double cheapestRoomPrice;

    // ── Metadata ──────────────────────────────────────────────────────────────
    private OffsetDateTime createdAt;

}
