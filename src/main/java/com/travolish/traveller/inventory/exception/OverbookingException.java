package com.travolish.traveller.inventory.exception;

public class OverbookingException extends RuntimeException {
    
    public OverbookingException(String message) {
        super(message);
    }

    public OverbookingException(String message, Throwable cause) {
        super(message, cause);
    }

    public static OverbookingException noRoomAvailable(Long roomId, Long hotelId) {
        return new OverbookingException(
            String.format("No available rooms. Room ID: %d, Hotel ID: %d", roomId, hotelId)
        );
    }

    public static OverbookingException insufficientInventory(Long roomId, int required, int available) {
        return new OverbookingException(
            String.format("Insufficient inventory. Room ID: %d, Required: %d, Available: %d", 
                roomId, required, available)
        );
    }

    public static OverbookingException bookingConflict(Long roomId, String checkIn, String checkOut) {
        return new OverbookingException(
            String.format("Booking conflict for Room ID: %d between %s and %s", 
                roomId, checkIn, checkOut)
        );
    }
}
