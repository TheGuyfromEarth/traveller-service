package com.travolish.traveller.storage.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Cors;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.travolish.traveller.storage.dto.AvatarSignedUrlResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsStorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsRaw;

    @PostConstruct
    void configureBucketCors() {
        try {
            List<Cors.Origin> origins = Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .map(Cors.Origin::of)
                    .collect(Collectors.toList());

            List<Cors> corsConfig = List.of(
                    Cors.newBuilder()
                            .setOrigins(origins)
                            .setMethods(List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.HEAD))
                            .setResponseHeaders(List.of("Content-Type"))
                            .setMaxAgeSeconds(3600)
                            .build()
            );

            Bucket bucket = storage.get(bucketName);
            if (bucket != null) {
                bucket.toBuilder().setCors(corsConfig).build().update();
                log.info("GCS bucket CORS configured for {} origins", origins.size());
            }
        } catch (Exception e) {
            log.warn("Could not configure GCS bucket CORS (may lack storage.buckets.update): {}", e.getMessage());
        }
    }

    public AvatarSignedUrlResponse generateAvatarUploadUrl(String filename, String contentType) {
        String ext = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.'))
                : "";
        String objectName = "avatars/" + UUID.randomUUID() + ext;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(contentType)
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );

        String publicUrl = String.format(
                "https://storage.googleapis.com/%s/%s", bucketName, objectName);

        return new AvatarSignedUrlResponse(signedUrl.toString(), publicUrl);
    }
}
