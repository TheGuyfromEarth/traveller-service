package com.travolish.traveller.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientAvailabilityException extends RuntimeException {
    
    public InsufficientAvailabilityException(String message) {
        super(message);
    }

    public InsufficientAvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InsufficientAvailabilityException forDateRange(Long roomId, String startDate, String endDate) {
        return new InsufficientAvailabilityException(
            String.format("Insufficient availability for Room ID: %d from %s to %s", 
                roomId, startDate, endDate)
        );
    }

    public static InsufficientAvailabilityException allDatesFullyBooked(Long roomId) {
        return new InsufficientAvailabilityException(
            String.format("All dates fully booked for Room ID: %d", roomId)
        );
    }

    public static InsufficientAvailabilityException roomNotAvailable(Long roomId) {
        return new InsufficientAvailabilityException(
            String.format("Room ID: %d is not available for booking", roomId)
        );
    }
}
