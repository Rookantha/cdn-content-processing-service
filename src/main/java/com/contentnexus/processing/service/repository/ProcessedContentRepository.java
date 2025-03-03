package com.contentnexus.processing.service.repository;

import com.contentnexus.processing.service.entity.ProcessedContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedContentRepository extends JpaRepository<ProcessedContent, Long> {
}
