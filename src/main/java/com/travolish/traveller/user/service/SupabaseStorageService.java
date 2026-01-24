package com.travolish.traveller.user.service;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.api-key}")
    private String supabaseApiKey;

    @Value("${supabase.bucket-name:travolish-bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        log.info("Supabase Storage Service initialized");
        log.info("Supabase URL: {}", supabaseUrl);
        log.info("Bucket Name: {}", bucketName);
        log.info("API Key (first 20 chars): {}", supabaseApiKey != null && supabaseApiKey.length() > 20 
            ? supabaseApiKey.substring(0, 20) + "..." 
            : "***");
    }

    /**
     * Upload a file to Supabase Storage
     *
     * @param path        The path/key where the file will be stored (e.g., "users/123/profile.jpg")
     * @param inputStream The file content as an InputStream
     * @param contentType The MIME type of the file
     * @return The full public URL of the uploaded file
     */
    public String upload(String path, InputStream inputStream, String contentType) {
        try {
            byte[] fileContent = inputStream.readAllBytes();

            // Construct the correct Supabase Storage URL
            // supabaseUrl should be like: https://xxxxx.supabase.co
            // Storage URL should be: https://xxxxx.supabase.co/storage/v1/object/travolish-bucket/path
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, path);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseApiKey);
            headers.set("Content-Type", contentType);

            HttpEntity<byte[]> entity = new HttpEntity<>(fileContent, headers);

            log.debug("Uploading to URL: {}", uploadUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File uploaded successfully to Supabase: {}", path);
                return getPublicUrl(path);
            } else {
                log.error("Failed to upload file to Supabase: {} - Status: {}", path, response.getStatusCode());
                log.error("Response body: {}", response.getBody());
                throw new RuntimeException("Failed to upload file to Supabase");
            }
        } catch (Exception e) {
            log.error("Error uploading file to Supabase: {}", path, e);
            throw new RuntimeException("Error uploading file to Supabase", e);
        }
    }

    /**
     * Get the public URL of a file in Supabase Storage
     *
     * @param path The file path/key
     * @return The public URL
     */
    public String getPublicUrl(String path) {
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, path);
    }

    /**
     * Get a signed URL for temporary access to a file
     * Note: Supabase Storage doesn't support expiring signed URLs via REST API.
     * Use getPublicUrl() for public access or implement custom authorization logic.
     *
     * @param path   The file path/key
     * @param expiry The expiration duration (not used for Supabase)
     * @return The public URL
     */
    public URL getPresignedUrl(String path, Duration expiry) {
        try {
            // Supabase Storage requires additional setup for signed URLs (requires RLS policies)
            // For now, return the public URL if the bucket is public
            // For private access, implement custom authorization with row-level security
            String url = getPublicUrl(path);
            return new URL(url);
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", path, e);
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }

    /**
     * Delete a file from Supabase Storage
     *
     * @param path The file path/key to delete
     */
    public void delete(String path) {
        try {
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, path);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File deleted successfully from Supabase: {}", path);
            } else {
                log.warn("Failed to delete file from Supabase: {} - Status: {}", path, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error deleting file from Supabase: {}", path, e);
        }
    }
}
