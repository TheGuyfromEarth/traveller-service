package com.travolish.traveller.user.controller;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import com.travolish.traveller.user.service.S3Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ImageController {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "";
        int i = original.lastIndexOf('.');
        if (i > 0) ext = original.substring(i);

        String key = String.format("users/%d/profile-%d%s", id, System.currentTimeMillis(), ext);

        s3Service.upload(key, file.getInputStream(), file.getSize(), file.getContentType());

        user.setImageKey(key);
        userRepository.save(user);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImage(@PathVariable Long id) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getImageKey() == null) {
            return ResponseEntity.notFound().build();
        }
        URL url = s3Service.getPresignedUrl(user.getImageKey(), Duration.ofHours(1));
        return ResponseEntity.ok().location(URI.create(url.toString())).build();
    }
}
