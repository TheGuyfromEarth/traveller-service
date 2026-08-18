package com.travolish.traveller.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleOAuth2Request {
    
    @NotBlank(message = "ID token required")
    private String idToken;

    private String accessToken;

    private String state;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleUserData {
        private String sub;           // Google's unique user ID
        private String email;
        private String name;
        private String givenName;    // First name
        private String familyName;   // Last name
        private String picture;       // Profile picture URL
        private Boolean emailVerified;
    }
}
