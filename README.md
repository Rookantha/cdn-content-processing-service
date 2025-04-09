# Content Nexus Processing Service

This repository contains the source code for the Content Nexus Processing Service, a Spring Boot application designed to process video content ingested into the system.

## Overview

The Processing Service listens for events on a Kafka topic (`video_ingested`), downloads raw video files from Google Cloud Storage (GCS), processes them using the Google Cloud Video Transcoder API, and manages the processed video files and metadata.

**Key Features:**

* **Kafka Integration:** Consumes `ProcessedContent` messages from the `video_ingested` Kafka topic.
* **GCS Interaction:**
    * Downloads raw video files from a configured input GCS bucket.
    * Uploads archived raw and processed video files to a designated archive GCS bucket.
    * Downloads processed video files from the Transcoder output location.
* **Google Cloud Video Transcoder API:** Utilizes the Transcoder API to perform video encoding and format conversion.
* **Metadata Management:** Updates the status and processed video path of `ProcessedContent` entities in a MongoDB database.
* **Error Handling:** Implements robust error handling to manage potential issues during video processing and updates the content status accordingly.
* **Configuration:** Leverages Spring Boot's `@Value` annotation for externalized configuration of GCS buckets, project ID, Transcoder location, and credentials.

## Architecture

The service consists of the following main components:

* **`VideoIngestedListener`:** A Spring `@Service` that acts as a Kafka consumer. It listens to the `video_ingested` topic and delegates the processing of `ProcessedContent` messages to the `VideoProcessingService`.
* **`VideoProcessingService`:** A core Spring `@Service` responsible for the end-to-end video processing workflow. This includes downloading, archiving, transcoding, and updating metadata.
* **`GcsService`:** A utility Spring `@Service` that encapsulates interactions with Google Cloud Storage, including downloading and uploading files and generating GCS URIs.
* **`ProcessedContentRepository`:** A Spring Data MongoDB repository for interacting with the `ProcessedContent` entity in the MongoDB database.
* **`MetadataUpdateService`:** A Spring `@Service` responsible for updating the metadata of `ProcessedContent` entities in the database.
* **`ProcessedContent`:** A JPA entity (though MongoDB is used, the naming might be a legacy) representing the video content being processed, containing information like video ID, raw video path, processed video path, and status.

## Technologies Used

* **Java:** Programming language
* **Spring Boot:** Framework for building the application
* **Spring Kafka:** Integration with Apache Kafka for message consumption
* **Spring Data MongoDB:** Integration with MongoDB for data persistence
* **Google Cloud Storage Client Library:** For interacting with Google Cloud Storage
* **Google Cloud Video Transcoder API Client Library:** For using the Google Cloud Video Transcoder API
* **Google Auth Library:** For handling Google Cloud authentication
* **SLF4j and Logback:** For logging
* **Maven:** Build automation tool

## Prerequisites

Before running the service, ensure you have the following:

* **Java Development Kit (JDK):** Version 17 or higher is recommended.
* **Maven:** Build tool for managing dependencies and building the project.
* **Google Cloud Project:** A Google Cloud project with the following enabled:
    * Google Cloud Storage API
    * Video Transcoder API
    * Billing enabled for the project.
* **Google Cloud Service Account:** A service account with the necessary permissions:
    * **Storage Object Admin (`roles/storage.objectAdmin`)** on the input, output, and archive GCS buckets.
    * **Cloud Video Transcoder Job Creator (`roles/transcoder.jobCreator`)** on the project.
* **Google Cloud Credentials File:** The JSON key file for the service account. The path to this file needs to be configured in the application properties.
* **MongoDB Instance:** A running MongoDB instance accessible to the application.
* **Apache Kafka Instance:** A running Kafka instance with a topic named `video_ingested`.

## Configuration

The application's behavior is configured through the `application.properties` or `application.yml` file. The following properties need to be set:

```properties
# Spring Application Name
spring.application.name=content-processing-service

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://your-mongodb-host:27017/your-database-name

# Kafka Configuration
spring.kafka.bootstrap-servers=your-kafka-bootstrap-servers:9092
spring.kafka.consumer.group-id=content-processing-group

# Google Cloud Configuration
google.cloud.project-id=your-gcp-project-id
google.cloud.transcoder.location=your-transcoder-location # e.g., us-central1
google.cloud.credentials.file=/path/to/your/service-account-key.json

# Google Cloud Storage Buckets
google.gcs.input-bucket=your-input-bucket-name
google.gcs.output-bucket=your-output-bucket-name
google.gcs.archive-bucket=your-archive-bucket-name
```

Build the Application:

```bash
mvn clean package
```
Run the Application:
```bash

mvn spring-boot:run
```
Alternatively, you can run the packaged JAR file:
```bash 
java -jar target/content-nexus-processing-service-0.0.1-SNAPSHOT.jar
(Adjust the JAR file name based on your build output).
``` 

## Usage

Once the service is running, it will automatically start listening for messages on the `video_ingested` Kafka topic. To trigger video processing:

1. Produce a Kafka message to the `video_ingested` topic.
2. The message payload should be a JSON representation of the `ProcessedContent` entity, containing at least the `rawVideoPath` (a GCS URI to the raw video file) and a unique `videoId`.

The service will then:

* Receive the `ProcessedContent` message.
* Download the raw video from the specified GCS path.
* Archive the raw video to the archive bucket.
* Initiate a video transcoding job using the Google Cloud Transcoder API to generate an HD MP4 version.
* Download the processed video from the Transcoder output location.
* Archive the processed video to the archive bucket.
* Update the `ProcessedContent` record in MongoDB with the processed video path and status.

## Error Handling

The service includes error handling mechanisms:

* **Logging:** Uses SLF4j to log important events, warnings, and errors.
* **Transcoder Job Status Monitoring:** Continuously checks the status of the Transcoder job and handles failures or cancellations.
* **Database Updates on Failure:** If an error occurs during processing, the `ProcessedContent` entity's status is updated to "Failed," and error details are saved.

## Future Enhancements

* Implement more sophisticated error recovery and retry mechanisms.
* Add support for different transcoding profiles and output formats.
* Integrate with other content processing services.
* Implement monitoring and alerting for service health and processing status.
* Explore using Cloud Tasks or Workflows for more robust and scalable processing pipelines.

## Contributing

Contributions to this project are welcome. Please follow standard GitHub pull request practices.

## License

MIT License