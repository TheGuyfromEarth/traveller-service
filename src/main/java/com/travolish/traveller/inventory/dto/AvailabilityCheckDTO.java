package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCheckDTO {

    private Long id;

    private Long roomId;

    private Long hotelId;

    private LocalDate availabilityDate;

    private Integer totalRooms;

    private Integer bookedRooms;

    private Integer availableRooms;

    private Integer blockedRooms;

    private String status; // AVAILABLE, LIMITED, FULL, BLOCKED, CLOSED

    private Double occupancyPercentage;

    private Boolean availableForBooking;

    private String blockReason;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    /**
     * Quick summary string
     */
    public String getSummary() {
        return String.format(
            "Room %d: %d/%d booked (%.1f%%), Status: %s",
            roomId, bookedRooms, totalRooms, occupancyPercentage, status
        );
    }
}
