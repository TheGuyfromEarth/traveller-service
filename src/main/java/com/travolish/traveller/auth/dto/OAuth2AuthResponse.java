package com.travolish.traveller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2AuthResponse {
    private Long userId;
    private String email;
    private String name;
    private String provider;
    private String accessToken;
    private String refreshToken;
    private String profileImageUrl;
    private Boolean isNewUser;
    private Long expiresIn;
}
