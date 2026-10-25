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
public class FacebookOAuth2Request {
    @NotBlank(message = "Access token required")
    private String accessToken;

    @NotBlank(message = "User ID required")
    private String userId;

    private String state;
}
