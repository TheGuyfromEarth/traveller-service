package com.travolish.traveller.inventory.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "room_availability", indexes = {
    @Index(name = "idx_room_availability_room_date", columnList = "room_id, availability_date"),
    @Index(name = "idx_room_availability_hotel_date", columnList = "hotel_id, availability_date"),
    @Index(name = "idx_room_availability_date_range", columnList = "availability_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private LocalDate availabilityDate;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer totalRooms = 1;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer bookedRooms = 0;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer availableRooms = 1;

    @Min(0)
    @Builder.Default
    private Integer blockedRooms = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    private String blockReason; // Reason for blocking (maintenance, event, etc.)

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum AvailabilityStatus {
        AVAILABLE,      // Rooms are available for booking
        LIMITED,        // Few rooms remaining
        FULL,           // All rooms booked
        BLOCKED,        // Room blocked for maintenance/other reasons
        CLOSED          // Room closed for the date
    }

    /**
     * Calculate occupancy percentage for this date
     */
    public Double getOccupancyPercentage() {
        if (totalRooms == 0) return 0.0;
        return ((double) bookedRooms / totalRooms) * 100;
    }

    /**
     * Check if room is available for booking
     */
    public Boolean isAvailableForBooking() {
        return availableRooms > 0 && status == AvailabilityStatus.AVAILABLE;
    }

    /**
     * Update availability based on new booking
     */
    public void addBooking() {
        if (bookedRooms < totalRooms) {
            bookedRooms++;
            availableRooms = totalRooms - bookedRooms - (blockedRooms != null ? blockedRooms : 0);
            updateStatus();
            updatedAt = OffsetDateTime.now();
        }
    }

    /**
     * Remove booking (cancellation)
     */
    public void removeBooking() {
        if (bookedRooms > 0) {
            bookedRooms--;
            availableRooms = totalRooms - bookedRooms - (blockedRooms != null ? blockedRooms : 0);
            updateStatus();
            updatedAt = OffsetDateTime.now();
        }
    }

    /**
     * Block rooms for maintenance
     */
    public void blockRooms(Integer count, String reason) {
        if (count > 0 && blockedRooms + count <= totalRooms) {
            blockedRooms = (blockedRooms != null ? blockedRooms : 0) + count;
            availableRooms = totalRooms - bookedRooms - blockedRooms;
            blockReason = reason;
            updateStatus();
            updatedAt = OffsetDateTime.now();
        }
    }

    /**
     * Unblock rooms
     */
    public void unblockRooms(Integer count) {
        if (count > 0 && blockedRooms >= count) {
            blockedRooms -= count;
            availableRooms = totalRooms - bookedRooms - blockedRooms;
            blockReason = null;
            updateStatus();
            updatedAt = OffsetDateTime.now();
        }
    }

    /**
     * Update status based on availability
     */
    public void updateStatus() {
        if (status == AvailabilityStatus.BLOCKED || status == AvailabilityStatus.CLOSED) {
            return; // Don't auto-change if explicitly blocked/closed
        }
        
        if (availableRooms == 0) {
            status = AvailabilityStatus.FULL;
        } else if (availableRooms <= (totalRooms * 0.2)) { // Less than 20% available
            status = AvailabilityStatus.LIMITED;
        } else {
            status = AvailabilityStatus.AVAILABLE;
        }
    }
}
