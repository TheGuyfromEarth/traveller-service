package com.travolish.traveller.storage.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class GcsConfig {

    @Bean
    public Storage googleCloudStorage() throws IOException {
        // On Cloud Run the SA key is injected as GCP_SA_KEY_B64 (base64-encoded JSON).
        // Locally, fall back to Application Default Credentials (gcloud auth application-default login).
        String b64Key = System.getenv("GCP_SA_KEY_B64");
        if (b64Key != null && !b64Key.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(b64Key);
            ServiceAccountCredentials credentials =
                    ServiceAccountCredentials.fromStream(new ByteArrayInputStream(keyBytes));
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }
        return StorageOptions.getDefaultInstance().getService();
    }
}
