package com.travolish.traveller.emergency.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivateSOSRequest {
    @NotNull(message = "User ID required")
    private Long userId;
    
    @NotNull(message = "Booking ID required")
    private Long bookingId;
    
    @NotNull(message = "Hotel ID required")
    private Long hotelId;
    
    @NotNull(message = "SOS type required")
    private String sosType;
    
    private String emergencyDescription;
    
    @NotNull(message = "Latitude required")
    private Double latitude;
    
    @NotNull(message = "Longitude required")
    private Double longitude;
    
    @NotNull(message = "Phone number required")
    private String phoneNumber;
    
    private String country;
    private String city;
}
