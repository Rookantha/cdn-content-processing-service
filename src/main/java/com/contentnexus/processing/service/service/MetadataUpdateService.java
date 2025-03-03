package com.contentnexus.processing.service.service;

import com.contentnexus.processing.service.entity.ProcessedContent;
import com.contentnexus.processing.service.repository.ProcessedContentRepository;
import org.springframework.stereotype.Service;

@Service
public class MetadataUpdateService {

        private final ProcessedContentRepository processedContentRepository;

        public MetadataUpdateService(ProcessedContentRepository processedContentRepository) {
            this.processedContentRepository = processedContentRepository;
        }

        public void updateMetadata(ProcessedContent content) {
            processedContentRepository.save(content);
        }
}
