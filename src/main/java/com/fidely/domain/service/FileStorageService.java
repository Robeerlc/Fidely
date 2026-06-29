package com.fidely.domain.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Storage storage;
    private final String bucketName;

    public FileStorageService(
            @Value("${GOOGLE_CREDENTIALS_JSON}") String credentialsJson,
            @Value("${fidely.storage.gcs-bucket}") String bucketName) throws IOException {

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));

        this.storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();

        this.bucketName = bucketName;
    }

    public String storeFile(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String blobName = "uploads/" + UUID.randomUUID() + "_" + originalName;

            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());

            return "https://storage.googleapis.com/" + bucketName + "/" + blobName;
        } catch (IOException e) {
            log.error("Error subiendo archivo a GCS: {}", e.getMessage());
            throw new RuntimeException("No se pudo subir el archivo.", e);
        }
    }
}
