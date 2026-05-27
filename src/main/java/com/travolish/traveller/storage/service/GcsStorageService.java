package com.travolish.traveller.storage.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.travolish.traveller.storage.dto.AvatarSignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GcsStorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

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
