package com.travolish.traveller.storage.controller;

import com.travolish.traveller.storage.dto.AvatarSignedUrlRequest;
import com.travolish.traveller.storage.dto.AvatarSignedUrlResponse;
import com.travolish.traveller.storage.service.GcsStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final GcsStorageService gcsStorageService;

    @PostMapping("/avatar/signed-url")
    public ResponseEntity<AvatarSignedUrlResponse> getAvatarSignedUrl(
            @RequestBody AvatarSignedUrlRequest request) {
        return ResponseEntity.ok(
                gcsStorageService.generateAvatarUploadUrl(request.getFilename(), request.getContentType()));
    }
}
