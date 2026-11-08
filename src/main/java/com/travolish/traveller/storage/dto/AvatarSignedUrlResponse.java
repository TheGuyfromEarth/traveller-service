package com.travolish.traveller.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarSignedUrlResponse {
    private String signedUrl;
    private String publicUrl;
}
