package com.contentnexus.processing.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {
    @Bean
    public Path tempDirectory() {
        // Define the path where temporary files will be stored
        return Paths.get("/tmp"); // Adjust this path as needed
    }
}
