package com.contentnexus.processing.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "processedContents")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String videoId;

    @Column(nullable = false)
    private String rawVideoPath;

    @Column(nullable = true)
    private String templateId;

    @Column(nullable = true)
    private String processedVideoPath;

    @Column(nullable = true)
    private String status;

    @Column(nullable = true)
    private String encodingDetails;

    @Column(nullable = true)
    private String errorDetails;
}
