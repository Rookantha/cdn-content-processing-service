package com.contentnexus.processing.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "processedContents")
public class ProcessedContent {

    @Id
    private String id;  // MongoDB uses String IDs by default

    private String videoId;
    private String rawVideoPath;
    private String templateId;
    private String processedVideoPath;
    private String status;
    private String encodingDetails;
    private String errorDetails;
}
