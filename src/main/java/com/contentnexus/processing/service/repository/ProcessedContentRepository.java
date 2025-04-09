package com.contentnexus.processing.service.repository;

import com.contentnexus.processing.service.entity.ProcessedContent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedContentRepository extends MongoRepository<ProcessedContent, Long> {
}
