package com.travolish.traveller.hotel.controller;

import com.travolish.traveller.hotel.service.HotelService;
import com.travolish.traveller.user.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelMediaController {

    private final HotelService hotelService;
    private final SupabaseStorageService supabaseStorageService;

    @PostMapping("/{hotelId}/images")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @PathVariable Long hotelId,
            @RequestPart("file") MultipartFile file) {
        if (hotelService.findById(hotelId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String ext = resolveExtension(file.getOriginalFilename());
            String key = String.format("hotels/%d/main-image-%d%s", hotelId, System.currentTimeMillis(), ext);
            String url = supabaseStorageService.upload(key, file.getInputStream(), file.getContentType());
            hotelService.updateImageUrl(hotelId, url);
            log.info("Uploaded image for hotel {}: {}", hotelId, url);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url, "hotelId", hotelId));
        } catch (Exception e) {
            log.error("Image upload failed for hotel {}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{hotelId}/videos")
    public ResponseEntity<Map<String, Object>> uploadVideo(
            @PathVariable Long hotelId,
            @RequestPart("file") MultipartFile file) {
        if (hotelService.findById(hotelId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String ext = resolveExtension(file.getOriginalFilename());
            String key = String.format("hotels/%d/main-video-%d%s", hotelId, System.currentTimeMillis(), ext);
            String url = supabaseStorageService.upload(key, file.getInputStream(), file.getContentType());
            hotelService.updateVideoUrl(hotelId, url);
            log.info("Uploaded video for hotel {}: {}", hotelId, url);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url, "hotelId", hotelId));
        } catch (Exception e) {
            log.error("Video upload failed for hotel {}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String resolveExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }
}
