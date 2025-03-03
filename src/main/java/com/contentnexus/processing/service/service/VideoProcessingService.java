package com.contentnexus.processing.service.service;

import com.contentnexus.processing.service.entity.ProcessedContent;
import com.contentnexus.processing.service.repository.ProcessedContentRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.video.transcoder.v1.Job;
import com.google.cloud.video.transcoder.v1.TranscoderServiceClient;
import com.google.cloud.video.transcoder.v1.TranscoderServiceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class VideoProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);
    private static final String tempDir = System.getProperty("java.io.tmpdir");

    private final GcsService gcsService;
    private final ProcessedContentRepository processedContentRepository;
    private Storage storage;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.transcoder.location}")
    private String location;

    @Value("${google.cloud.credentials.file}")
    private String credentialsPath;

    @Value("${google.gcs.input-bucket}")
    private String inputBucket;

    @Value("${google.gcs.output-bucket}")
    private String outputBucket;

    @Value("${google.gcs.archive-bucket}")
    private String archiveBucket;

    public VideoProcessingService(GcsService gcsService, ProcessedContentRepository processedContentRepository) {
        this.gcsService = gcsService;
        this.processedContentRepository = processedContentRepository;
    }

    @PostConstruct
    public void init() throws IOException {
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
        }
    }

    public void processVideo(ProcessedContent content) {
        try {
            logger.info("Starting video processing for video ID: {}", content.getVideoId());

            // Download the raw video file from GCS
            String gcsInputUri = content.getRawVideoPath();
            File rawFile = new File(tempDir, new File(gcsInputUri).getName());
            logger.info("Downloading raw video from GCS: {}", gcsInputUri);
            gcsService.downloadFromGcs(gcsInputUri, rawFile);


            // Archive the raw video to the archive bucket
            String gcsArchiveUri = gcsService.generateGcsUri(archiveBucket, rawFile.getName());
            logger.info("Archiving raw video to GCS: {}", gcsArchiveUri);
            gcsService.uploadToGcs(rawFile, gcsArchiveUri);
            logger.info("Raw video archived successfully.");

            // Ensure the output URI ends with a slash to indicate a directory
            String gcsOutputUri = gcsService.generateGcsUri(outputBucket, "processed_videos/");
            if (!gcsOutputUri.endsWith("/")) {
                gcsOutputUri += "/";
            }

            // Transcoder API will place the output in the specified directory
            logger.info("Transcoding output will be placed in: {}", gcsOutputUri);

            // Process video using Transcoder
            processVideoWithTranscoder(content, gcsInputUri, gcsOutputUri);

            // Assuming the output file will be named based on the original input
            String finalGcsOutputUri = gcsOutputUri + "hd.mp4";
            logger.info("Final GCS URI for the processed video: {}", finalGcsOutputUri);

            // Download the processed video back from GCS to local temp directory
            File outputFile = new File(tempDir, "processed_" + rawFile.getName());
            logger.info("Downloading processed video from GCS: {}", finalGcsOutputUri);
            gcsService.downloadFromGcs(finalGcsOutputUri, outputFile);

            // Upload the processed video to the final GCS bucket
            //String reuploadedGcsUri = gcsService.generateGcsUri(outputBucket, "processed_" + outputFile.getName());
            //logger.info("Re-uploading processed video to GCS: {}", reuploadedGcsUri);
            //gcsService.uploadToGcs(outputFile, reuploadedGcsUri);

            // Archive the processed video to the archive bucket
            String processedArchiveUri = gcsService.generateGcsUri(archiveBucket, "Archive_" + outputFile.getName());
            logger.info("Archiving processed video to GCS: {}", processedArchiveUri);
            gcsService.uploadToGcs(outputFile, processedArchiveUri);
            logger.info("Processed video archived successfully.");

            // Update and save processed content
            content.setProcessedVideoPath(processedArchiveUri);
            content.setStatus("Processed");
            content.setEncodingDetails("720p, H.264");
            processedContentRepository.save(content);
            logger.info("Processed content saved to database for video ID: {}", content.getVideoId());

        } catch (IOException e) {
            logger.error("I/O Error processing video ID: {}", content.getVideoId(), e);
            handleProcessingFailure(content, "I/O Error", e);
        } catch (Exception e) {
            logger.error("Unexpected error processing video ID: {}", content.getVideoId(), e);
            handleProcessingFailure(content, "Unexpected Error", e);
        }
    }

    private void processVideoWithTranscoder(ProcessedContent content, String gcsInputUri, String gcsOutputUri) throws IOException {
        try (TranscoderServiceClient transcoderClient = createTranscoderClient()) {
            String parent = String.format("projects/%s/locations/%s", projectId, location);

            Job job = Job.newBuilder()
                    .setInputUri(gcsInputUri)
                    .setOutputUri(gcsOutputUri)
                    .build();

            Job createdJob = transcoderClient.createJob(parent, job);
            logger.info("Video transcoding started for video ID: {}", content.getVideoId());

            boolean jobCompleted = false;

            while (!jobCompleted) {
                try {
                    Thread.sleep(10000); // Adjust sleep time as needed

                    Job jobStatus = transcoderClient.getJob(createdJob.getName());
                    String jobState = jobStatus.getState().name(); // Use string representation of state

                    logger.info("Current job state for video ID {}: {}", content.getVideoId(), jobState);

                    if (jobState.equals("FAILED")) {
                        String reason = "Transcoding failed. Job state: " + jobState;
                        logger.error("Transcoding failed for video ID {}. Error details: {}", content.getVideoId(), jobStatus.getError().getMessage());
                        handleProcessingFailure(content, reason, new Exception(reason));
                        break;
                    }

                    switch (jobState) {
                        case "SUCCEEDED":
                            jobCompleted = true;
                            logger.info("Video transcoding completed successfully for video ID: {}", content.getVideoId());
                            break;
                        case "RUNNING":
                            // Optionally log running state or handle differently
                            break;
                        case "CANCELLED":
                            jobCompleted = true;
                            String cancelledReason = "Transcoding job was cancelled. Job state: " + jobState;
                            handleProcessingFailure(content, cancelledReason, new Exception(cancelledReason));
                            break;
                        default:
                            logger.warn("Unexpected job state for video ID {}: {}", content.getVideoId(), jobState);
                            break;
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for job to complete for video ID: {}", content.getVideoId(), e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                } catch (Exception e) {
                    logger.error("Error while checking job status for video ID: {}", content.getVideoId(), e);
                    handleProcessingFailure(content, "Error checking job status", e);
                    break;
                }
            }
        }
    }

    private TranscoderServiceClient createTranscoderClient() throws IOException {
        // Load credentials from the specified file
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

        TranscoderServiceSettings settings = TranscoderServiceSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        return TranscoderServiceClient.create(settings);
    }

    private void handleProcessingFailure(ProcessedContent content, String errorMessage, Exception e) {
        content.setStatus("Failed");
        content.setErrorDetails(errorMessage);
        processedContentRepository.save(content);
        logger.error("Updated content status to 'Failed' for video ID: {}. Error details saved.", content.getVideoId());
    }

}
