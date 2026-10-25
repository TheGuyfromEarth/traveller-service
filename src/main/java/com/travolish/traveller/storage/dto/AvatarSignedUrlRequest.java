package com.travolish.traveller.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarSignedUrlRequest {
    private String filename;
    private String contentType;
}
