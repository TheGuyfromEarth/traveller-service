package com.travolish.traveller.storage.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Cloudflare R2 storage service (S3-compatible API).
 * Replaces SupabaseStorageService — same interface, zero egress fees.
 *
 * Required env vars:
 *   R2_ACCOUNT_ID        — found in Cloudflare dashboard → R2 overview
 *   R2_ACCESS_KEY_ID     — R2 API token Access Key ID
 *   R2_SECRET_ACCESS_KEY — R2 API token Secret Access Key
 *   R2_BUCKET_NAME       — the R2 bucket name (e.g. travolish-media)
 *   R2_PUBLIC_URL        — public bucket URL (e.g. https://pub-xxx.r2.dev or custom domain)
 */
@Slf4j
@Service
public class R2StorageService {

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.access-key-id}")
    private String accessKeyId;

    @Value("${r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    private S3Client s3;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        URI endpoint = URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
        var credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );

        s3 = S3Client.builder()
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .region(Region.of("auto"))   // R2 uses "auto" as region
            .build();

        presigner = S3Presigner.builder()
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .region(Region.of("auto"))
            .build();

        log.info("R2StorageService initialised — bucket: {}", bucketName);
    }

    /**
     * Upload a file to R2 and return its public URL.
     *
     * @param path        Object key, e.g. "hotels/61/main-image-123.jpg"
     * @param inputStream File bytes
     * @param contentType MIME type, e.g. "image/jpeg"
     * @return Public URL served via Cloudflare CDN (zero egress cost)
     */
    public String upload(String path, InputStream inputStream, String contentType) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .contentType(contentType)
                .build();
            s3.putObject(req, RequestBody.fromBytes(bytes));
            log.info("R2 upload OK: {}", path);
            return getPublicUrl(path);
        } catch (Exception e) {
            log.error("R2 upload failed for key {}: {}", path, e.getMessage());
            throw new RuntimeException("R2 upload failed", e);
        }
    }

    /**
     * Returns the public CDN URL for a stored object.
     * Uses the R2 public bucket URL (r2.dev domain or custom domain).
     */
    public String getPublicUrl(String path) {
        return publicUrl.endsWith("/")
            ? publicUrl + path
            : publicUrl + "/" + path;
    }

    /**
     * Returns a time-limited pre-signed GET URL (for private buckets).
     */
    public URL getPresignedUrl(String path, Duration ttl) {
        var presignReq = GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(r -> r.bucket(bucketName).key(path))
            .build();
        return presigner.presignGetObject(presignReq).url();
    }

    /**
     * Delete an object from R2.
     */
    public void delete(String path) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(path).build());
            log.info("R2 delete OK: {}", path);
        } catch (Exception e) {
            log.warn("R2 delete failed for key {}: {}", path, e.getMessage());
        }
    }
}
