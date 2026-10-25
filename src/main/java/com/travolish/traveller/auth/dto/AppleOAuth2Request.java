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
public class AppleOAuth2Request {
    @NotBlank(message = "Authorization code required")
    private String code;

    @NotBlank(message = "ID token required")
    private String idToken;

    private String state;

    private AppleUserData user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppleUserData {
        private String name;
        private String email;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Name {
            private String firstName;
            private String lastName;
        }
    }
}
