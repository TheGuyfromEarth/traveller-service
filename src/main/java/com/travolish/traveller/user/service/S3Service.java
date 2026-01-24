package com.travolish.traveller.user.service;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private final String bucket;
    private final Region region;

    public S3Service(@Value("${spring.s3.bucket:travolish-bucket}") String bucket,
                     @Value("${spring.s3.region:us-east-1}") String region) {
        this.bucket = bucket;
        this.region = Region.of(region);
    }

    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        S3Client s3 = S3Client.builder().region(region).build();

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(por, RequestBody.fromInputStream(inputStream, contentLength));
        s3.close();

        return key;
    }

    public URL getPresignedUrl(String key, Duration expiry) {
        // Fallback: return S3 utilities URL (not presigned). If your bucket is private
        // consider streaming the object through the app or adding a presigner dependency.
        S3Client s3 = S3Client.builder().region(region).build();
        S3Utilities utilities = s3.utilities();
        GetUrlRequest getUrlRequest = GetUrlRequest.builder().bucket(bucket).key(key).build();
        URL url = utilities.getUrl(getUrlRequest);
        s3.close();
        return url;
    }
}
