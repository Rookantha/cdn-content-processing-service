package com.contentnexus.processing.service.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GcsService {
    private static final Logger LOGGER = Logger.getLogger(GcsService.class.getName());

    private Storage storage;

    @Value("${google.cloud.credentials.file}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            throw new IllegalArgumentException("Credentials path is not configured properly.");
        }

        File credentialsFile = new File(credentialsPath.replace("\\", "/"));
        if (!credentialsFile.exists()) {
            throw new IllegalArgumentException("Credentials file not found at: " + credentialsPath);
        }

        try (FileInputStream credentialsStream = new FileInputStream(credentialsFile)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            this.storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            LOGGER.info("GCS Service initialized successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize GCS Service.", e);
            throw new RuntimeException("Failed to initialize GCS Service", e);
        }
    }

    public void uploadToGcs(File file, String gcsUri) {
        String[] uriParts = gcsUri.replace("gs://", "").split("/", 2);
        if (uriParts.length != 2) {
            throw new IllegalArgumentException("Invalid GCS URI format: " + gcsUri);
        }
        String bucketName = uriParts[0];
        String objectName = uriParts[1];

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            storage.create(blobInfo, fileBytes);
            LOGGER.info("Uploaded file to GCS: " + gcsUri);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read file: " + file.getPath(), e);
            throw new RuntimeException("Failed to read file: " + file.getPath(), e);
        } catch (StorageException e) {
            LOGGER.log(Level.SEVERE, "Failed to upload file to GCS: " + gcsUri, e);
            throw new RuntimeException("Failed to upload file to GCS: " + gcsUri, e);
        }
    }

    public String generateGcsUri(String bucketName, String fileName) {
        return "gs://" + bucketName + "/" + fileName;
    }

    public void downloadFromGcs(String gcsUri, File destination) {
        String[] uriParts = gcsUri.replace("gs://", "").split("/", 2);
        if (uriParts.length != 2) {
            throw new IllegalArgumentException("Invalid GCS URI format: " + gcsUri);
        }
        String bucketName = uriParts[0];
        String objectName = uriParts[1];

        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);

        if (blob != null) {
            try {
                blob.downloadTo(destination.toPath());
                LOGGER.info("Downloaded file from GCS: " + gcsUri);

            } catch (StorageException e) {
                LOGGER.log(Level.SEVERE, "Failed to download file from GCS: " + gcsUri, e);
                throw new RuntimeException("Failed to download file from GCS: " + gcsUri, e);
            }
        } else {
            throw new RuntimeException("File not found in GCS: " + gcsUri);
        }
    }
}
