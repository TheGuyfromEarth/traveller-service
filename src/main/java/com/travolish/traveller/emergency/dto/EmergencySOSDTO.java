package com.travolish.traveller.emergency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencySOSDTO {
    private Long id;
    private Long userId;
    private Long bookingId;
    private Long hotelId;
    private String status;
    private String sosType;
    private String emergencyDescription;
    private Double latitude;
    private Double longitude;
    private String userPhoneNumber;
    private String userCountry;
    private String userCity;
    private LocalDateTime activatedAt;
    private Integer emergencyContactsNotified;
    private Integer localAuthoritiesContacted;
    private Boolean liveLocationSharing;
}
