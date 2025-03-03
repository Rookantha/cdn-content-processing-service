package com.contentnexus.processing.service.service;
import com.contentnexus.processing.service.entity.ProcessedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class VideoIngestedListener {
    private static final Logger logger = LoggerFactory.getLogger(VideoIngestedListener.class);

    private final VideoProcessingService videoProcessingService;

    public VideoIngestedListener(VideoProcessingService videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @KafkaListener(topics = "video_ingested", groupId = "content-processing-group")
    public void listen(ProcessedContent content) {
        logger.info("Received content for processing: {}", content);

        try {
            // Process the content
            videoProcessingService.processVideo(content);
        } catch (Exception e) {
            logger.error("Error processing content for video ID: {}", content.getVideoId(), e);
        }
    }


}
