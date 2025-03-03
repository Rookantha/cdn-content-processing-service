/* package com.contentnexus.processing.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class S3Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;
    private final Path tempDirectory;

    @Value("${cloud.aws.s3.raw-bucket}")
    private String rawBucketName;

    @Value("${cloud.aws.s3.processed-bucket}")
    private String processedBucketName;

    public S3Service(S3Client s3Client, Path tempDirectory) {
        this.s3Client = s3Client;
        this.tempDirectory = tempDirectory;
    }

    public void downloadRawVideo(String key, File destinationFile) throws IOException {
        // Remove the s3://bucket/ prefix if it exists
        if (key.startsWith("s3://")) {
            key = key.substring(key.indexOf('/', 5) + 1);
        }

        int maxAttempts = 3;
        int attempt = 1;
        boolean success = false;

        while (attempt <= maxAttempts && !success) {
            try {
                LOGGER.info("Attempting to download video from S3, attempt {}", attempt);

                if (destinationFile.exists()) {
                    LOGGER.info("File already exists, deleting to avoid conflicts: {}", destinationFile.getAbsolutePath());
                    if (!destinationFile.delete()) {
                        throw new IOException("Failed to delete existing file: " + destinationFile.getAbsolutePath());
                    }
                }

                // Download file from S3
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(rawBucketName)
                        .key(key)
                        .build();

                LOGGER.info("Downloading from bucket: {}, key: {}", rawBucketName, key);

                try (var s3Object = s3Client.getObject(getObjectRequest);
                     var fileOutputStream = Files.newOutputStream(destinationFile.toPath(), StandardOpenOption.CREATE_NEW)) {

                    s3Object.transferTo(fileOutputStream);
                    success = true;
                    LOGGER.info("Video downloaded successfully: {}", destinationFile.getAbsolutePath());
                }

            } catch (NoSuchKeyException e) {
                LOGGER.error("No such key exists in the bucket: {}", key, e);
                throw new IOException("No such key exists in the bucket: " + key, e);
            } catch (SdkClientException e) {
                LOGGER.error("An error occurred while downloading video from S3, attempt {}", attempt, e);
                attempt++;

                if (attempt > maxAttempts) {
                    throw new IOException("Failed to download video from S3 after " + maxAttempts + " attempts", e);
                }

                // Optionally, you can add a sleep or backoff strategy here before retrying
                try {
                    Thread.sleep(1000);  // sleep for 1 second before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry", ie);
                }
            } catch (IOException e) {
                LOGGER.error("I/O error occurred while handling the file: {}", destinationFile.getAbsolutePath(), e);
                throw e;
            }
        }
    }

    public String uploadProcessedVideo(File file, String videoId) throws IOException {
        String key = videoId + "/processed/" + System.currentTimeMillis() + "_" + file.getName();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(processedBucketName)
                            .key(key)
                            .build(),
                    RequestBody.fromFile(file)
            );
        } catch (SdkClientException e) {
            LOGGER.error("An error occurred while uploading processed video to S3: {}", e.getMessage(), e);
            throw new IOException("An error occurred while uploading processed video to S3", e);
        }
        return "s3://" + processedBucketName + "/" + key;
    }
}


 */