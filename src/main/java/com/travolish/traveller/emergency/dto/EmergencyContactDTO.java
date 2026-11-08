package com.travolish.traveller.emergency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactDTO {
    private Long id;
    private Long hotelId;
    private String label;
    private String country;
    private String city;
    private String contactType;
    private String contactNumber;
    private String contactName;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String email;
    private String operatingHours;
    private Integer responseTimeMinutes;
    private Boolean isActive;
}
