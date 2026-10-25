package com.travolish.traveller.listing.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.travolish.traveller.listing.model.ListingDraft.DraftStatus;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDraftDTO {
    private Long id;
    private Long hostId;
    private Integer currentStep;
    private DraftStatus status;

    // Step 1
    private String category;

    // Step 2
    private List<String> subTypes;

    // Step 3
    private String name;
    private Integer starRating;
    private Integer numBedrooms;
    private Integer numBathrooms;
    private Integer maxGuests;
    private Integer numUnits;
    private String checkInTime;
    private String checkOutTime;

    // Step 4
    private List<String> amenities;
    private List<String> targetGuests;
    private String stayType;

    private Long publishedHotelId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;
}
