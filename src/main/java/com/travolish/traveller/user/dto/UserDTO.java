package com.travolish.traveller.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String preferredName;
    private String email;
    private String password;
    private String phone;
    private String city;
    private String timeZone;
    private String travelStyle;
    private String bio;
    private String provider;
    private String providerId;
    private String imageKey;
}
